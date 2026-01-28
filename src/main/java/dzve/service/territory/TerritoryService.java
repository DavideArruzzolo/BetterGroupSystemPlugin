package dzve.service.territory;

import com.hypixel.hytale.builtin.teleport.components.TeleportHistory;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.command.system.arguments.types.Coord;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.model.*;
import dzve.service.NotificationService;
import dzve.service.group.GroupService;
import dzve.utils.ChatFormatter;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.hypixel.hytale.protocol.packets.interface_.NotificationStyle.Danger;
import static com.hypixel.hytale.protocol.packets.interface_.NotificationStyle.Success;

public class TerritoryService {

    private final GroupService groupService;
    private final NotificationService notificationService;
    // Cache for O(1) chunk lookup: key="worldName:x:z", value=groupId
    private final Map<String, UUID> chunkOwnerCache = new ConcurrentHashMap<>();

    public TerritoryService(GroupService groupService) {
        this.groupService = groupService;
        this.notificationService = NotificationService.getInstance();
    }

    private String getChunkKey(String worldName, int x, int z) {
        return worldName + ":" + x + ":" + z;
    }

    public void clearCache() {
        chunkOwnerCache.clear();
    }

    public void cacheGroupClaims(Group group) {
        group.getClaims().forEach(claim -> chunkOwnerCache
                .put(getChunkKey(claim.getWorld(), claim.getChunkX(), claim.getChunkZ()), group.getId()));
    }

    public void uncacheGroupClaims(Group group) {
        group.getClaims().forEach(
                claim -> chunkOwnerCache.remove(getChunkKey(claim.getWorld(), claim.getChunkX(), claim.getChunkZ())));
    }

    public void cacheChunkOwner(String world, int x, int z, UUID groupId) {
        chunkOwnerCache.put(getChunkKey(world, x, z), groupId);
    }

    public void removeChunkOwner(String world, int x, int z) {
        chunkOwnerCache.remove(getChunkKey(world, x, z));
    }

    public Group getGroupByChunk(String worldName, int chunkX, int chunkZ) {
        UUID groupId = chunkOwnerCache.get(getChunkKey(worldName, chunkX, chunkZ));
        return groupId != null ? groupService.getGroup(groupId) : null;
    }

    private ChunkInfo getChunkInfo(PlayerRef player, World world) {
        int blockX = (int) player.getTransform().getPosition().getX();
        int blockZ = (int) player.getTransform().getPosition().getZ();
        int chunkX = blockX >> 5;
        int chunkZ = blockZ >> 5;
        Group group = groupService.getGroupOrNotify(player);
        if (group == null)
            return null;
        if (!group.getLeaderId().equals(player.getUuid())
                && !group.getRole(group.getMember(player.getUuid()).getRoleId())
                .hasPermission(Permission.CAN_MANAGE_CLAIM)) {
            groupService.notify(player, "You don't have permission to do that.");
            return null;
        }
        return new ChunkInfo(group, chunkX, chunkZ, world.getName());
    }

    public void claimChunk(PlayerRef sender, World world) {
        ChunkInfo chunkInfo = getChunkInfo(sender, world);
        if (chunkInfo == null)
            return;

        Group existingOwner = getGroupByChunk(world.getName(), chunkInfo.cx, chunkInfo.cz);
        if (existingOwner != null) {
            if (existingOwner instanceof Faction existingFaction && existingFaction.isRaidable()) {
                convertChunkFromRaidable(existingOwner, chunkInfo.group, chunkInfo, sender);
                return;
            } else {
                groupService.notify(sender, "Chunk already claimed.");
                return;
            }
        }

        if (chunkInfo.group instanceof Faction f) {
            int maxClaims = f.getMaxClaims(GroupService.getConfig().getClaimRatio());
            if (f.getClaims().size() >= maxClaims) {
                groupService.notify(sender, "Not enough power. Claims: " + f.getClaims().size() + "/" + maxClaims);
                return;
            }
        } else if (chunkInfo.group.getClaims().size() >= GroupService.getConfig().getMaxClaimsPerFaction()) {
            groupService.notify(sender, "Claim limit reached.");
            return;
        }

        chunkInfo.group.addClaim(new GroupClaimedChunk(chunkInfo.cx, chunkInfo.cz, chunkInfo.world));
        chunkOwnerCache.put(getChunkKey(chunkInfo.world, chunkInfo.cx, chunkInfo.cz), chunkInfo.group.getId());
        groupService.saveGroups();
        groupService.notify(sender, "Land claimed!", false);
    }

