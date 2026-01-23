package dzve.service.group;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.world.location.Location;
import dzve.config.BetterGroupSystemPluginConfig;
import dzve.model.*;
import dzve.service.JsonStorage;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static dzve.config.BetterGroupSystemPluginConfig.DATA_FOLDER;
import static dzve.config.BetterGroupSystemPluginConfig.FILE_NAME;

public class GroupService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static GroupService instance;
    private final JsonStorage<GroupData> storage;
    private final Map<UUID, Group> groups;
    private final Map<UUID, GroupMember> onlinePlayerCache = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> invitations = new ConcurrentHashMap<>();
    // private final EconomyService economyService = EconomyService.getInstance();

    private GroupService() {
        File dataFolder = new File(DATA_FOLDER);
        File groupsFile = new File(dataFolder, FILE_NAME);
        this.storage = new JsonStorage<>(groupsFile, GroupData.class);
        this.groups = new HashMap<>();
        loadGroups();
    }

    public static synchronized GroupService getInstance() {
        if (instance == null) {
            instance = new GroupService();
        }
        return instance;
    }

    private void loadGroups() {
        GroupData data = storage.load();
        if (data != null && data.getGroups() != null) {
            this.groups.putAll(data.getGroups());
            LOGGER.atInfo().log("Loaded " + groups.size() + " groups from storage.");
        } else {
            LOGGER.atInfo().log("No existing group data found. Starting fresh.");
        }
    }

    public void saveGroups() {
        GroupData data = new GroupData(new HashMap<>(groups));
        storage.saveAsync(data);
    }

    public void saveGroupsSync() {
        GroupData data = new GroupData(new HashMap<>(groups));
        storage.saveSync(data);
    }

    public void shutdown() {
        saveGroupsSync();
        storage.shutdown();
    }

    public GroupMember getGroupMember(UUID playerId) {
        if (onlinePlayerCache.containsKey(playerId)) {
            return onlinePlayerCache.get(playerId);
        }
        for (Group group : groups.values()) {
            GroupMember member = group.getMember(playerId);
            if (member != null) {
                onlinePlayerCache.put(playerId, member);
                return member;
            }
        }
        return null;
    }

    public Group getGroupForPlayer(UUID playerId) {
        GroupMember member = getGroupMember(playerId);
        return (member != null && member.getGroupId() != null) ? groups.get(member.getGroupId()) : null;
    }

    public void addGroup(Group group) {
        groups.put(group.getId(), group);
        saveGroups();
    }

    public Group getGroup(UUID groupId) {
        return groups.get(groupId);
    }

    public void removeGroup(UUID groupId) {
        groups.remove(groupId);
        saveGroups();
    }

    public boolean isNameTaken(String name) {
        return groups.values().stream().anyMatch(g -> g.getName().equalsIgnoreCase(name));
    }

    public boolean isTagTaken(String tag) {
        return groups.values().stream().anyMatch(g -> g.getTag().equalsIgnoreCase(tag));
    }

    public Group createGroup(String name, String tag, String color, String description, UUID leaderId) {
        Group group = new Group(name, tag, description, color, leaderId);
        getLeaderRole().ifPresent(role -> group.addMember(leaderId, role.getId()));
        addGroup(group);
        return group;
    }

    public Group findGroupByNameOrTag(String identifier) {
        return groups.values().stream()
                .filter(g -> g.getName().equalsIgnoreCase(identifier) || g.getTag().equalsIgnoreCase(identifier))
                .findFirst().orElse(null);
    }

    public void disbandGroup(UUID groupId) {
        removeGroup(groupId);
    }

    public boolean deleteGroup(UUID playerId) {
        Group group = getGroupForPlayer(playerId);
        if (group == null || !group.isLeader(playerId)) {
            return false;
        }
        disbandGroup(group.getId());
        return true;
    }

    public boolean leaveGroup(UUID playerId) {
        Group group = getGroupForPlayer(playerId);
        if (group == null) return false;

        if (group.isLeader(playerId)) {
            if (group.getMemberCount() > 1) {
                return false; // Must transfer leadership first
            }
            disbandGroup(group.getId());
        } else {
            group.removeMember(playerId);
            saveGroups();
        }
        return true;
    }

    public void invitePlayer(UUID targetId, UUID groupId) {
        invitations.computeIfAbsent(targetId, k -> ConcurrentHashMap.newKeySet()).add(groupId);
    }

    public Set<UUID> getInvitations(UUID targetId) {
        return invitations.getOrDefault(targetId, Collections.emptySet());
    }

    public boolean acceptInvitation(UUID targetId, String groupIdentifier) {
        Group group = findGroupByNameOrTag(groupIdentifier);
        if (group == null) {
            return false;
        }
        UUID groupId = group.getId();

        Set<UUID> playerInvitations = invitations.get(targetId);
        if (playerInvitations == null || !playerInvitations.contains(groupId)) {
            return false; // No invitation from this group
        }

        if (group.getMemberCount() >= BetterGroupSystemPluginConfig.getInstance().getMaxSize()) {
            playerInvitations.remove(groupId);
            if (playerInvitations.isEmpty()) {
                invitations.remove(targetId);
            }
            return false; // Group is full
        }

        getRecruitRole().ifPresent(role -> group.addMember(targetId, role.getId()));
        saveGroups();

        // Clear all invitations for the player now that they've joined a group
        invitations.remove(targetId);
        return true;
    }

    public boolean kickMember(UUID targetId, UUID actorId) {
        Group group = getGroupForPlayer(actorId);
        if (group == null || !group.isMember(targetId)) return false;

        GroupMember targetMember = group.getMember(targetId);
        GroupMember actorMember = group.getMember(actorId);
        if (targetMember == null || actorMember == null || targetId.equals(actorId)) return false;

        Optional<GroupRole> targetRole = getRole(group, targetMember.getRoleId());
        Optional<GroupRole> actorRole = getRole(group, actorMember.getRoleId());

        if (targetRole.isEmpty() || actorRole.isEmpty() || !actorRole.get().canManage(targetRole.get())) {
            return false;
        }

        group.removeMember(targetId);
        saveGroups();
        return true;
    }

    public boolean transferLeadership(UUID newLeaderId, UUID oldLeaderId) {
        Group group = getGroupForPlayer(oldLeaderId);
        if (group != null && group.isLeader(oldLeaderId) && group.isMember(newLeaderId)) {
            group.setLeaderId(newLeaderId);
            getLeaderRole().ifPresent(role -> group.changeMemberRole(newLeaderId, role.getId()));
            getOfficerRole().ifPresent(role -> group.changeMemberRole(oldLeaderId, role.getId()));
            saveGroups();
            return true;
        }
        return false;
    }

    public boolean hasPermission(UUID playerId, Permission permission) {
        Group group = getGroupForPlayer(playerId);
        if (group == null) return false;
        GroupMember member = group.getMember(playerId);
        if (member == null) return false;
        return getRole(group, member.getRoleId()).map(role -> role.hasPermission(permission)).orElse(false);
    }

    public boolean promotePlayer(UUID targetId, UUID actorId) {
        Group group = getGroupForPlayer(actorId);
        if (group == null || !group.isMember(targetId)) return false;

        GroupMember targetMember = group.getMember(targetId);
        GroupMember actorMember = group.getMember(actorId);
        if (targetMember == null || actorMember == null) return false;

        Optional<GroupRole> targetRoleOpt = getRole(group, targetMember.getRoleId());
        Optional<GroupRole> actorRoleOpt = getRole(group, actorMember.getRoleId());
        if (targetRoleOpt.isEmpty() || actorRoleOpt.isEmpty()) return false;

        GroupRole targetRole = targetRoleOpt.get();
        GroupRole actorRole = actorRoleOpt.get();

        Optional<GroupRole> nextRoleOpt = getNextRole(group, targetRole);
        if (nextRoleOpt.isEmpty()) return false; // Already highest

        GroupRole nextRole = nextRoleOpt.get();
        if (actorRole.canManage(targetRole) && actorRole.canManage(nextRole)) {
            group.changeMemberRole(targetId, nextRole.getId());
            saveGroups();
            return true;
        }
        return false;
    }

    public boolean demotePlayer(UUID targetId, UUID actorId) {
        Group group = getGroupForPlayer(actorId);
        if (group == null || !group.isMember(targetId)) return false;

        GroupMember targetMember = group.getMember(targetId);
        GroupMember actorMember = group.getMember(actorId);
        if (targetMember == null || actorMember == null) return false;

        Optional<GroupRole> targetRoleOpt = getRole(group, targetMember.getRoleId());
        Optional<GroupRole> actorRoleOpt = getRole(group, actorMember.getRoleId());
        if (targetRoleOpt.isEmpty() || actorRoleOpt.isEmpty()) return false;

        Optional<GroupRole> prevRoleOpt = getPreviousRole(group, targetRoleOpt.get());
        if (prevRoleOpt.isEmpty()) return false; // Already lowest

        if (actorRoleOpt.get().canManage(targetRoleOpt.get())) {
            group.changeMemberRole(targetId, prevRoleOpt.get().getId());
            saveGroups();
            return true;
        }
        return false;
    }

    public boolean setHome(UUID playerId, String name, Location location) {
        Group group = getGroupForPlayer(playerId);
        if (group == null || group.getHomeCount() >= BetterGroupSystemPluginConfig.getInstance().getMaxHome())
            return false;
        group.addHome(new GroupHome(name, location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch()));
        saveGroups();
        return true;
    }

    public boolean deleteHome(UUID playerId, String name) {
        Group group = getGroupForPlayer(playerId);
        if (group == null) return false;
        boolean removed = group.removeHome(name);
        if (removed) saveGroups();
        return removed;
    }

    public Optional<GroupHome> getHome(UUID playerId, String name) {
        return Optional.ofNullable(getGroupForPlayer(playerId))
                .flatMap(g -> Optional.ofNullable(g.getHome(name)));
    }

    public Optional<GroupRole> getRole(Group group, UUID roleId) {
        return group.getRoles().stream().filter(r -> r.getId().equals(roleId)).findFirst();
    }

    public Optional<GroupRole> getRoleByName(Group group, String name) {
        return group.getRoles().stream().filter(r -> r.getName().equalsIgnoreCase(name)).findFirst();
    }

    private Optional<GroupRole> getNextRole(Group group, GroupRole currentRole) {
        return group.getRoles().stream()
                .filter(r -> r.getPriority() > currentRole.getPriority())
                .min(Comparator.comparingInt(GroupRole::getPriority));
    }

    private Optional<GroupRole> getPreviousRole(Group group, GroupRole currentRole) {
        return group.getRoles().stream()
                .filter(r -> r.getPriority() < currentRole.getPriority())
                .max(Comparator.comparingInt(GroupRole::getPriority));
    }

    public Optional<GroupRole> getLeaderRole() {
        return GroupRole.initializeRoles().stream().filter(r -> r.getName().equals("Leader")).findFirst();
    }

    public Optional<GroupRole> getRecruitRole() {
        return GroupRole.initializeRoles().stream().filter(r -> r.getName().equals("Recruit")).findFirst();
    }

    public Optional<GroupRole> getOfficerRole() {
        return GroupRole.initializeRoles().stream().filter(r -> r.getName().equals("Officer")).findFirst();
    }

    public void updateGroup(Group group) {
        groups.put(group.getId(), group);
        saveGroups();
    }

    public boolean updateGroupName(UUID playerId, String newName) {
        Group group = getGroupForPlayer(playerId);
        if (group == null || !hasPermission(playerId, Permission.CAN_UPDATE_GROUP)) {
            return false;
        }
        if (isNameTaken(newName)) {
            return false;
        }
        group.setName(newName);
        updateGroup(group);
        return true;
    }

    public boolean updateGroupTag(UUID playerId, String newTag) {
        Group group = getGroupForPlayer(playerId);
        if (group == null || !hasPermission(playerId, Permission.CAN_UPDATE_GROUP)) {
            return false;
        }
        if (isTagTaken(newTag)) {
            return false;
        }
        group.setTag(newTag);
        updateGroup(group);
        return true;
    }

    public boolean updateGroupColor(UUID playerId, String newColor) {
        Group group = getGroupForPlayer(playerId);
        if (group == null || !hasPermission(playerId, Permission.CAN_UPDATE_GROUP)) {
            return false;
        }
        group.setColor(newColor);
        updateGroup(group);
        return true;
    }

    public boolean updateGroupDescription(UUID playerId, String newDescription) {
        Group group = getGroupForPlayer(playerId);
        if (group == null || !hasPermission(playerId, Permission.CAN_UPDATE_GROUP)) {
            return false;
        }
        group.setDescription(newDescription);
        updateGroup(group);
        return true;
    }

    public boolean createRole(UUID playerId, String roleName, int priority) {
        Group group = getGroupForPlayer(playerId);
        if (group == null) return false;
        if (group.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase(roleName))) return false;
        group.getRoles().add(new GroupRole(roleName, roleName, priority, false, new HashSet<>()));
        saveGroups();
        return true;
    }

    public boolean deleteRole(UUID playerId, String roleName) {
        Group group = getGroupForPlayer(playerId);
        if (group == null) return false;
        Optional<GroupRole> roleToDelete = getRoleByName(group, roleName);
        if (roleToDelete.isEmpty() || roleToDelete.get().isDefault()) return false;
        if (group.getMembers().stream().anyMatch(m -> m.getRoleId().equals(roleToDelete.get().getId()))) return false;
        group.getRoles().remove(roleToDelete.get());
        saveGroups();
        return true;
    }

    public boolean setPlayerRole(UUID targetId, UUID actorId, String roleName) {
        Group group = getGroupForPlayer(actorId);
        if (group == null || !group.isMember(targetId)) return false;

        Optional<GroupRole> newRoleOpt = getRoleByName(group, roleName);
        if (newRoleOpt.isEmpty()) return false;

        GroupMember targetMember = group.getMember(targetId);
        GroupMember actorMember = group.getMember(actorId);
        if (targetMember == null || actorMember == null) return false;

        Optional<GroupRole> targetRoleOpt = getRole(group, targetMember.getRoleId());
        Optional<GroupRole> actorRoleOpt = getRole(group, actorMember.getRoleId());

        if (targetRoleOpt.isEmpty() || actorRoleOpt.isEmpty()) return false;

        if (actorRoleOpt.get().canManage(targetRoleOpt.get()) && actorRoleOpt.get().canManage(newRoleOpt.get())) {
            group.changeMemberRole(targetId, newRoleOpt.get().getId());
            saveGroups();
            return true;
        }
        return false;
    }

    public boolean claimChunk(UUID playerId, int chunkX, int chunkZ, String world) {
        Group group = getGroupForPlayer(playerId);
        if (group == null) return false;
        if (isChunkClaimed(chunkX, chunkZ, world)) return false;
        if (group.getClaimCount() >= BetterGroupSystemPluginConfig.getInstance().getMaxClaimsPerFaction()) return false;
        group.addClaim(new GroupClaimedChunk(chunkX, chunkZ, world));
        saveGroups();
        return true;
    }

    public boolean unclaimChunk(UUID playerId, int chunkX, int chunkZ, String world) {
        Group group = getGroupForPlayer(playerId);
        if (group == null) return false;
        if (!group.isChunkClaimed(chunkX, chunkZ, world)) return false;
        group.removeClaim(chunkX, chunkZ, world);
        saveGroups();
        return true;
    }

    public boolean isChunkClaimed(int chunkX, int chunkZ, String world) {
        return groups.values().stream().anyMatch(g -> g.isChunkClaimed(chunkX, chunkZ, world));
    }

    public boolean setDiplomacy(UUID sourceGroupId, UUID targetGroupId, DiplomacyStatus status) {
        Group sourceGroup = getGroup(sourceGroupId);
        Group targetGroup = getGroup(targetGroupId);
        if (sourceGroup == null || targetGroup == null) return false;
        sourceGroup.setDiplomacyStatus(targetGroupId, status);
        // For ally requests, the target group would need to accept, but for now we'll just set it
        if (status == DiplomacyStatus.ALLY) {
            targetGroup.setDiplomacyStatus(sourceGroupId, status);
        }
        saveGroups();
        return true;
    }

    public boolean deposit(UUID playerId, double amount) {
        Group group = getGroupForPlayer(playerId);
        if (group == null || amount <= 0) return false;
        // if (economyService.withdrawPlayer(playerId, amount)) {
        //     group.deposit(amount);
        //     saveGroups();
        //     return true;
        // }
        return false;
    }

    public boolean withdraw(UUID playerId, double amount) {
        Group group = getGroupForPlayer(playerId);
        if (group == null || amount <= 0) return false;
        if (hasPermission(playerId, Permission.CAN_MANAGE_BANK)) {
            if (group.withdraw(amount)) {
                // economyService.depositPlayer(playerId, amount);
                saveGroups();
                return true;
            }
        }
        return false;
    }

    private static class GroupData {
        private Map<UUID, Group> groups;

        public GroupData(Map<UUID, Group> groups) {
            this.groups = groups;
        }

        public Map<UUID, Group> getGroups() {
            return groups;
        }
    }
}
