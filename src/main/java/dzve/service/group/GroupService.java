package dzve.service.group;

import com.hypixel.hytale.builtin.teleport.components.TeleportHistory;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.command.system.arguments.types.Coord;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.config.BetterGroupSystemPluginConfig;
import dzve.model.*;
import dzve.service.JsonStorage;
import dzve.service.NotificationService;
import dzve.utils.ChatFormatter;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.File;
import java.text.Normalizer;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.hypixel.hytale.protocol.packets.interface_.NotificationStyle.Danger;
import static com.hypixel.hytale.protocol.packets.interface_.NotificationStyle.Success;
import static dzve.config.BetterGroupSystemPluginConfig.DATA_FOLDER;
import static dzve.config.BetterGroupSystemPluginConfig.FILE_NAME;
import static dzve.model.GroupType.FACTION;

public class GroupService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final GroupService instance = new GroupService();
    private static final NotificationService notificationService = NotificationService.getInstance();
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}_]+$");
    private static final Pattern HEX_PATTERN = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static BetterGroupSystemPluginConfig config = null;
    private final JsonStorage<GroupData> storage;
    private final Map<UUID, Group> groups = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerGroupMap = new ConcurrentHashMap<>(); // PlayerUUID -> GroupUUID
    private final Map<UUID, Set<UUID>> invitations = new ConcurrentHashMap<>(); // PlayerUUID -> Set<GroupUUID>
    // Cache unicità
    private final Set<String> namesGroups = ConcurrentHashMap.newKeySet();
    private final Set<String> tagsGroups = ConcurrentHashMap.newKeySet();

    private GroupService() {
        this.storage = new JsonStorage<>(new File(DATA_FOLDER, FILE_NAME), GroupData.class);
        loadGroups();
    }

    public static synchronized GroupService getInstance(BetterGroupSystemPluginConfig betterGroupSystemPluginConfig) {
        config = betterGroupSystemPluginConfig;
        return instance;
    }

    private void loadGroups() {
        groups.clear();
        playerGroupMap.clear();
        namesGroups.clear();
        tagsGroups.clear();
        GroupData data = storage.load();
        if (data != null && data.groups() != null) {
            this.groups.putAll(data.groups());
            groups.values().forEach(this::cacheGroupData);
            LOGGER.atInfo().log("Loaded " + groups.size() + " groups.");
        }
    }

    public void reload(PlayerRef player) {
        loadGroups();
        notify(player, "Data is reloaded.", false);
    }

    // --- Core Logic ---

    public void shutdown() {
        storage.shutdown();
    }

    private void cacheGroupData(Group group) {
        namesGroups.add(group.getName().toLowerCase());
        tagsGroups.add(group.getTag().toLowerCase());
        group.getMembers().forEach(m -> playerGroupMap.put(m.getPlayerId(), group.getId()));
    }

    public void saveGroups() {
        storage.saveAsync(new GroupData(new HashMap<>(groups)));
    }

    public void invitePlayer(PlayerRef sender, PlayerRef target) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !hasPerm(group, sender, Permission.CAN_INVITE)) return;

        if (playerGroupMap.containsKey(target.getUuid())) {
            notify(sender, "Player already in a group.");
            return;
        }
        if (group.getMembers().size() >= (group.getType().equals(FACTION) ? config.getMaxSize() : config.getMaxSize() + config.getSlotQuantityGainForLevel() * ((Guild) group).getLevel())) {
            notify(sender, "Group full.");
            return;
        }

        invitations.computeIfAbsent(target.getUuid(), k -> ConcurrentHashMap.newKeySet()).add(group.getId());
        notify(sender, "Invited " + target.getUsername(), false);
    }

    /* --- I. Management Commands --- */

    public void createGroup(PlayerRef player, String name, String tag, @Nullable String color, @Nullable String desc) {
        if (playerGroupMap.containsKey(player.getUuid())) {
            notify(player, "You are already in a group.");
            return;
        }

        String safeName = normalize(name);
        String safeTag = normalize(tag);

        if (!validateIdentifier(player, safeName, config.getMinNameLength(), config.getMaxNameLength(), namesGroups, "Name"))
            return;
        if (!validateIdentifier(player, safeTag, config.getMinTagLength(), config.getMaxTagLength(), tagsGroups, "Tag"))
            return;
        if (color != null && !HEX_PATTERN.matcher(color).matches()) {
            notify(player, "Invalid color code.");
            return;
        }
        if (desc != null && desc.length() > config.getMaxDescriptionLength()) {
            notify(player, "Description too long.");
            return;
        }

        Group group;
        desc = desc != null ? desc.substring(0, desc.length() - 1) : null;
        if ("GUILD".equalsIgnoreCase(config.getPluginMode())) {
            group = new Guild(safeName, safeTag, desc, color, player);
        } else {
            group = new Faction(safeName, safeTag, desc, color, player);
        }

        groups.put(group.getId(), group);
        cacheGroupData(group);
        saveGroups();
        notify(player, "Group created successfully!", false);
    }

    public void disband(PlayerRef player) {
        Group group = getGroupOrNotify(player);
        if (group == null || !isLeader(group, player)) return;

        namesGroups.remove(group.getName().toLowerCase());
        tagsGroups.remove(group.getTag().toLowerCase());
        group.getMembers().forEach(m -> playerGroupMap.remove(m.getPlayerId()));

        groups.remove(group.getId());
        saveGroups();
        notify(player, "Group deleted.", false);
    }

    public void leaveGroup(PlayerRef player) {
        Group group = getGroupOrNotify(player);
        if (group == null) return;
        if (group.isLeader(player.getUuid()) && group.getMembers().size() > 1) {
            notify(player, "Leader cannot leave. Transfer ownership first.");
            return;
        }

        if (group.getMembers().size() <= 1) {
            disband(player);
        } else {
            group.removeMember(player.getUuid());
            playerGroupMap.remove(player.getUuid());
            saveGroups();
            notify(player, "You left the group.", false);
        }
    }

    public void updateGroup(PlayerRef player, String type, String value) {
        Group group = getGroupOrNotify(player);
        if (group == null || !hasPerm(group, player, Permission.CAN_UPDATE_GROUP)) return;

        switch (type.toLowerCase()) {
            case "name" -> {
                String sName = normalize(value);
                if (sName.equalsIgnoreCase(group.getName())) {
                    notify(player, "Group name is already set to this value.");
                    return;
                }
                if (validateIdentifier(player, sName, config.getMinNameLength(), config.getMaxNameLength(), namesGroups, "Name")) {
                    namesGroups.remove(group.getName().toLowerCase());
                    group.setName(sName);
                    namesGroups.add(sName.toLowerCase());
                    notify(player, "The name of the group updated successfully", false);
                } else {
                    return;
                }
            }
            case "tag" -> {
                String sTag = normalize(value);
                if (sTag.equalsIgnoreCase(group.getTag())) {
                    notify(player, "Group tag is already set to this value.");
                    return;
                }
                if (validateIdentifier(player, sTag, config.getMinTagLength(), config.getMaxTagLength(), tagsGroups, "Tag")) {
                    tagsGroups.remove(group.getTag().toLowerCase());
                    group.setTag(sTag);
                    tagsGroups.add(sTag.toLowerCase());
                    notify(player, "The tag of the group updated successfully", false);
                } else {
                    return;
                }
            }
            case "color" -> {
                if (group.getColor() != null && group.getColor().equalsIgnoreCase(value)) {
                    notify(player, "Group color is already set to this value.");
                    return;
                }
                if (HEX_PATTERN.matcher(value).matches()) {
                    group.setColor(value);
                    notify(player, "The color of the group updated successfully", false);
                } else {
                    notify(player, "Invalid hex color.");
                    return;
                }
            }
            case "desc" -> {
                if (group.getDescription() != null && group.getDescription().equals(value)) {
                    notify(player, "Group description is already set to this value.");
                    return;
                }
                if (value.length() <= config.getMaxDescriptionLength()) {
                    group.setDescription(value);
                    notify(player, "The description of the group updated successfully", false);
                } else {
                    notify(player, "Desc too long.");
                    return;
                }
            }
            default -> {
                notify(player, "Invalid property to update.");
                return;
            }
        }
        saveGroups();
    }

    /* --- II. Member Commands --- */

    public void transferLeadership(PlayerRef sender, UUID targetId) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !isLeader(group, sender)) return;
        if (!group.isMember(targetId)) {
            notify(sender, "Target not in group.");
            return;
        }

        GroupRole leaderRole = getRoleByPriority(group, Integer.MAX_VALUE);
        GroupRole memberRole = getRoleByPriority(group, 50);

        group.setLeaderId(targetId);
        group.changeMemberRole(targetId, leaderRole.getId());
        group.changeMemberRole(sender.getUuid(), memberRole.getId());
        saveGroups();
        notify(sender, "Leadership transferred.", false);
    }

    public void acceptInvitation(PlayerRef player, String groupName) {
        if (playerGroupMap.containsKey(player.getUuid())) {
            notify(player, "Already in a group.");
            return;
        }

        Group group = groups.values().stream().filter(g -> g.getName().equalsIgnoreCase(groupName)).findFirst().orElse(null);
        if (group == null) {
            notify(player, "Group not found.");
            return;
        }

        Set<UUID> invites = invitations.get(player.getUuid());
        if (invites == null || !invites.contains(group.getId())) {
            notify(player, "No invitation from this group.");
            return;
        }
        if (group.getMembers().size() >= config.getMaxSize()) {
            notify(player, "Group is full.");
            return;
        }

        GroupRole defaultRole = group.getRoles().stream().filter(GroupRole::isDefault).findFirst().orElseThrow();
        group.addMember(player, defaultRole.getId());
        playerGroupMap.put(player.getUuid(), group.getId());
        invites.remove(group.getId());
        saveGroups();
        notify(player, "Joined " + group.getName(), false);
    }

    public void kickMember(PlayerRef sender, UUID targetId) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !hasPerm(group, sender, Permission.CAN_KICK)) return;
        if (!group.isMember(targetId)) {
            notify(sender, "Target not in group.");
            return;
        }
        if (sender.getUuid().equals(targetId)) {
            notify(sender, "Cannot kick self.");
            return;
        }

        if (!canModify(group, sender.getUuid(), targetId)) {
            notify(sender, "Target rank too high.");
            return;
        }

        group.removeMember(targetId);
        playerGroupMap.remove(targetId);
        saveGroups();
        notify(sender, "Member kicked.", false);
    }

    public void setHome(PlayerRef sender, String name) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !hasPerm(group, sender, Permission.CAN_MANAGE_HOME)) return;

        if (group.getHome(name) != null) {
            if (!hasPerm(group, sender, Permission.CAN_MANAGE_HOME)) {
                notify(sender, "You don't have permission to overwrite an existing home.");
                return;
            }
            group.removeHome(name);
        } else if (group.getHomeCount() >= config.getMaxHome()) {
            notify(sender, "Max homes reached.");
            return;
        }

        int cx = (int) sender.getTransform().getPosition().getX() >> 5;
        int cz = (int) sender.getTransform().getPosition().getZ() >> 5;
        if (!group.isChunkClaimed(cx, cz, sender.getWorldUuid())) {
            notify(sender, "Must be in claimed land.");
            return;
        }

        group.addHome(new GroupHome(name, sender.getWorldUuid(), sender.getTransform().getPosition().getX(), sender.getTransform().getPosition().getY(), sender.getTransform().getPosition().getZ(), sender.getTransform().getRotation().getYaw(), sender.getTransform().getRotation().getPitch()));
        saveGroups();
        notify(sender, "Home set successfully.", false);
    }

    /* --- III. Role Commands --- */

    public void createRole(PlayerRef sender, String name, List<String> grants) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !hasPerm(group, sender, Permission.CAN_MANAGE_ROLE)) return;
        if (group.getRoles().size() >= 10) {
            notify(sender, "Max roles reached.");
            return;
        }
        if (getRoleByName(group, name) != null) {
            notify(sender, "Role exists.");
            return;
        }

        Set<Permission> perms = parsePerms(grants);
        if (perms == null) {
            notify(sender, "Invalid permissions.");
            return;
        }

        modifyRoles(group, roles -> roles.add(new GroupRole(name, name, 10, false, perms)));
        notify(sender, "Role created.", false);
    }

    public void deleteRole(PlayerRef sender, String name) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !hasPerm(group, sender, Permission.CAN_MANAGE_ROLE)) return;

        GroupRole role = getRoleByName(group, name);
        if (role == null || role.isDefault()) {
            notify(sender, "Invalid role.");
            return;
        }
        if (group.getMembers().stream().anyMatch(m -> m.getRoleId().equals(role.getId()))) {
            notify(sender, "Role is in use.");
            return;
        }

        modifyRoles(group, roles -> roles.remove(role));
        notify(sender, "Role deleted.", false);
    }

    public void updateRole(PlayerRef sender, String name, List<String> addGrants, List<String> removeGrants) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !hasPerm(group, sender, Permission.CAN_MANAGE_ROLE)) return;

        GroupRole role = getRoleByName(group, name);
        if (role == null || role.isDefault()) {
            notify(sender, "Cannot edit this role.");
            return;
        }

        Set<Permission> perms = new HashSet<>(role.getPermissions());
        Set<Permission> addPerms = parsePerms(addGrants);
        Set<Permission> removePerms = parsePerms(removeGrants);

        if (addPerms != null) {
            perms.addAll(addPerms);
        }
        if (removePerms != null) {
            perms.removeAll(removePerms);
        }

        role.setPermissions(perms);
        saveGroups();
        notify(sender, "Role permissions updated.", false);
    }

    public void setRole(PlayerRef sender, UUID targetId, String roleName) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !hasPerm(group, sender, Permission.CAN_CHANGE_ROLE)) return;
        if (!group.isMember(targetId)) {
            notify(sender, "Target not in group.");
            return;
        }
        if (!canModify(group, sender.getUuid(), targetId)) {
            notify(sender, "Hierarchy prevents this.");
            return;
        }

        GroupRole role = getRoleByName(group, roleName);
        if (role == null) {
            notify(sender, "Role not found.");
            return;
        }

        GroupRole senderRole = getMemberRole(group, sender.getUuid());
        if (!group.isLeader(sender.getUuid()) && role.getPriority() >= senderRole.getPriority()) {
            notify(sender, "Cannot promote to rank >= yours.");
            return;
        }

        group.changeMemberRole(targetId, role.getId());
        saveGroups();
    }

    /* --- IV. Territory & V. Economy --- */

    public void claimChunk(PlayerRef sender) {
        ChunkInfo chunkInfo = getChunkInfo(sender);
        if (chunkInfo == null) return;

        boolean taken = groups.values().stream().anyMatch(g -> g.isChunkClaimed(chunkInfo.cx, chunkInfo.cz, chunkInfo.world));
        if (taken) {
            notify(sender, "Chunk already claimed.");
            return;
        }

        if (chunkInfo.group instanceof Faction f) {
            int maxClaims = f.getMaxClaims(config.getClaimRatio());
            if (f.getClaims().size() >= maxClaims) {
                notify(sender, "Not enough power. Claims: " + f.getClaims().size() + "/" + maxClaims);
                return;
            }
        } else if (chunkInfo.group.getClaims().size() >= config.getMaxClaimsPerFaction()) {
            notify(sender, "Claim limit reached.");
            return;
        }

        chunkInfo.group.addClaim(new GroupClaimedChunk(chunkInfo.cx, chunkInfo.cz, chunkInfo.world));
        saveGroups();
        notify(sender, "Land claimed!", false);
    }

    public void teleportHome(PlayerRef sender, String name, Store<EntityStore> store, Ref<EntityStore> ref, World world) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !hasPerm(group, sender, Permission.CAN_TELEPORT_HOME)) return;

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
                notify(sender, "Teleport failed.");
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

            notify(sender, "Teleporting to " + home.getName() + " in 5sec...", false);

            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> world.execute(() -> {
                Teleport teleport = Teleport.createForPlayer(
                        new Vector3d(x, y, z),
                        new Vector3f(previousBodyRotation.getPitch(), home.getYaw(), previousBodyRotation.getRoll())
                ).setHeadRotation(new Vector3f(home.getPitch(), home.getYaw(), 0));

                store.addComponent(ref, Teleport.getComponentType(), teleport);
                store.ensureAndGetComponent(ref, TeleportHistory.getComponentType())
                        .append(world, previousPos, previousHeadRotation,
                                String.format("Teleport to (%s, %s, %s)", x, y, z));
            }), 5, TimeUnit.SECONDS);

        } else {
            notify(sender, "Home not found.");
        }
    }

    public void unclaimChunk(PlayerRef sender) {
        ChunkInfo chunkInfo = getChunkInfo(sender);
        if (chunkInfo == null) return;

        if (!chunkInfo.group.isChunkClaimed(chunkInfo.cx, chunkInfo.cz, chunkInfo.world)) {
            notify(sender, "This land is not claimed by your group.");
            return;
        }

        chunkInfo.group.removeClaim(chunkInfo.cx, chunkInfo.cz, chunkInfo.world);
        saveGroups();
        notify(sender, "Land unclaimed.", false);
    }

    public void withdraw(PlayerRef sender, double amount) {
        Group group = getGroupOrNotify(sender);
        if (group == null) return;

        if (amount <= 0 || !group.withdraw(amount, sender.getUuid())) {
            notify(sender, "Invalid amount or insufficient funds.");
            return;
        }

        // EconomyService.add(sender, amount);
        saveGroups();
        notify(sender, "Withdrawn " + amount, false);
    }

    public void withdrawFromGroup(PlayerRef sender, double amount) {
        Group group = getGroupOrNotify(sender);
        if (group == null) return;

        if (amount <= 0 || !group.withdrawFromGroup(amount, sender.getUuid())) {
            notify(sender, "Invalid amount, insufficient funds, or no permission.");
            return;
        }

        // EconomyService.add(sender, amount);
        saveGroups();
        notify(sender, "Withdrawn " + amount + " from group bank", false);
    }

    public void upgradeGuild(PlayerRef sender) {
        Group group = getGroupOrNotify(sender);
        if (!(group instanceof Guild guild)) {
            notify(sender, "Not a guild.");
            return;
        }
        if (!hasPerm(group, sender, Permission.CAN_UPGRADE_GUILD)) return;

        if (!guild.canUpgrade()) {
            notify(sender, "Cannot upgrade (Max level or Insufficient funds).");
            return;
        }

        if (guild.withdrawFromGroup(guild.calculateCostToNextLevel(), sender.getUuid())) {
            guild.setLevel(guild.getLevel() + 1);
            saveGroups();
            notify(sender, "Guild Level Up: " + guild.getLevel(), false);
        } else {
            notify(sender, "Insufficient funds in group bank to upgrade.");
        }
    }

    // --- Info & Lists ---

    public void getGroupInfo(PlayerRef sender, @Nullable String targetGroupName) {

        Group group;
        if (targetGroupName == null) {
            group = getGroupOrNotify(sender);
        } else {
            group = groups.values().stream()
                    .filter(g -> g.getName().equalsIgnoreCase(targetGroupName))
                    .findFirst()
                    .orElse(null);

            if (group == null) {
                notify(sender, "Group not found.");
                return;
            }
        }

        if (group == null) return;

        String leaderName = Optional.ofNullable(group.getMember(group.getLeaderId()))
                .map(GroupMember::getPlayerName)
                .orElse("N/A");

        Color groupColor = group.getColor() != null ? Color.decode(group.getColor()) : Color.white;

        ChatFormatter.StyledText msg =
                ChatFormatter.of("#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#\n")
                        .append("Name: ").append(group.getName() + "\n").withBold()
                        .append("Tag: ").append(group.getTag() + "\n").withBold()
                        .append("Color: ").append(group.getColor() != null ? group.getColor() : "No color set" + "\n").withBold().withColor(groupColor)
                        .append("Description: ").append(group.getDescription() != null ? group.getDescription() : "No description set" + "\n").withBold()
                        .append("\n")
                        .append("Leader: ").append(leaderName + "\n").withBold()
                        .append("Members: ").append(group.getMembers().size() + " / " + (group.getType().equals(FACTION) ? config.getMaxSize() : config.getMaxSize() + config.getSlotQuantityGainForLevel() * ((Guild) group).getLevel()) + "\n").withBold()
                        .append("Claims: ").append(group.getMembers().stream().anyMatch(a -> a.getPlayerId().equals(sender.getUuid())) ? group.getClaims().size() + " / " + config.getMaxClaimsPerFaction() + "\n" : "Not allowed to see this info\n").withBold()
                        .append("Homes: ").append(group.getMembers().stream().anyMatch(a -> a.getPlayerId().equals(sender.getUuid())) ? group.getHomeCount() + "\n" : "Not allowed to see this info\n").withBold()
                        .append("Bank: ").append(group.getMembers().stream().anyMatch(a -> a.getPlayerId().equals(sender.getUuid())) ? String.format("%.2f", group.getBankBalance()) + "\n" : "Not allowed to see this info\n").withBold()
                        .append("Created: ").append(group.getCreatedAt().toLocalDate().toString() + "\n").withBold()
                        .append("\n");
        if (group instanceof Faction faction) {
            msg = msg
                    .append("Total Power: ").append(String.format("%.2f", faction.getTotalPower()) + "\n").withBold()
                    .append("Kills: ").append(faction.getKills() + "\n").withBold()
                    .append("Deaths: ").append(faction.getDeaths() + "\n").withBold()
                    .append("K/D Ratio: ").append(String.format("%.2f", faction.getKillDeathRatio()) + "\n").withBold()
                    .append("Can be raidable: ");

            if (group.getMembers().stream().anyMatch(a -> a.getPlayerId().equals(sender.getUuid()))) {
                msg = msg.append(faction.isRaidable() ? "YES\n" : "NO\n").withBold().withColor(faction.isRaidable() ? Color.RED : Color.GREEN);
            } else {
                msg = msg.append("Not allowed to see this info\n").withBold();
            }
        }
        if (group instanceof Guild guild) {
            msg = msg.append("Level: ").append(String.valueOf(guild.getLevel()))
                    .append("MoneyToNextLevel: ").append(group.getMembers().stream().anyMatch(a -> a.getPlayerId().equals(sender.getUuid())) ? guild.getMoneyToNextLevel() + "\n" : "Not allowed to see this info\n").withBold();
        }
        msg = msg.append("#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#");

        sender.sendMessage(msg.toMessage());
    }


    public void listInvitations(PlayerRef sender) {
        Set<UUID> invites = invitations.get(sender.getUuid());
        if (invites == null || invites.isEmpty()) {
            notify(sender, "No pending invitations.", false);
            return;
        }

        ChatFormatter.StyledText msg = ChatFormatter.of("You have invitations from the following groups:\n");

        invites.stream()
                .map(groups::get)
                .filter(Objects::nonNull)
                .forEach(group -> {
                    Color groupColor = group.getColor() != null ? Color.decode(group.getColor()) : Color.white;
                    String leaderName = Optional.ofNullable(group.getMember(group.getLeaderId()))
                            .map(GroupMember::getPlayerName)
                            .orElse("N/A");

                    msg.append("-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-\n")
                            .append("Name: ").append(group.getName() + "\n").withBold()
                            .append("Tag: ").append(group.getTag() + "\n").withBold()
                            .append("Color: ").append(group.getColor() != null ? group.getColor() : "No color set" + "\n").withBold().withColor(groupColor)
                            .append("Description: ").append(group.getDescription() != null ? group.getDescription() : "No description set" + "\n").withBold()
                            .append("\n")
                            .append("Leader: ").append(leaderName + "\n").withBold()
                            .append("Members: ").append(group.getMembers().size() + " / " + (group.getType().equals(FACTION) ? config.getMaxSize() : config.getMaxSize() + config.getSlotQuantityGainForLevel() * ((Guild) group).getLevel()) + "\n").withBold();

                    if (group instanceof Faction faction) {
                        msg.append("Total Power: ").append(String.format("%.2f", faction.getTotalPower()) + "\n").withBold();
                    }
                    if (group instanceof Guild guild) {
                        msg.append("Level: ").append(guild.getLevel() + "\n").withBold();
                    }
                    msg.append("-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-\n");
                });

        sender.sendMessage(msg.toMessage());
    }

    public void listRoles(PlayerRef sender) {
        Group group = getGroupOrNotify(sender);
        if (group == null) return;

        ChatFormatter.StyledText msg = ChatFormatter.of("Roles for " + group.getName() + ":\n");

        group.getRoles().stream()
                .sorted(Comparator.comparingInt(GroupRole::getPriority).reversed())
                .forEach(role -> {
                    String perms = role.getPermissions().stream()
                            .map(Permission::name)
                            .collect(Collectors.joining(", "));

                    msg.append("-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-\n")
                            .append("Role: ").append(role.getName() + "\n").withBold()
                            .append("Priority: ").append(role.getPriority() + "\n")
                            .append("Permissions: ").append(perms.isEmpty() ? "None" : perms + "\n");
                });

        msg.append("-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-");
        sender.sendMessage(msg.toMessage());
    }

    public void listHomes(PlayerRef sender) {
        Group group = getGroupOrNotify(sender);
        if (group == null) return;

        if (!hasPerm(group, sender, Permission.CAN_TELEPORT_HOME)) {
            return;
        }

        Set<GroupHome> homes = group.getHomes();
        if (homes.isEmpty()) {
            notify(sender, "Your group has no homes set.", false);
            return;
        }

        ChatFormatter.StyledText msg = ChatFormatter.of("Homes for " + group.getName() + ":\n").withBold();
        homes.stream()
                .sorted(Comparator.comparing(GroupHome::getName))
                .forEach(home -> {
                    msg.append("- " + home.getName() + ": ")
                            .append("x=" + (int) home.getX() + ", y=" + (int) home.getY() + ", z=" + (int) home.getZ() + "\n");
                });
        sender.sendMessage(msg.toMessage());
    }

    // --- Territory Extension ---

    public void deleteHome(PlayerRef sender, String homeName) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !hasPerm(group, sender, Permission.CAN_MANAGE_HOME)) return;

        if (!group.removeHome(homeName)) {
            notify(sender, "Home not found.");
            return;
        }
        saveGroups();
        notify(sender, "Home " + homeName + " deleted.", false);
    }

    public void setDefaultHome(PlayerRef sender, @Nullable String homeName) {
        Group group = getGroupOrNotify(sender);
        if (group == null) return;

        GroupMember member = group.getMember(sender.getUuid());
        if (homeName == null) {
            member.setDefaultHome(null);
            notify(sender, "Default home removed.", false);
        } else {
            GroupHome home = group.getHome(homeName);
            if (home == null) {
                notify(sender, "Home not found.");
                return;
            }
            member.setDefaultHome(home.getId());
            notify(sender, "Home " + homeName + " is now your default.", false);
        }
        saveGroups();
    }

    public void setDiplomacy(PlayerRef sender, String targetGroupName, DiplomacyStatus status) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !hasPerm(group, sender, Permission.CAN_MANAGE_DIPLOMACY)) return;

        Group target = groups.values().stream()
                .filter(g -> g.getName().equalsIgnoreCase(targetGroupName))
                .findFirst().orElse(null);

        if (target == null) {
            notify(sender, "Target group not found.");
            return;
        }
        if (target.getId().equals(group.getId())) {
            notify(sender, "Cannot change relations with yourself.");
            return;
        }

        if (status == DiplomacyStatus.ALLY) {
            // Qui servirebbe un sistema di "Richiesta Alleanza" simile agli inviti.
            // Per ora lo setto diretto come da tua richiesta semplificata, ma idealmente è bidirezionale.
            notify(sender, "Alliance request sent (Not implemented fully).", false);
        } else {
            // Neutral o Enemy si possono settare unilateralmente
            group.setDiplomacyStatus(target.getId(), status);
            saveGroups();
            notify(sender, "Diplomacy with " + target.getName() + " set to " + status, false);
        }
    }

    // --- Diplomacy ---

    public void listDiplomacy(PlayerRef sender) {
        Group group = getGroupOrNotify(sender);
        if (group == null) return;

        ChatFormatter.StyledText msg = ChatFormatter.of("Diplomatic Relations for " + group.getName() + ":\n");

        if (group.getDiplomaticRelations().isEmpty()) {
            msg.append("No diplomatic relations set.");
        } else {
            group.getDiplomaticRelations().forEach((groupId, status) -> {
                Group relatedGroup = groups.get(groupId);
                if (relatedGroup != null) {
                    Color color = switch (status) {
                        case ALLY -> Color.GREEN;
                        case NEUTRAL -> Color.GRAY;
                        case ENEMY -> Color.RED;
                    };
                    msg.append("- " + relatedGroup.getName() + ": ").append(status.name()).withColor(color).append("\n");
                }
            });
        }
        sender.sendMessage(msg.toMessage());
    }

    public void deposit(PlayerRef sender, double amount) {
        if (amount <= 0) {
            notify(sender, "Amount must be positive.");
            return;
        }

        Group group = getGroupOrNotify(sender);
        if (group == null) return;

        // TODO: Integrare check: if (EconomyService.remove(sender, amount)) ...

        group.deposit(amount, sender.getUuid());
        if (group instanceof Guild guild) {
            guild.getMoneyContributions().merge(sender.getUuid(), amount, Double::sum);
        }

        saveGroups();
        notify(sender, "Deposited " + amount, false);
    }

    public void depositToGroup(PlayerRef sender, double amount) {
        if (amount <= 0) {
            notify(sender, "Amount must be positive.");
            return;
        }

        Group group = getGroupOrNotify(sender);
        if (group == null) return;

        // TODO: Integrare check: if (EconomyService.remove(sender, amount)) ...

        group.depositToGroup(amount);
        if (group instanceof Guild guild) {
            guild.getMoneyContributions().merge(sender.getUuid(), amount, Double::sum);
        }

        saveGroups();
        notify(sender, "Deposited " + amount + " to group bank", false);
    }

    // --- Economy Extension ---

    public void getBalance(PlayerRef sender, @Nullable String type) {
        Group group = getGroupOrNotify(sender);
        if (group == null) return;

        if (type == null || type.equalsIgnoreCase("player")) {
            notify(sender, "Player balance: " + group.getBalance(sender.getUuid()), false);
        } else if (type.equalsIgnoreCase("group")) {
            notify(sender, "Group Bank Balance: " + group.getBankBalance(), false);
        } else {
            notify(sender, "Invalid type. Use 'player' or 'group'.");
        }
    }

    @Nullable
    private ChunkInfo getChunkInfo(PlayerRef sender) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !hasPerm(group, sender, Permission.CAN_MANAGE_CLAIM)) return null;

        int cx = (int) sender.getTransform().getPosition().getX() >> 5;
        int cz = (int) sender.getTransform().getPosition().getZ() >> 5;
        UUID world = sender.getWorldUuid();
        return new ChunkInfo(group, cx, cz, world);
    }

    // --- Helpers (Optimization) ---

    private boolean hasPerm(Group g, PlayerRef p, Permission perm) {
        if (g.isLeader(p.getUuid())) return true;
        GroupRole r = getMemberRole(g, p.getUuid());
        if (r != null && r.hasPermission(perm)) {
            return true;
        }
        notify(p, "No Permission.");
        return false;
    }

    @Nullable
    private Group getGroupOrNotify(PlayerRef p) {
        UUID gid = playerGroupMap.get(p.getUuid());
        if (gid == null) notify(p, "You are not in a group.");
        return gid != null ? groups.get(gid) : null;
    }

    private boolean isLeader(Group g, PlayerRef p) {
        if (g.isLeader(p.getUuid())) {
            return true;
        }
        notify(p, "Leader only.");
        return false;
    }

    private void notify(PlayerRef player, String msg) {
        notify(player, msg, true);
    }

    private void notify(PlayerRef player, String msg, boolean isError) {
        notificationService.sendNotification(player.getUuid(), msg, isError ? Danger : Success); // Simplification
    }

    private boolean validateIdentifier(PlayerRef player, String val, int min, int max, Set<String> cache, String field) {
        if (val.length() < min || val.length() > max) {
            notify(player, field + " length invalid.");
            return false;
        }
        if (!NAME_PATTERN.matcher(val).matches()) {
            notify(player, field + " contains invalid chars.");
            return false;
        }
        if (cache.contains(val.toLowerCase())) {
            notify(player, field + " already taken.");
            return false;
        }
        return true;
    }

    private String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFC).trim().replace(" ", "_");
    }

    private GroupRole getRoleByName(Group g, String n) {
        return g.getRoles().stream().filter(r -> r.getName().equalsIgnoreCase(n)).findFirst().orElse(null);
    }

    private GroupRole getRoleByPriority(Group g, int p) {
        return g.getRoles().stream().filter(r -> r.getPriority() <= p).max(Comparator.comparingInt(GroupRole::getPriority)).orElseThrow();
    }

    private GroupRole getMemberRole(Group g, UUID pid) {
        return g.getRoles().stream().filter(r -> r.getId().equals(g.getMember(pid).getRoleId())).findFirst().orElse(null);
    }

    private boolean canModify(Group g, UUID actor, UUID target) {
        if (g.isLeader(actor)) return true;
        if (g.isLeader(target)) return false;
        return getMemberRole(g, actor).getPriority() > getMemberRole(g, target).getPriority();
    }

    private void modifyRoles(Group g, Consumer<Set<GroupRole>> modifier) {
        Set<GroupRole> mutable = new HashSet<>(g.getRoles());
        modifier.accept(mutable);
        g.setRoles(mutable);
        saveGroups();
    }

    private Set<Permission> parsePerms(List<String> list) {
        if (list == null) return null;
        try {
            return list.stream()
                    .map(s -> s.replace(",", "").trim())
                    .map(s -> Permission.valueOf(s.toUpperCase().replace(".", "_")))
                    .collect(Collectors.toSet());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private record GroupData(Map<UUID, Group> groups) {
    }

    private record ChunkInfo(Group group, int cx, int cz, UUID world) {
    }
}