    private void convertChunkFromRaidable(Group raidableFaction, Group newOwner, ChunkInfo chunkInfo,
                                          PlayerRef sender) {
        raidableFaction.removeClaim(chunkInfo.cx, chunkInfo.cz, chunkInfo.world);
        chunkOwnerCache.remove(getChunkKey(chunkInfo.world, chunkInfo.cx, chunkInfo.cz));

        newOwner.addClaim(new GroupClaimedChunk(chunkInfo.cx, chunkInfo.cz, chunkInfo.world));
        chunkOwnerCache.put(getChunkKey(chunkInfo.world, chunkInfo.cx, chunkInfo.cz), newOwner.getId());

        groupService.saveGroups();

        if (raidableFaction instanceof Faction faction) {
            faction.updateRaidableStatus();
        }

        String conversionMessage = String.format(
                "§c[RAID] §f%s §chas conquered chunk (%d, %d) from raidable faction §f%s§c!",
                newOwner.getName(),
                chunkInfo.cx,
                chunkInfo.cz,
                raidableFaction.getName());

        notificationService.broadcastGroup(
                Universe.get().getPlayers().stream()
                        .map(PlayerRef::getUuid)
                        .toList(),
                conversionMessage,
                Danger);

        groupService.notify(sender, "Successfully conquered chunk from raidable faction!", false);

        notificationService.broadcastGroup(
                raidableFaction.getMembers().stream()
                        .map(GroupMember::getPlayerId)
                        .toList(),
                "Your faction has lost chunk (" + chunkInfo.cx + ", " + chunkInfo.cz + ") to " + newOwner.getName()
                        + "!",
                Danger);

        notificationService.broadcastGroup(
                newOwner.getMembers().stream()
                        .map(GroupMember::getPlayerId)
                        .toList(),
                "Your faction has conquered chunk (" + chunkInfo.cx + ", " + chunkInfo.cz + ") from "
                        + raidableFaction.getName() + "!",
                Success);

        notifyAlliesAboutRaid(raidableFaction, newOwner, chunkInfo, false);
        notifyAlliesAboutRaid(newOwner, raidableFaction, chunkInfo, true);
    }

    private void notifyAlliesAboutRaid(Group faction, Group otherFaction, ChunkInfo chunkInfo, boolean isAttacker) {
        if (!(faction instanceof Faction))
            return;

        List<UUID> alliedMembers = new ArrayList<>();
        faction.getDiplomaticRelations().forEach((groupId, status) -> {
            if (status == DiplomacyStatus.ALLY) {
                Group ally = groupService.getGroup(groupId);
                if (ally != null) {
                    alliedMembers.addAll(ally.getMembers().stream()
                            .map(GroupMember::getPlayerId)
                            .toList());
                }
            }
        });

        if (!alliedMembers.isEmpty()) {
            String allyMessage;
            if (isAttacker) {
                allyMessage = String.format(
                        "§a[ALLY RAID] §fYour ally §a%s §fhas successfully raided §c%s §fat chunk (%d, %d)!",
                        faction.getName(),
                        otherFaction.getName(),
                        chunkInfo.cx,
                        chunkInfo.cz);
            } else {
                allyMessage = String.format(
                        "§c[ALLY RAID] §fYour ally §c%s §fhas been raided by §a%s §fat chunk (%d, %d)!",
                        faction.getName(),
                        otherFaction.getName(),
                        chunkInfo.cx,
                        chunkInfo.cz);
            }

            notificationService.broadcastGroup(
                    alliedMembers,
                    allyMessage,
                    isAttacker ? Success : Danger);
        }
    }

    public void unclaimChunk(PlayerRef sender, World world) {
        ChunkInfo chunkInfo = getChunkInfo(sender, world);
        if (chunkInfo == null)
            return;

        if (!chunkInfo.group.isChunkClaimed(chunkInfo.cx, chunkInfo.cz, world.getName())) {
            groupService.notify(sender, "This land is not claimed by your group.");
            return;
        }

        Set<GroupHome> homesToRemove = new HashSet<>();
        for (GroupHome home : chunkInfo.group.getHomes()) {
            int homeChunkX = (int) home.getX() >> 5;
            int homeChunkZ = (int) home.getZ() >> 5;
            if (homeChunkX == chunkInfo.cx && homeChunkZ == chunkInfo.cz &&
                    home.getName().equals(world.getName())) {
                homesToRemove.add(home);
            }
        }

        if (!homesToRemove.isEmpty()) {
            chunkInfo.group.getHomes().removeAll(homesToRemove);
            groupService.notify(sender, "Removed " + homesToRemove.size() + " home(s) from this chunk.", false);
        }

        chunkInfo.group.removeClaim(chunkInfo.cx, chunkInfo.cz, world.getName());
        chunkOwnerCache.remove(getChunkKey(world.getName(), chunkInfo.cx, chunkInfo.cz));
        groupService.saveGroups();
        groupService.notify(sender, "Land unclaimed.", false);
    }

