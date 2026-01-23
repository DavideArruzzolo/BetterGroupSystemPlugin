package dzve.service.group;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dzve.config.BetterGroupSystemPluginConfig;
import dzve.model.*;
import dzve.service.JsonStorage;
import dzve.service.NotificationService;
import lombok.Getter;

import javax.annotation.Nullable;
import java.io.File;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.hypixel.hytale.protocol.packets.interface_.NotificationStyle.Danger;
import static com.hypixel.hytale.protocol.packets.interface_.NotificationStyle.Success;
import static dzve.config.BetterGroupSystemPluginConfig.DATA_FOLDER;
import static dzve.config.BetterGroupSystemPluginConfig.FILE_NAME;
import static dzve.model.GroupType.FACTION;

public class GroupService {

    @Getter
    private static class GroupData {
        private final Map<UUID, Group> groups;

        public GroupData(Map<UUID, Group> groups) {
            this.groups = groups;
        }
    }

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final GroupService instance = new GroupService();
    private static final NotificationService notificationService = NotificationService.getInstance();
    private static final BetterGroupSystemPluginConfig config = BetterGroupSystemPluginConfig.getInstance();

    private final JsonStorage<GroupData> storage;
    private final Map<UUID, Group> groups = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerGroupMap = new ConcurrentHashMap<>(); // PlayerUUID -> GroupUUID
    private final Map<UUID, Set<UUID>> invitations = new ConcurrentHashMap<>(); // PlayerUUID -> Set<GroupUUID>

