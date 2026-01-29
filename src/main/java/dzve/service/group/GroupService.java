package dzve.service.group;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.config.BetterGroupSystemPluginConfig;
import dzve.model.*;
import dzve.service.JsonStorage;
import dzve.service.NotificationService;
import dzve.service.diplomacy.DiplomacyService;
import dzve.service.economy.EconomyService;
import dzve.service.membership.MembershipService;
import dzve.service.territory.TerritoryService;
import dzve.utils.ChatFormatter;
import dzve.utils.MapUtils;
import lombok.Getter;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.File;
import java.text.Normalizer;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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
    private final Map<UUID, UUID> playerGroupMap = new ConcurrentHashMap<>();

    private final Set<String> namesGroups = ConcurrentHashMap.newKeySet();
    private final Set<String> tagsGroups = ConcurrentHashMap.newKeySet();

    @Getter
    private final TerritoryService territoryService;
    @Getter
    private final EconomyService economyService;
    @Getter
    private final DiplomacyService diplomacyService;
    @Getter
    private final MembershipService membershipService;

    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors
            .newSingleThreadExecutor();

    private GroupService() {
        this.storage = new JsonStorage<>(new File(DATA_FOLDER, FILE_NAME), GroupData.class);
        this.territoryService = new TerritoryService(this);
        this.economyService = new EconomyService(this);
        this.diplomacyService = new DiplomacyService(this);
        this.membershipService = new MembershipService(this);
        loadGroups();
    }

    public static BetterGroupSystemPluginConfig getConfig() {
        if (config == null) {
            throw new IllegalStateException(
                    "GroupService config not initialized. Please ensure getInstance(config) is called before using the service.");
        }
        return config;
    }

    public static void initialize(BetterGroupSystemPluginConfig betterGroupSystemPluginConfig) {
        if (betterGroupSystemPluginConfig == null) {
            throw new IllegalArgumentException("Config cannot be null during initialization");
        }
        config = betterGroupSystemPluginConfig;
    }

    public static GroupService getInstance() {
        if (config == null) {
            throw new IllegalStateException("GroupService not initialized! Call initialize() first.");
        }
        return instance;
    }

    @Deprecated
    public static synchronized GroupService getInstance(BetterGroupSystemPluginConfig betterGroupSystemPluginConfig) {
        if (config == null) {
            if (betterGroupSystemPluginConfig != null) {
                initialize(betterGroupSystemPluginConfig);
            } else {
                LOGGER.atWarning().log("GroupService.getInstance(null) called before config initialization!");
            }
        }
        return instance;
    }

    private void loadGroups() {
        groups.clear();
        playerGroupMap.clear();
        namesGroups.clear();
        tagsGroups.clear();
        territoryService.clearCache();
        GroupData data = storage.load();
        if (data != null && data.getGroups() != null) {
            this.groups.putAll(data.getGroups());
            groups.values().forEach(this::cacheGroupData);
            LOGGER.atInfo().log("Loaded " + groups.size() + " groups.");
        }
    }

    public void reload(PlayerRef player) {
        loadGroups();
        notify(player, "Data is reloaded.", false);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void cacheGroupData(Group group) {
        namesGroups.add(group.getName().toLowerCase());
        tagsGroups.add(group.getTag().toLowerCase());
        group.getMembers().forEach(m -> playerGroupMap.put(m.getPlayerId(), group.getId()));
        territoryService.cacheGroupClaims(group);
    }

    public void saveGroups() {

        Map<UUID, Group> groupsCopy = new HashMap<>();
        for (Map.Entry<UUID, Group> entry : this.groups.entrySet()) {
            groupsCopy.put(entry.getKey(), entry.getValue().copy());
        }
        GroupData dataToSave = new GroupData(groupsCopy);

        executor.submit(() -> {
            try {
                storage.save(dataToSave);
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to save groups");
            }
        });
    }

    public void invitePlayer(PlayerRef sender, PlayerRef target) {
        membershipService.invitePlayer(sender, target);
    }

    public void createGroup(PlayerRef player, String name, String tag, @Nullable String color, @Nullable String desc) {
        if (playerGroupMap.containsKey(player.getUuid())) {
            notify(player, "You are already in a group.");
            return;
        }

        String safeName = normalize(name);
        String safeTag = normalize(tag);

        if (!validateIdentifier(player, safeName, getConfig().getMinNameLength(), getConfig().getMaxNameLength(),
                namesGroups, "Name"))
            return;
        if (!validateIdentifier(player, safeTag, getConfig().getMinTagLength(), getConfig().getMaxTagLength(),
                tagsGroups, "Tag"))
            return;
        if (color != null && !HEX_PATTERN.matcher(color).matches()) {
            notify(player, "Invalid color code.");
            return;
        }
        if (desc != null && desc.length() > getConfig().getMaxDescriptionLength()) {
            notify(player, "Description too long.");
            return;
        }

        Group group;
        desc = desc != null ? desc.substring(0, desc.length() - 1) : null;
        if ("GUILD".equalsIgnoreCase(getConfig().getPluginMode())) {
            group = new Guild(safeName, safeTag, desc, color, player);
        } else {
            group = new Faction(safeName, safeTag, desc, color, player);
        }

        groups.put(group.getId(), group);
        cacheGroupData(group);
        saveGroups();

        updateGroupMaps(group);

        notify(player, "Group created successfully!", false);
    }

    public void disband(PlayerRef player) {
        Group group = getGroupOrNotify(player);
        if (group == null || !isLeader(group, player))
            return;

        updateGroupMaps(group);

        namesGroups.remove(group.getName().toLowerCase());
        tagsGroups.remove(group.getTag().toLowerCase());
        group.getMembers().forEach(m -> {
            playerGroupMap.remove(m.getPlayerId());

            clearPlayerMapFilter(m.getPlayerId());
        });

        territoryService.uncacheGroupClaims(group);

        groups.remove(group.getId());
        saveGroups();
        notify(player, "Group deleted.", false);
    }

    public void leaveGroup(PlayerRef player) {
        membershipService.leaveGroup(player);
    }

    public void updateGroup(PlayerRef player, String type, String value) {
        Group group = getGroupOrNotify(player);
        if (group == null || !hasPerm(group, player, Permission.CAN_UPDATE_GROUP))
            return;

        switch (type.toLowerCase()) {
            case "name" -> {
                String sName = normalize(value);
                if (sName.equalsIgnoreCase(group.getName())) {
                    notify(player, "Group name is already set to this value.");
                    return;
                }
                if (validateIdentifier(player, sName, getConfig().getMinNameLength(), getConfig().getMaxNameLength(),
                        namesGroups, "Name")) {
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
                if (validateIdentifier(player, sTag, getConfig().getMinTagLength(), getConfig().getMaxTagLength(),
                        tagsGroups, "Tag")) {
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
                if (value.length() <= getConfig().getMaxDescriptionLength()) {
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

        if ("name".equalsIgnoreCase(type) || "tag".equalsIgnoreCase(type)) {

            if ("name".equalsIgnoreCase(type)) {
                namesGroups.remove(group.getName().toLowerCase());
                namesGroups.add(normalize(value).toLowerCase());
            } else if ("tag".equalsIgnoreCase(type)) {
                tagsGroups.remove(group.getTag().toLowerCase());
                tagsGroups.add(normalize(value).toLowerCase());
            }
            updateGroupMaps(group);
        }
    }

    public void transferLeadership(PlayerRef sender, UUID targetId) {
        membershipService.transferLeadership(sender, targetId);
    }

    public void acceptInvitation(PlayerRef player, String groupName) {
        membershipService.acceptInvitation(player, groupName);
    }

    public void kickMember(PlayerRef sender, UUID targetId) {
        membershipService.kickMember(sender, targetId);
    }

    public void setHome(PlayerRef sender, String name, World world) {
        territoryService.setHome(sender, name, world);
    }

    public void createRole(PlayerRef sender, String name, List<String> grants) {
        membershipService.createRole(sender, name, grants);
    }

    public void deleteRole(PlayerRef sender, String name) {
        membershipService.deleteRole(sender, name);
    }

    public void updateRole(PlayerRef sender, String name, List<String> addGrants, List<String> removeGrants) {
        membershipService.updateRole(sender, name, addGrants, removeGrants);
    }

    public void setRole(PlayerRef sender, UUID targetId, String roleName) {
        membershipService.setRole(sender, targetId, roleName);
    }

    public void claimChunk(PlayerRef sender, World world) {
        territoryService.claimChunk(sender, world);
    }

    public void teleportHome(PlayerRef sender, String name, Store<EntityStore> store, Ref<EntityStore> ref,
                             World world) {
        territoryService.teleportHome(sender, name, store, ref, world);
    }

    public void unclaimChunk(PlayerRef sender, World world) {
        territoryService.unclaimChunk(sender, world);
    }

    public void withdraw(PlayerRef sender, double amount) {
        economyService.withdraw(sender, amount);
    }

    public void withdrawFromGroup(PlayerRef sender, double amount) {
        economyService.withdrawFromGroup(sender, amount);
    }

    public void upgradeGuild(PlayerRef sender) {
        Group group = getGroupOrNotify(sender);
        if (!(group instanceof Guild guild)) {
            notify(sender, "Not a guild.");
            return;
        }
        if (!hasPerm(group, sender, Permission.CAN_UPGRADE_GUILD))
            return;

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

        if (group == null)
            return;

        String leaderName = Optional.ofNullable(group.getMember(group.getLeaderId()))
                .map(GroupMember::getPlayerName)
                .orElse("N/A");

        Color groupColor = group.getColor() != null ? Color.decode(group.getColor()) : Color.white;

        ChatFormatter.StyledText msg = ChatFormatter.of("#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#\n")
                .append("Name: ").append(group.getName() + "\n").withBold()
                .append("Tag: ").append(group.getTag() + "\n").withBold()
                .append("Color: ").append(group.getColor() != null ? group.getColor() + "\n" : "No color set" + "\n")
                .withBold().withColor(groupColor)
                .append("Description: ")
                .append(group.getDescription() != null ? group.getDescription() : "No description set" + "\n")
                .withBold()
                .append("\n")
                .append("Leader: ").append(leaderName + "\n").withBold()
                .append("Members: ")
                .append(group.getMembers().size() + " / "
                        + (group.getType().equals(FACTION) ? getConfig().getMaxSize()
                        : getConfig().getMaxSize()
                        + getConfig().getSlotQuantityGainForLevel() * ((Guild) group).getLevel())
                        + "\n")
                .withBold()
                .append("Claims: ")
                .append(group.getMembers().stream().anyMatch(a -> a.getPlayerId().equals(sender.getUuid()))
                        ? group.getClaims().size() + " / " + getConfig().getMaxClaimsPerFaction() + "\n"
                        : "Not allowed to see this info\n")
                .withBold()
                .append("Homes: ")
                .append(group.getMembers().stream().anyMatch(a -> a.getPlayerId().equals(sender.getUuid()))
                        ? group.getHomeCount() + "\n"
                        : "Not allowed to see this info\n")
                .withBold()
                .append("Bank: ")
                .append(group.getMembers().stream().anyMatch(a -> a.getPlayerId().equals(sender.getUuid()))
                        ? String.format("%.2f", group.getBankBalance()) + "\n"
                        : "Not allowed to see this info\n")
                .withBold()
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
                msg = msg.append(faction.isRaidable() ? "YES\n" : "NO\n").withBold()
                        .withColor(faction.isRaidable() ? Color.RED : Color.GREEN);
            } else {
                msg = msg.append("Not allowed to see this info\n").withBold();
            }
        }
        if (group instanceof Guild guild) {
            msg = msg.append("Level: ").append(String.valueOf(guild.getLevel()))
                    .append("MoneyToNextLevel: ")
                    .append(group.getMembers().stream().anyMatch(a -> a.getPlayerId().equals(sender.getUuid()))
                            ? guild.getMoneyToNextLevel() + "\n"
                            : "Not allowed to see this info\n")
                    .withBold();
        }
        msg = msg.append("#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#");

        sender.sendMessage(msg.toMessage());
    }

    public void listInvitations(PlayerRef sender) {
        membershipService.listInvitations(sender);
    }

    public void listRoles(PlayerRef sender) {
        membershipService.listRoles(sender);
    }

    public void listHomes(PlayerRef sender) {
        territoryService.listHomes(sender);
    }

    public void deleteHome(PlayerRef sender, String homeName) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !hasPerm(group, sender, Permission.CAN_MANAGE_HOME))
            return;

        if (!group.removeHome(homeName)) {
            notify(sender, "Home not found.");
            return;
        }
        saveGroups();
        notify(sender, "Home " + homeName + " deleted.", false);
    }

    public void setDefaultHome(PlayerRef sender, @Nullable String homeName) {
        Group group = getGroupOrNotify(sender);
        if (group == null)
            return;

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
        diplomacyService.setDiplomacy(sender, targetGroupName, status);
    }

    public void listDiplomacy(PlayerRef sender) {
        diplomacyService.listDiplomacy(sender);
    }

    public void deposit(PlayerRef sender, double amount) {
        economyService.deposit(sender, amount);
    }

    public void depositToGroup(PlayerRef sender, double amount) {
        economyService.depositToGroup(sender, amount);
    }

    public void getBalance(PlayerRef sender, @Nullable String type) {
        economyService.getBalance(sender, type);
    }

    public void getPower(PlayerRef sender, @Nullable String type) {
        Group group = getGroupOrNotify(sender);
        if (group == null)
            return;

        if (group instanceof dzve.model.Faction faction) {
            if (type == null || type.equalsIgnoreCase("player")) {
                double playerPower = faction.getPlayerPower(sender.getUuid());
                notify(sender, "Your power: " + playerPower, false);
            } else if (type.equalsIgnoreCase("group")) {
                notify(sender, "Faction Total Power: " + faction.getTotalPower(), false);
            } else {
                notify(sender, "Invalid type. Use 'player' or 'group'.");
            }
        } else {
            notify(sender, "Power system is only available for factions.", false);
        }
    }

    public void showClaimMap(PlayerRef player, World world) {
        territoryService.showClaimMap(player, world);
    }

    public boolean hasPerm(Group g, PlayerRef p, Permission perm) {
        if (g.isLeader(p.getUuid()))
            return true;
        GroupRole r = getMemberRole(g, p.getUuid());
        if (r != null && r.hasPermission(perm)) {
            return true;
        }
        notify(p, "No Permission.");
        return false;
    }

    private GroupRole getMemberRole(Group g, UUID pid) {
        GroupMember member = g.getMember(pid);
        if (member == null)
            return null;
        return g.getRoles().stream().filter(r -> r.getId().equals(member.getRoleId())).findFirst().orElse(null);
    }

    @Nullable
    public Group getGroupOrNotify(PlayerRef p) {
        UUID gid = playerGroupMap.get(p.getUuid());
        if (gid == null)
            notify(p, "You are not in a group.");
        return gid != null ? groups.get(gid) : null;
    }

    public Group getGroup(UUID id) {
        return groups.get(id);
    }

    public void updatePlayerGroupMap(UUID playerId, UUID groupId) {
        playerGroupMap.put(playerId, groupId);
    }

    public void removePlayerFromGroupMap(UUID playerId) {
        playerGroupMap.remove(playerId);
    }

    @Nullable
    public Group getPlayerGroup(UUID playerUuid) {
        UUID groupId = playerGroupMap.get(playerUuid);
        if (groupId != null) {
            return groups.get(groupId);
        }
        return null;
    }

    private boolean isLeader(Group g, PlayerRef p) {
        if (g.isLeader(p.getUuid())) {
            return true;
        }
        notify(p, "Leader only.");
        return false;
    }

    public void notify(PlayerRef player, String msg) {
        notify(player, msg, true);
    }

    public void notify(PlayerRef player, String msg, boolean isError) {
        notificationService.sendNotification(player.getUuid(), msg, isError ? Danger : Success);
    }

    private boolean validateIdentifier(PlayerRef player, String val, int min, int max, Set<String> cache,
                                       String field) {
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

    public void sendGroupMessage(PlayerRef sender, String[] message) {
        Group group = getGroupOrNotify(sender);
        if (group == null)
            return;

        String[] messageContent = message.length > 1 ? Arrays.copyOfRange(message, 1, message.length) : new String[0];

        ChatFormatter.StyledText styledMessage = ChatFormatter.of("[GroupChat]")
                .withColor(Color.decode(group.getColor()))
                .withBold()
                .withMonospace()
                .append(sender.getUsername() + ": ")
                .append(String.join(" ", messageContent));

        Universe.get().getPlayers().stream()
                .filter(player -> group.getMembers().stream()
                        .anyMatch(member -> member.getPlayerId().equals(player.getUuid())))
                .forEach(player -> player.sendMessage(styledMessage.toMessage()));
    }

    public void sendAllyMessage(PlayerRef sender, String[] message) {
        diplomacyService.sendAllyMessage(sender, message);
    }

    public Group getGroupByName(String name) {
        if (name == null)
            return null;
        return groups.values().stream()
                .filter(g -> g.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public Group getGroupByChunk(String worldName, int chunkX, int chunkZ) {
        return territoryService.getGroupByChunk(worldName, chunkX, chunkZ);
    }

    public void updateGroupMaps(Group group) {
        if (group == null)
            return;

        Universe universe = Universe.get();
        if (universe == null)
            return;

        for (GroupMember member : group.getMembers()) {
            PlayerRef playerRef = universe.getPlayer(member.getPlayerId());
            if (playerRef != null) {
                Player player = Objects.requireNonNull(playerRef.getReference()).getStore().getComponent(
                        playerRef.getReference(),
                        Player.getComponentType());
                WorldMapTracker mapTracker = null;
                if (player != null) {
                    mapTracker = player.getWorldMapTracker();
                }
                if (mapTracker != null) {
                    MapUtils.updateMapFilter(mapTracker, member.getPlayerId(), this);
                }
            }
        }
    }

    public void clearPlayerMapFilter(UUID playerId) {
        Universe universe = Universe.get();
        if (universe == null)
            return;

        universe.getPlayers().forEach(playerRef -> {
            if (playerRef.getUuid().equals(playerId)) {
                Player player = Objects.requireNonNull(playerRef.getReference()).getStore().getComponent(
                        playerRef.getReference(),
                        Player.getComponentType());
                WorldMapTracker mapTracker = null;
                if (player != null) {
                    mapTracker = player.getWorldMapTracker();
                }
                if (mapTracker != null) {
                    MapUtils.clearMapFilter(mapTracker, playerId);
                }
            }
        });
    }

    private record ChunkInfo(Group group, int cx, int cz, String world) {
    }

}