    public void setHome(PlayerRef sender, String name, World world) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null || !groupService.hasPerm(group, sender, Permission.CAN_MANAGE_HOME))
            return;

        if (group.getHome(name) != null) {
            if (!groupService.hasPerm(group, sender, Permission.CAN_MANAGE_HOME)) {
                groupService.notify(sender, "You don't have permission to overwrite an existing home.");
                return;
            }
            group.removeHome(name);
        } else if (group.getHomeCount() >= GroupService.getConfig().getMaxHome()) {
            groupService.notify(sender, "Max homes reached.");
            return;
        }

        int cx = (int) sender.getTransform().getPosition().getX() >> 5;
        int cz = (int) sender.getTransform().getPosition().getZ() >> 5;
        if (!group.isChunkClaimed(cx, cz, world.getName())) {
            groupService.notify(sender, "Must be in claimed land.");
            return;
        }

        group.addHome(new GroupHome(name, sender.getWorldUuid(), sender.getTransform().getPosition().getX(),
                sender.getTransform().getPosition().getY(), sender.getTransform().getPosition().getZ(),
                sender.getTransform().getRotation().getYaw(), sender.getTransform().getRotation().getPitch()));
        groupService.saveGroups();
        groupService.notify(sender, "Home set successfully.", false);
    }

    public void teleportHome(PlayerRef sender, String name, Store<EntityStore> store, Ref<EntityStore> ref,
                             World world) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null || !groupService.hasPerm(group, sender, Permission.CAN_TELEPORT_HOME))
            return;

        GroupMember member = group.getMember(sender.getUuid());
        GroupHome home;

        if (name != null) {
            home = group.getHome(name);
        } else if (member.getDefaultHome() != null) {
            home = group.getHomeById(member.getDefaultHome());
        } else if (group.getHomeCount() >= 1) {
            home = group.getHomes().stream().findFirst().orElse(null);
        } else {
            home = null;
        }

        if (home != null) {
            TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
            HeadRotation headRotationComponent = store.getComponent(ref, HeadRotation.getComponentType());

            if (transformComponent == null || headRotationComponent == null) {
                groupService.notify(sender, "Teleport failed.");
                return;
            }

            Vector3d previousPos = transformComponent.getPosition().clone();
            Vector3f previousHeadRotation = headRotationComponent.getRotation().clone();
            Vector3f previousBodyRotation = transformComponent.getRotation().clone();

            Coord relX = Coord.parse(String.valueOf(home.getX()));
            Coord relY = Coord.parse(String.valueOf(home.getY()));
            Coord relZ = Coord.parse(String.valueOf(home.getZ()));

            double x = relX.resolveXZ(previousPos.getX());
            double z = relZ.resolveXZ(previousPos.getZ());
            double y = relY.resolveYAtWorldCoords(previousPos.getY(), world, x, z);

            groupService.notify(sender, "Teleporting to " + home.getName() + " in 5sec...", false);

            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> world.execute(() -> {
                Teleport teleport = Teleport.createForPlayer(
                                new Vector3d(x, y, z),
                                new Vector3f(previousBodyRotation.getPitch(), home.getYaw(), previousBodyRotation.getRoll()))
                        .setHeadRotation(new Vector3f(home.getPitch(), home.getYaw(), 0));

                store.addComponent(ref, Teleport.getComponentType(), teleport);
                store.ensureAndGetComponent(ref, TeleportHistory.getComponentType())
                        .append(world, previousPos, previousHeadRotation,
                                String.format("Teleport to (%s, %s, %s)", x, y, z));
            }), 5, TimeUnit.SECONDS);

        } else {
            groupService.notify(sender, "Home not found.");
        }
    }

    public void listHomes(PlayerRef sender) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null)
            return;

        if (!groupService.hasPerm(group, sender, Permission.CAN_TELEPORT_HOME)) {
            return;
        }

        ChatFormatter.StyledText msg = ChatFormatter.of("=== Homes for " + group.getName() + " ===\n\n")
                .withColor(Color.YELLOW).withBold();

        Set<GroupHome> homes = group.getHomes();
        if (homes.isEmpty()) {
            msg.append("Your group has no homes set.").withColor(Color.GRAY);
        } else {
            homes.stream()
                    .sorted(Comparator.comparing(GroupHome::getName))
                    .forEach(home -> {
                        GroupMember member = group.getMember(sender.getUuid());
                        boolean isDefault = member != null && home.getName().equals(member.getDefaultHome());
                        msg.append("● ").withColor(Color.GREEN)
                                .append(home.getName()).withBold()
                                .append(isDefault ? " (default)" : "").withColor(new Color(255, 170, 0))
                                .append("\n")
                                .append("  Location: ").withColor(Color.WHITE)
                                .append("x=" + (int) home.getX() + ", y=" + (int) home.getY() + ", z="
                                        + (int) home.getZ())
                                .withColor(Color.CYAN)
                                .append("\n")
                                .append("  World: ").withColor(Color.WHITE)
                                .append(home.getWorld().toString()).withColor(Color.GREEN)
                                .append("\n\n");
                    });
        }
        sender.sendMessage(msg.toMessage());
    }

    public void deleteHome(PlayerRef sender, String homeName) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null || !groupService.hasPerm(group, sender, Permission.CAN_MANAGE_HOME))
            return;

        if (!group.removeHome(homeName)) {
            groupService.notify(sender, "Home not found.");
            return;
        }
        groupService.saveGroups();
        groupService.notify(sender, "Home " + homeName + " deleted.", false);
    }

    public void setDefaultHome(PlayerRef sender, String homeName) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null)
            return;

        GroupMember member = group.getMember(sender.getUuid());
        if (homeName == null) {
            member.setDefaultHome(null);
            groupService.notify(sender, "Default home removed.", false);
        } else {
            GroupHome home = group.getHome(homeName);
            if (home == null) {
                groupService.notify(sender, "Home not found.");
                return;
            }
            member.setDefaultHome(home.getId());
            groupService.notify(sender, "Home " + homeName + " is now your default.", false);
        }
        groupService.saveGroups();
    }

    public void showClaimMap(PlayerRef player, World world) {
        Group playerGroup = groupService.getGroupOrNotify(player);
        if (playerGroup == null)
            return;

        int playerChunkX = (int) player.getTransform().getPosition().getX() >> 5;
        int playerChunkZ = (int) player.getTransform().getPosition().getZ() >> 5;
        String worldName = world.getName();

        // Map dimensions
        int horizontalRadius = 21; // 4 chunks horizontally = 9x5 grid
        int verticalRadius = 6; // 2 chunks vertically
        int mapWidth = horizontalRadius * 2 + 1;
        int mapHeight = verticalRadius * 2 + 1;

        // Build the map
        StringBuilder[] mapLines = new StringBuilder[mapHeight];
        for (int i = 0; i < mapHeight; i++) {
            mapLines[i] = new StringBuilder();
        }

        // Generate map content
        for (int z = -verticalRadius; z <= verticalRadius; z++) {
            for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
                int chunkX = playerChunkX + x;
                int chunkZ = playerChunkZ + z;

                String symbol;
                if (x == 0 && z == 0) {
                    symbol = "@"; // Player position
                } else {
                    Group chunkOwner = getGroupByChunk(worldName, chunkX, chunkZ);
                    symbol = getChunkSymbol(chunkOwner, playerGroup);
                }

                mapLines[z + verticalRadius].append(symbol).append(" ");
            }
        }

        // Display the map
        sendMapMessage(player, "==================== CLAIM MAP ====================");

        // Map rows with simpler borders
        for (StringBuilder line : mapLines) {
            sendMapMessage(player, line.toString());
        }

        sendMapMessage(player, "=================================================");
        sendMapMessage(player, "KEY: @ -> You, O -> Own, A -> Ally, E -> Enemy, - -> Wild");
    }

    private void sendMapMessage(PlayerRef player, String message) {
        player.sendMessage(com.hypixel.hytale.server.core.Message.raw(message));
    }

    private String getChunkSymbol(Group chunkOwner, Group playerGroup) {
        if (chunkOwner == null) {
            return "-"; // Wilderness
        }

        if (chunkOwner.equals(playerGroup)) {
            return "O"; // Own
        }

        // Check if ally or enemy
        // Assuming GroupService or Diplomacy handles status logic directly
        dzve.model.DiplomacyStatus status = playerGroup.getDiplomacyStatus(chunkOwner.getId());
        return switch (status) {
            case ALLY -> "A"; // Ally
            case ENEMY -> "E"; // Enemy
            default -> "E"; // Default to enemy for non-allied groups
        };
    }

    private record ChunkInfo(Group group, int cx, int cz, String world) {
    }
}