    // Cache unicità
    private final Set<String> namesGroups = ConcurrentHashMap.newKeySet();
    private final Set<String> tagsGroups = ConcurrentHashMap.newKeySet();

    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}_]+$");
    private static final Pattern HEX_PATTERN = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private GroupService() {
        this.storage = new JsonStorage<>(new File(DATA_FOLDER, FILE_NAME), GroupData.class);
        loadGroups();
    }

    public static synchronized GroupService getInstance() {
        return instance;
    }

    // --- Core Logic ---

    private void loadGroups() {
        GroupData data = storage.load();
        if (data != null && data.getGroups() != null) {
            this.groups.putAll(data.getGroups());
            groups.values().forEach(this::cacheGroupData);
            LOGGER.atInfo().log("Loaded " + groups.size() + " groups.");
        }
    }

    private void cacheGroupData(Group group) {
        namesGroups.add(group.getName().toLowerCase());
        tagsGroups.add(group.getTag().toLowerCase());
        group.getMembers().forEach(m -> playerGroupMap.put(m.getPlayerId(), group.getId()));
    }

    public void saveGroups() {
        storage.saveAsync(new GroupData(new HashMap<>(groups)));
    }

    /* --- I. Management Commands --- */

    public void createGroup(PlayerRef player, String name, String tag, @Nullable String color, @Nullable String desc) {
        UUID pid = player.getUuid();
        if (playerGroupMap.containsKey(pid)) {
            notify(pid, "You are already in a group.");
            return;
        }

        String safeName = normalize(name);
        String safeTag = normalize(tag);

        if (!validateIdentifier(pid, safeName, config.getMinNameLength(), config.getMaxNameLength(), namesGroups, "Name"))
            return;
        if (!validateIdentifier(pid, safeTag, config.getMinTagLength(), config.getMaxTagLength(), tagsGroups, "Tag"))
            return;
        if (color != null && !HEX_PATTERN.matcher(color).matches()) {
            notify(pid, "Invalid color code.");
            return;
        }
        if (desc != null && desc.length() > config.getMaxDescriptionLength()) {
            notify(pid, "Description too long.");
            return;
        }

        Group group;
        if ("GUILD".equalsIgnoreCase(config.getPluginMode())) {
            group = new Guild(safeName, safeTag, desc, color, player);
        } else {
            group = new Faction(safeName, safeTag, desc, color, player);
        }

        groups.put(group.getId(), group);
        cacheGroupData(group);
        saveGroups();
        notify(pid, "Group created successfully!", false);
    }

    public void deleteGroup(PlayerRef player) {
        Group group = getGroupOrNotify(player);
        if (group == null || !checkLeader(group, player)) return;

        namesGroups.remove(group.getName().toLowerCase());
        tagsGroups.remove(group.getTag().toLowerCase());
        group.getMembers().forEach(m -> playerGroupMap.remove(m.getPlayerId()));

        groups.remove(group.getId());
        saveGroups();
        notify(player.getUuid(), "Group deleted.", false);
    }

    public void leaveGroup(PlayerRef player) {
        Group group = getGroupOrNotify(player);
        if (group == null) return;
        if (group.isLeader(player.getUuid()) && group.getMemberCount() > 1) {
            notify(player.getUuid(), "Leader cannot leave. Transfer ownership first.");
            return;
        }

        if (group.getMemberCount() <= 1) {
            deleteGroup(player);
        } else {
            group.removeMember(player.getUuid());
            playerGroupMap.remove(player.getUuid());
            saveGroups();
            notify(player.getUuid(), "You left the group.", false);
        }
    }

    public void updateGroup(PlayerRef player, String type, String value) { //TODO sbagliato tutti i parametri posso essere insieme
        Group group = getGroupOrNotify(player);
        if (group == null || !checkPerm(group, player, Permission.CAN_UPDATE_GROUP)) return;

        UUID pid = player.getUuid();
        switch (type.toLowerCase()) {
            case "name" -> {
                String sName = normalize(value);
                if (validateIdentifier(pid, sName, config.getMinNameLength(), config.getMaxNameLength(), namesGroups, "Name")) {
                    namesGroups.remove(group.getName().toLowerCase());
                    group.setName(sName);
                    namesGroups.add(sName.toLowerCase());
                }
            }
            case "tag" -> {
                String sTag = normalize(value);
                if (validateIdentifier(pid, sTag, config.getMinTagLength(), config.getMaxTagLength(), tagsGroups, "Tag")) {
                    tagsGroups.remove(group.getTag().toLowerCase());
                    group.setTag(sTag);
                    tagsGroups.add(sTag.toLowerCase());
                }
            }
            case "color" -> {
                if (HEX_PATTERN.matcher(value).matches()) group.setColor(value);
                else notify(pid, "Invalid hex color.");
            }
            case "desc" -> {
                if (value.length() <= config.getMaxDescriptionLength()) group.setDescription(value);
                else notify(pid, "Desc too long.");
            }
        }
        saveGroups();
    }

    /* --- II. Member Commands --- */

    public void invitePlayer(PlayerRef sender, PlayerRef target) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !checkPerm(group, sender, Permission.CAN_INVITE)) return;

        if (playerGroupMap.containsKey(target.getUuid())) {
            notify(sender.getUuid(), "Player already in a group.");
            return;
        }
        if (group.getMemberCount() >= (group.getType().equals(FACTION) ? config.getMaxSize() : config.getMaxSize() + config.getSlotQuantityGainForLevel() * group.getLevel())) {
            notify(sender.getUuid(), "Group full.");
            return;
        }

        invitations.computeIfAbsent(target.getUuid(), k -> ConcurrentHashMap.newKeySet()).add(group.getId());
        notify(sender.getUuid(), "Invited " + target.getUsername(), false);
    }

    public void acceptInvitation(PlayerRef player, String groupName) {
        if (playerGroupMap.containsKey(player.getUuid())) {
            notify(player.getUuid(), "Already in a group.");
            return;
        }

        Group group = groups.values().stream().filter(g -> g.getName().equalsIgnoreCase(groupName)).findFirst().orElse(null);
        if (group == null) {
            notify(player.getUuid(), "Group not found.");
            return;
        }

        Set<UUID> invites = invitations.get(player.getUuid());
        if (invites == null || !invites.contains(group.getId())) {
            notify(player.getUuid(), "No invitation from this group.");
            return;
        }
        if (group.getMemberCount() >= config.getMaxSize()) {
            notify(player.getUuid(), "Group is full.");
            return;
        }

        // Logic
        GroupRole defaultRole = group.getRoles().stream().filter(GroupRole::isDefault).findFirst().orElseThrow();
        group.addMember(player, defaultRole.getId());
        playerGroupMap.put(player.getUuid(), group.getId());
        invites.remove(group.getId());
        saveGroups();
        notify(player.getUuid(), "Joined " + group.getName(), false);
    }

    public void kickMember(PlayerRef sender, UUID targetId) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !checkPerm(group, sender, Permission.CAN_KICK)) return;
        if (!group.isMember(targetId)) {
            notify(sender.getUuid(), "Target not in group.");
            return;
        }
        if (sender.getUuid().equals(targetId)) {
            notify(sender.getUuid(), "Cannot kick self.");
            return;
        }

        if (!canModify(group, sender.getUuid(), targetId)) {
            notify(sender.getUuid(), "Target rank too high.");
            return;
        }

        group.removeMember(targetId);
        playerGroupMap.remove(targetId);
        saveGroups();
        notify(sender.getUuid(), "Member kicked.", false);
    }

    public void transferLeadership(PlayerRef sender, UUID targetId, boolean confirm) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !checkLeader(group, sender)) return;
        if (!group.isMember(targetId)) {
            notify(sender.getUuid(), "Target not in group.");
            return;
        }
        if (!confirm) {
            notify(sender.getUuid(), "Confirm with /group transfer <name> confirm");
            return;
        }

        GroupRole leaderRole = getRoleByPriority(group, Integer.MAX_VALUE);
        GroupRole memberRole = getRoleByPriority(group, 50); // Fallback to member

        group.setLeaderId(targetId);
        group.changeMemberRole(targetId, leaderRole.getId());
        group.changeMemberRole(sender.getUuid(), memberRole.getId());
        saveGroups();
        notify(sender.getUuid(), "Leadership transferred.", false);
    }

    // --- III. Role Commands ---

    public void createRole(PlayerRef sender, String name, List<String> grants) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !checkPerm(group, sender, Permission.CAN_MANAGE_ROLE)) return;
        if (group.getRoles().size() >= 10) {
            notify(sender.getUuid(), "Max roles reached.");
            return;
        }
        if (getRoleByName(group, name) != null) {
            notify(sender.getUuid(), "Role exists.");
            return;
        }

        Set<Permission> perms = parsePerms(grants);
        if (perms == null) {
            notify(sender.getUuid(), "Invalid permissions.");
            return;
        }

        modifyRoles(group, roles -> roles.add(new GroupRole(name, name, 10, false, perms)));
        notify(sender.getUuid(), "Role created.", false);
    }

    public void deleteRole(PlayerRef sender, String name) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !checkPerm(group, sender, Permission.CAN_MANAGE_ROLE)) return;

        GroupRole role = getRoleByName(group, name);
        if (role == null || role.isDefault()) {
            notify(sender.getUuid(), "Invalid role.");
            return;
        }
        if (group.getMembers().stream().anyMatch(m -> m.getRoleId().equals(role.getId()))) {
            notify(sender.getUuid(), "Role is in use.");
            return;
        }

        modifyRoles(group, roles -> roles.remove(role));
        notify(sender.getUuid(), "Role deleted.", false);
    }

    public void modifyRolePerms(PlayerRef sender, String name, List<String> grants) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !checkPerm(group, sender, Permission.CAN_MANAGE_ROLE)) return;

        GroupRole role = getRoleByName(group, name);
        if (role == null || role.isDefault()) {
            notify(sender.getUuid(), "Cannot edit this role.");
            return;
        }

        Set<Permission> perms = parsePerms(grants);
        if (perms != null) {
            role.setPermissions(perms);
            saveGroups();
            notify(sender.getUuid(), "Role permissions updated.", false);
        }
    }

    public void setRole(PlayerRef sender, UUID targetId, String roleName) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !checkPerm(group, sender, Permission.CAN_CHANGE_ROLE)) return;
        if (!group.isMember(targetId)) {
            notify(sender.getUuid(), "Target not in group.");
            return;
        }
        if (!canModify(group, sender.getUuid(), targetId)) {
            notify(sender.getUuid(), "Hierarchy prevents this.");
            return;
        }

        GroupRole role = getRoleByName(group, roleName);
        if (role == null) {
            notify(sender.getUuid(), "Role not found.");
            return;
        }

        // Prevent promoting above self
        GroupRole senderRole = getMemberRole(group, sender.getUuid());
        if (!group.isLeader(sender.getUuid()) && role.getPriority() >= senderRole.getPriority()) {
            notify(sender.getUuid(), "Cannot promote to rank >= yours.");
            return;
        }

        group.changeMemberRole(targetId, role.getId());
        saveGroups();
    }

    // --- IV. Territory & V. Economy ---

    public void setHome(PlayerRef sender, String name) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !checkPerm(group, sender, Permission.CAN_MANAGE_HOME)) return;
        if (group.getHomeCount() >= config.getMaxHome()) {
            notify(sender.getUuid(), "Max homes reached.");
            return;
        }
        if (group.getHome(name) != null) {
            notify(sender.getUuid(), "Home exists.");
            return;
        }

        int cx = (int) sender.getLocation().getX() >> 4;
        int cz = (int) sender.getLocation().getZ() >> 4;
        if (!group.isChunkClaimed(cx, cz, sender.getLocation().getWorldName())) {
            notify(sender.getUuid(), "Must be in claimed land.");
            return;
        }

        group.addHome(new GroupHome(name, sender.getLocation().getWorldName(), sender.getLocation().getX(), sender.getLocation().getY(), sender.getLocation().getZ(), sender.getLocation().getYaw(), sender.getLocation().getPitch()));
        saveGroups();
    }

    public void teleportHome(PlayerRef sender, String name) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !checkPerm(group, sender, Permission.CAN_TELEPORT_HOME)) return;

        String target = (name == null) ? (group.getHomeCount() == 1 ? group.getHomes().iterator().next().getName() : "default") : name;
        GroupHome home = group.getHome(target);

        if (home != null) {
            // sender.teleport(...) implementation required here
            notify(sender.getUuid(), "Teleporting to " + target + "...", false);
        } else {
            notify(sender.getUuid(), "Home not found.");
        }
    }

    public void claimChunk(PlayerRef sender) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !checkPerm(group, sender, Permission.CAN_MANAGE_CLAIM)) return;

        int cx = (int) sender.getLocation().getX() >> 4;
        int cz = (int) sender.getLocation().getZ() >> 4;
        String world = sender.getLocation().getWorldName();

        // Global check (is already claimed by ANY group?)
        boolean taken = groups.values().stream().anyMatch(g -> g.isChunkClaimed(cx, cz, world));
        if (taken) {
            notify(sender.getUuid(), "Chunk already claimed.");
            return;
        }

        // Faction specific checks
        if (group instanceof Faction f) {
            if (f.getClaims().size() >= f.getMaxClaims(config.getClaimRatio())) {
                notify(sender.getUuid(), "Not enough power.");
                return;
            }
        } else if (group.getClaims().size() >= config.getMaxClaimsPerFaction()) { // Generic limit
            notify(sender.getUuid(), "Claim limit reached.");
            return;
        }

        group.addClaim(new GroupClaimedChunk(cx, cz, world));
        saveGroups();
        notify(sender.getUuid(), "Land claimed!", false);
    }

    public void withdraw(PlayerRef sender, double amount) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !checkPerm(group, sender, Permission.CAN_MANAGE_BANK)) return;
        if (amount <= 0 || !group.withdraw(amount)) {
            notify(sender.getUuid(), "Invalid amount or insufficient funds.");
            return;
        }

        // EconomyService.add(sender, amount);
        saveGroups();
        notify(sender.getUuid(), "Withdrawn " + amount, false);
    }

    public void upgradeGuild(PlayerRef sender) {
        Group group = getGroupOrNotify(sender);
        if (!(group instanceof Guild guild)) {
            notify(sender.getUuid(), "Not a guild.");
            return;
        }
        if (!checkPerm(group, sender, Permission.CAN_UPGRADE_GUILD)) return;

        if (!guild.canUpgrade()) {
            notify(sender.getUuid(), "Cannot upgrade (Max level or Insufficient funds).");
            return;
        }

        guild.withdraw(guild.calculateCostToNextLevel());
        guild.setLevel(guild.getLevel() + 1);
        saveGroups();
        notify(sender.getUuid(), "Guild Level Up: " + guild.getLevel(), false);
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
                notify(sender.getUuid(), "Group not found.");
                return;
            }
        }

        if (group == null) return; // Caso sender senza gruppo e senza target

        // Costruisci il messaggio info (Placeholder per la formattazione reale)
        String info = String.format("Group: %s [%s] | Leader: %s | Balance: %.2f | Members: %d/%d",
                group.getName(), group.getTag(), group.getLeaderId(), group.getBankBalance(),
                group.getMemberCount(), config.getMaxSize());

        notify(sender.getUuid(), info, false);
    }

    public void listInvitations(PlayerRef sender) {
        Set<UUID> invites = invitations.get(sender.getUuid());
        if (invites == null || invites.isEmpty()) {
            notify(sender.getUuid(), "No pending invitations.", false);
            return;
        }

        String inviteNames = invites.stream()
                .map(groups::get)
                .filter(Objects::nonNull)
                .map(Group::getName)
                .collect(Collectors.joining(", "));

        notify(sender.getUuid(), "Invited by: " + inviteNames, false);
    }

    public void listRoles(PlayerRef sender) {
        Group group = getGroupOrNotify(sender);
        if (group == null) return;

        String roleList = group.getRoles().stream()
                .sorted(Comparator.comparingInt(GroupRole::getPriority))
                .map(r -> r.getName() + " (P:" + r.getPriority() + ")")
                .collect(Collectors.joining(", "));

        notify(sender.getUuid(), "Roles: " + roleList, false);
    }

    // --- Territory Extension ---

    public void unclaimChunk(PlayerRef sender) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !checkPerm(group, sender, Permission.CAN_MANAGE_CLAIM)) return;

        int cx = (int) sender.getLocation().getX() >> 4;
        int cz = (int) sender.getLocation().getZ() >> 4;
        String world = sender.getLocation().getWorldName();

        if (!group.isChunkClaimed(cx, cz, world)) {
            notify(sender.getUuid(), "This land is not claimed by your group.");
            return;
        }

        group.removeClaim(cx, cz, world);
        saveGroups();
        notify(sender.getUuid(), "Land unclaimed.", false);
    }

    public void deleteHome(PlayerRef sender, String homeName) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !checkPerm(group, sender, Permission.CAN_MANAGE_HOME)) return;

        if (!group.removeHome(homeName)) {
            notify(sender.getUuid(), "Home not found.");
            return;
        }
        saveGroups();
        notify(sender.getUuid(), "Home " + homeName + " deleted.", false);
    }

    public void setDefaultHome(PlayerRef sender, String homeName) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !checkPerm(group, sender, Permission.CAN_MANAGE_HOME)) return;

        GroupHome home = group.getHome(homeName);
        if (home == null) {
            notify(sender.getUuid(), "Home not found.");
            return;
        }

        // Rimuove la vecchia default e ricrea la nuova con nome "default" o usa un flag nel modello GroupHome
        // Per semplicità qui assumiamo che "default" sia un nome riservato, quindi rinominiamo.
        // Implementazione suggerita: Aggiungere un campo 'isDefault' in GroupHome o gestire l'alias.
        // Qui simulo scambiando il nome per aderire alla logica 'teleportHome' che cerca "default".

        GroupHome oldDefault = group.getHome("default");
        if (oldDefault != null) oldDefault.setName("old_default_" + System.currentTimeMillis());

        home.setName("default");
        saveGroups();
        notify(sender.getUuid(), "Home " + homeName + " is now default.", false);
    }

    // --- Diplomacy ---

    public void setDiplomacy(PlayerRef sender, String targetGroupName, DiplomacyStatus status) {
        Group group = getGroupOrNotify(sender);
        if (group == null || !checkPerm(group, sender, Permission.CAN_MANAGE_DIPLOMACY)) return;

        Group target = groups.values().stream()
                .filter(g -> g.getName().equalsIgnoreCase(targetGroupName))
                .findFirst().orElse(null);

        if (target == null) {
            notify(sender.getUuid(), "Target group not found.");
            return;
        }
        if (target.getId().equals(group.getId())) {
            notify(sender.getUuid(), "Cannot change relations with yourself.");
            return;
        }

        if (status == DiplomacyStatus.ALLY) {
            // Qui servirebbe un sistema di "Richiesta Alleanza" simile agli inviti.
            // Per ora lo setto diretto come da tua richiesta semplificata, ma idealmente è bidirezionale.
            notify(sender.getUuid(), "Alliance request sent (Not implemented fully).", false);
        } else {
            // Neutral o Enemy si possono settare unilateralmente
            group.setDiplomacyStatus(target.getId(), status);
            saveGroups();
            notify(sender.getUuid(), "Diplomacy with " + target.getName() + " set to " + status, false);
        }
    }

    public void listDiplomacy(PlayerRef sender) {
        Group group = getGroupOrNotify(sender);
        if (group == null) return;

        String relations = group.getDiplomaticRelations().entrySet().stream()
                .map(e -> {
                    Group g = groups.get(e.getKey());
                    return (g != null ? g.getName() : "Unknown") + ": " + e.getValue();
                })
                .collect(Collectors.joining(", "));

        notify(sender.getUuid(), "Relations: " + (relations.isEmpty() ? "None" : relations), false);
    }

    // --- Economy Extension ---

    public void deposit(PlayerRef sender, double amount) {
        if (amount <= 0) {
            notify(sender.getUuid(), "Amount must be positive.");
            return;
        }

        Group group = getGroupOrNotify(sender);
        if (group == null) return;

        // TODO: Integrare check: if (EconomyService.remove(sender, amount)) ...

        group.deposit(amount);
        if (group instanceof Guild guild) {
            guild.getMoneyContributions().merge(sender.getUuid(), amount, Double::sum);
        }

        saveGroups();
        notify(sender.getUuid(), "Deposited " + amount, false);
    }

    public void getBalance(PlayerRef sender) {
        Group group = getGroupOrNotify(sender);
        if (group == null) return;
        // Permesso opzionale: checkPerm(group, sender, Permission.CAN_VIEW_BANK)
        notify(sender.getUuid(), "Bank Balance: " + group.getBankBalance(), false);
    }

    // --- Helpers (Optimization) ---

    @Nullable
    private Group getGroupOrNotify(PlayerRef p) {
        UUID gid = playerGroupMap.get(p.getUuid());
        if (gid == null) notify(p.getUuid(), "You are not in a group.");
        return gid != null ? groups.get(gid) : null;
    }

    private boolean checkPerm(Group g, PlayerRef p, Permission perm) {
        if (g.isLeader(p.getUuid())) return true;
        GroupRole r = getMemberRole(g, p.getUuid());
        if (r == null || !r.hasPermission(perm)) {
            notify(p.getUuid(), "No Permission.");
            return false;
        }
        return true;
    }

    private boolean checkLeader(Group g, PlayerRef p) {
        if (!g.isLeader(p.getUuid())) {
            notify(p.getUuid(), "Leader only.");
            return false;
        }
        return true;
    }

    private void notify(UUID uuid, String msg) {
        notify(uuid, msg, true);
    }

    private void notify(UUID uuid, String msg, boolean isError) {
        notificationService.sendNotification(uuid, msg, isError ? Danger : Success); // Simplification
    }

    private boolean validateIdentifier(UUID pid, String val, int min, int max, Set<String> cache, String field) {
        if (val.length() < min || val.length() > max) {
            notify(pid, field + " length invalid.");
            return false;
        }
        if (!NAME_PATTERN.matcher(val).matches()) {
            notify(pid, field + " contains invalid chars.");
            return false;
        }
        if (cache.contains(val.toLowerCase())) {
            notify(pid, field + " already taken.");
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

    private void modifyRoles(Group g, java.util.function.Consumer<Set<GroupRole>> modifier) {
        Set<GroupRole> mutable = new HashSet<>(g.getRoles());
        modifier.accept(mutable);
        g.setRoles(mutable);
        saveGroups();
    }

    private Set<Permission> parsePerms(List<String> list) {
        if (list == null) return null;
        try {
            return list.stream().map(s -> Permission.valueOf(s.toUpperCase().replace(".", "_"))).collect(Collectors.toSet());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}