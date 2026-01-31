package dzve.service.membership;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import dzve.model.*;
import dzve.service.NotificationService;
import dzve.service.group.GroupService;
import dzve.utils.ChatFormatter;
import dzve.utils.LogService;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.hypixel.hytale.protocol.packets.interface_.NotificationStyle.Success;
import static com.hypixel.hytale.protocol.packets.interface_.NotificationStyle.Warning;

public class MembershipService {

    private final GroupService groupService;
    private final NotificationService notificationService;
    private final Map<UUID, Set<UUID>> invitations = new ConcurrentHashMap<>();

    public MembershipService(GroupService groupService) {
        this.groupService = groupService;
        this.notificationService = NotificationService.getInstance();
    }

    public void invitePlayer(PlayerRef sender, PlayerRef target) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null || !groupService.hasPerm(group, sender, Permission.CAN_INVITE))
            return;

        LogService.debug("MEMBERSHIP", "Inviting player", "sender", sender.getUsername(), "target",
                target.getUsername(), "group", group.getName());

        if (groupService.getPlayerGroup(target.getUuid()) != null) {
            groupService.notify(sender, "Player already in a group.");
            return;
        }
        if (group.getMembers().size() >= (group.getType().equals(GroupType.FACTION)
                ? GroupService.getConfig().getMaxSize()
                : GroupService.getConfig().getMaxSize()
                + GroupService.getConfig().getSlotQuantityGainForLevel() * ((Guild) group).getLevel())) {
            groupService.notify(sender, "Group full.");
            return;
        }

        invitations.computeIfAbsent(target.getUuid(), k -> ConcurrentHashMap.newKeySet()).add(group.getId());
        groupService.notify(sender, "Invited " + target.getUsername(), false);
        groupService.notify(target, "You have been invited to join " + group.getName() + "! Use '/group accept "
                + group.getName() + "' to join.", false);
    }

    public void acceptInvitation(PlayerRef player, String groupName) {
        if (groupService.getPlayerGroup(player.getUuid()) != null) {
            groupService.notify(player, "Already in a group.");
            return;
        }

        LogService.debug("MEMBERSHIP", "Accepting invitation", "player", player.getUsername(), "group", groupName);

        Group group = groupService.getGroupByName(groupName);
        if (group == null) {
            groupService.notify(player, "Group not found.");
            return;
        }

        Set<UUID> invites = invitations.get(player.getUuid());
        if (invites == null || !invites.contains(group.getId())) {
            groupService.notify(player, "No invitation from this group.");
            return;
        }
        if (group.getMembers().size() >= GroupService.getConfig().getMaxSize()) {
            groupService.notify(player, "Group is full.");
            return;
        }

        GroupRole defaultRole = group.getRoles().stream().filter(GroupRole::isDefault).findFirst().orElseThrow();
        group.addMember(player, defaultRole.getId());
        groupService.updatePlayerGroupMap(player.getUuid(), group.getId());
        invites.remove(group.getId());

        // Persist
        GroupMember newMember = group.getMember(player.getUuid());
        groupService.persistAddMember(group.getId(), newMember);

        groupService.notify(player, "Joined " + group.getName(), false);

        groupService.updateGroupMaps(group);

        notificationService.broadcastGroup(
                group.getMembers().stream()
                        .map(GroupMember::getPlayerId)
                        .filter(id -> !id.equals(player.getUuid()))
                        .toList(),
                player.getUsername() + " joined the group!",
                Success);
    }

    public void kickMember(PlayerRef sender, UUID targetId) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null || !groupService.hasPerm(group, sender, Permission.CAN_KICK))
            return;
        if (!group.isMember(targetId)) {
            groupService.notify(sender, "Target not in group.");
            return;
        }
        if (sender.getUuid().equals(targetId)) {
            groupService.notify(sender, "Cannot kick self.");
            return;
        }

        if (group.isLeader(targetId)) {
            groupService.notify(sender, "Cannot kick the group leader.", true);
            return;
        }

        if (!canModify(group, sender.getUuid(), targetId)) {
            groupService.notify(sender, "Target rank too high.");
            return;
        }

        group.removeMember(targetId);
        LogService.info("MEMBERSHIP", "Kicked member", "kicker", sender.getUsername(), "targetId", targetId.toString(),
                "group", group.getName());
        groupService.removePlayerFromGroupMap(targetId);
        groupService.clearPlayerMapFilter(targetId);

        groupService.persistRemoveMember(group.getId(), targetId);

        groupService.notify(sender, "Member kicked.", false);

        PlayerRef targetPlayer = Universe.get().getPlayer(targetId);
        if (targetPlayer != null) {
            groupService.notify(targetPlayer, "You have been kicked from " + group.getName() + ".", true);
        }

        notificationService.broadcastGroup(
                group.getMembers().stream()
                        .map(GroupMember::getPlayerId)
                        .filter(id -> !id.equals(sender.getUuid()) && !id.equals(targetId))
                        .toList(),
                targetPlayer != null ? targetPlayer.getUsername() : "A member" + " has been kicked from the group!",
                Warning);

        groupService.updateGroupMaps(group);
    }

    public void leaveGroup(PlayerRef player) {
        Group group = groupService.getGroupOrNotify(player);
        if (group == null)
            return;
        if (group.isLeader(player.getUuid()) && group.getMembers().size() > 1) {
            groupService.notify(player, "Leader cannot leave. Transfer ownership first.");
            return;
        }

        if (group.getMembers().size() <= 1) {
            groupService.disband(player);
        } else {
            group.removeMember(player.getUuid());
            groupService.removePlayerFromGroupMap(player.getUuid());
            groupService.clearPlayerMapFilter(player.getUuid());

            groupService.persistRemoveMember(group.getId(), player.getUuid());

            groupService.notify(player, "You left the group.", false);

            notificationService.broadcastGroup(
                    group.getMembers().stream()
                            .map(GroupMember::getPlayerId)
                            .toList(),
                    player.getUsername() + " has left the group!",
                    Warning);

            groupService.updateGroupMaps(group);
        }
    }

    public void transferLeadership(PlayerRef sender, UUID targetId) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null || !group.isLeader(sender.getUuid()))
            return;
        if (sender.getUuid().equals(targetId)) {
            groupService.notify(sender, "You cannot transfer leadership to yourself.");
            return;
        }
        if (!group.isMember(targetId)) {
            groupService.notify(sender, "Target not in group.");
            return;
        }

        GroupRole leaderRole = getRoleByPriority(group, Integer.MAX_VALUE);
        GroupRole memberRole = getRoleByPriority(group, 50);

        group.setLeaderId(targetId);
        group.changeMemberRole(targetId, leaderRole.getId());
        group.changeMemberRole(sender.getUuid(), memberRole.getId());

        groupService.persistUpdateGroup(group);
        groupService.persistUpdateMember(group.getId(), group.getMember(targetId));
        groupService.persistUpdateMember(group.getId(), group.getMember(sender.getUuid()));

        groupService.notify(sender, "Leadership transferred.", false);

        PlayerRef targetPlayer = Universe.get().getPlayer(targetId);
        if (targetPlayer != null) {
            groupService.notify(targetPlayer, "You are now the leader of " + group.getName() + "!", false);
        }
    }

    public void createRole(PlayerRef sender, String name, List<String> grants) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null || !groupService.hasPerm(group, sender, Permission.CAN_MANAGE_ROLE))
            return;
        if (group.getRoles().size() >= 10) {
            groupService.notify(sender, "Max roles reached.");
            return;
        }
        GroupRole existingRole = getRoleByName(group, name);
        if (existingRole != null && !existingRole.isDefault()) {
            groupService.notify(sender, "Role exists.");
            return;
        }

        Set<Permission> perms = parsePerms(grants);
        if (perms == null) {
            groupService.notify(sender, "Invalid permissions.");
            return;
        }

        GroupRole newRole = new GroupRole(name, name, 10, false, perms);
        modifyRoles(group, roles -> roles.add(newRole));
        groupService.persistCreateRole(group.getId(), newRole);

        groupService.notify(sender, "Role created.", false);
    }

    public void deleteRole(PlayerRef sender, String name) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null || !groupService.hasPerm(group, sender, Permission.CAN_MANAGE_ROLE))
            return;

        GroupRole role = getRoleByName(group, name);
        if (role == null) {
            groupService.notify(sender, "Role not found.");
            return;
        }
        if (role.isDefault()) {
            groupService.notify(sender, "Cannot delete default role.");
            return;
        }
        if (group.getMembers().stream().anyMatch(m -> m.getRoleId().equals(role.getId()))) {
            groupService.notify(sender, "Role is in use.");
            return;
        }

        modifyRoles(group, roles -> roles.remove(role));
        groupService.persistDeleteRole(role.getId());

        groupService.notify(sender, "Role deleted.", false);
    }

    public void updateRole(PlayerRef sender, String name, List<String> addGrants, List<String> removeGrants) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null || !groupService.hasPerm(group, sender, Permission.CAN_MANAGE_ROLE))
            return;

        GroupRole role = getRoleByName(group, name);
        if (role == null) {
            groupService.notify(sender, "Role not found.");
            return;
        }
        if (role.isDefault()) {
            groupService.notify(sender, "Cannot edit default role.");
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

        groupService.persistUpdateRole(group.getId(), role);

        groupService.notify(sender, "Role permissions updated.", false);
    }

    public void setRole(PlayerRef sender, UUID targetId, String roleName) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null || !groupService.hasPerm(group, sender, Permission.CAN_CHANGE_ROLE))
            return;
        if (!group.isMember(targetId)) {
            groupService.notify(sender, "Target not in group.");
            return;
        }

        if (group.isLeader(targetId)) {
            groupService.notify(sender, "Cannot change the leader's role.", true);
            return;
        }

        if (!canModify(group, sender.getUuid(), targetId)) {
            groupService.notify(sender, "Hierarchy prevents this.");
            return;
        }

        GroupRole role = getRoleByName(group, roleName);
        if (role == null) {
            groupService.notify(sender, "Role not found.");
            return;
        }

        GroupRole senderRole = getMemberRole(group, sender.getUuid());
        if (!group.isLeader(sender.getUuid())
                && role.getPriority() >= (senderRole != null ? senderRole.getPriority() : 0)) {
            groupService.notify(sender, "Cannot promote to rank >= yours.");
            return;
        }

        group.changeMemberRole(targetId, role.getId());

        groupService.persistUpdateMember(group.getId(), group.getMember(targetId));

        groupService.notify(sender, "Role updated successfully.", false);
    }

    private boolean canModify(Group g, UUID actor, UUID target) {
        if (g.isLeader(actor))
            return true;
        if (g.isLeader(target))
            return false;
        GroupRole actorRole = getMemberRole(g, actor);
        GroupRole targetRole = getMemberRole(g, target);
        return actorRole != null && targetRole != null && actorRole.getPriority() > targetRole.getPriority();
    }

    private GroupRole getMemberRole(Group g, UUID pid) {
        GroupMember member = g.getMember(pid);
        if (member == null)
            return null;
        return g.getRoles().stream().filter(r -> r.getId().equals(member.getRoleId())).findFirst().orElse(null);
    }

    private GroupRole getRoleByName(Group g, String n) {
        return g.getRoles().stream().filter(r -> r.getName().equalsIgnoreCase(n)).findFirst().orElse(null);
    }

    private GroupRole getRoleByPriority(Group g, int p) {
        return g.getRoles().stream().filter(r -> r.getPriority() <= p)
                .max(Comparator.comparingInt(GroupRole::getPriority)).orElseThrow();
    }

    private void modifyRoles(Group g, Consumer<Set<GroupRole>> modifier) {
        Set<GroupRole> mutable = new HashSet<>(g.getRoles());
        modifier.accept(mutable);
        g.setRoles(mutable);
        // groupService.saveGroups(); // Handled by caller granularly
    }

    private Set<Permission> parsePerms(List<String> list) {
        if (list == null)
            return null;
        try {
            return list.stream()
                    .map(s -> s.replace("\"", "").trim().replace(",", ""))
                    .map(s -> Permission.valueOf(s.toUpperCase().replace(".", "_")))
                    .collect(Collectors.toSet());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Set<UUID> getInvites(UUID playerId) {
        return invitations.get(playerId);
    }

    public void listInvitations(PlayerRef sender) {
        Set<UUID> invites = invitations.get(sender.getUuid());
        if (invites == null || invites.isEmpty()) {
            groupService.notify(sender, "No pending invitations.", false);
            return;
        }

        final ChatFormatter.StyledText[] msg = {ChatFormatter.of("=== Pending Group Invitations ===\n").withBold()};

        invites.stream()
                .map(groupService::getGroup)
                .filter(Objects::nonNull)
                .forEach(group -> {
                    Color groupColor = group.getColor() != null ? Color.decode(group.getColor()) : Color.white;
                    String leaderName = Optional.ofNullable(group.getMember(group.getLeaderId()))
                            .map(GroupMember::getPlayerName)
                            .orElse("N/A");

                    msg[0] = msg[0].append("Group: ").withBold().append(group.getName()).append("\n")
                            .append("\tTag: ").append(group.getTag()).withColor(groupColor).append("\n")
                            .append("\tMembers: ").append(group.getMembers().size() + " / " +
                                    (group.getType().equals(GroupType.FACTION) ? GroupService.getConfig().getMaxSize()
                                            : GroupService.getConfig().getMaxSize()
                                            + GroupService.getConfig().getSlotQuantityGainForLevel()
                                            * ((Guild) group).getLevel()))
                            .append("\n");
                    if (group instanceof Faction faction) {
                        msg[0] = msg[0].append("\tTotal Power: ").append(String.format("%.2f", faction.getTotalPower()))
                                .append("\n");
                    }
                    if (group instanceof Guild guild) {
                        msg[0] = msg[0].append("\tLevel: ")
                                .append(String.valueOf(guild.getLevel())).append("\n\n");
                    }
                });

        sender.sendMessage(msg[0].toMessage());
    }

    public void listRoles(PlayerRef sender) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null)
            return;

        final ChatFormatter.StyledText[] msg = {ChatFormatter.of("=== Roles for " + group.getName() + " ===\n")
                .withColor(Color.YELLOW).withBold()};

        if (group.getRoles().isEmpty()) {
            msg[0] = msg[0].append("No roles found.").withColor(Color.GRAY);
        } else {
            Set<String> seenRoles = new HashSet<>();
            group.getRoles().stream()
                    .sorted(Comparator.comparingInt(GroupRole::getPriority).reversed())
                    .filter(role -> seenRoles.add(role.getName().toLowerCase()))
                    .forEach(role -> {
                        String perms = role.getPermissions().stream()
                                .map(Permission::name)
                                .collect(Collectors.joining(", "));

                        Color roleColor = role.getPriority() >= 100 ? new Color(255, 170, 0)
                                : role.getPriority() >= 50 ? Color.BLUE : Color.GRAY;

                        msg[0] = msg[0]
                                .append("Role: ").withBold().append(role.getName()).append("\n")
                                .append("\tPriority: ").append(role.getPriority() + "\n")
                                .append("\tPermissions: ").append(perms.isEmpty() ? "None" : perms).append("\n");
                    });
        }
        sender.sendMessage(msg[0].toMessage());
    }

    public void listMembers(PlayerRef sender) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null)
            return;

        final ChatFormatter.StyledText[] msg = {
                ChatFormatter.of("=== Members of " + group.getName() + " ===\n").withBold()};

        if (group.getMembers().isEmpty()) {
            msg[0] = msg[0].append("No members found.").withColor(Color.GRAY);
        } else {
            group.getMembers().stream()
                    .sorted((a, b) -> {
                        GroupRole roleA = group.getRole(a.getRoleId());
                        GroupRole roleB = group.getRole(b.getRoleId());
                        int priorityA = roleA != null ? roleA.getPriority() : 0;
                        int priorityB = roleB != null ? roleB.getPriority() : 0;
                        return Integer.compare(priorityB, priorityA);
                    })
                    .forEach(member -> {
                        GroupRole role = group.getRole(member.getRoleId());
                        String roleName = role != null ? role.getName() : "Unknown";
                        boolean isLeader = member.getPlayerId().equals(group.getLeaderId());

                        msg[0] = msg[0].append("- ").withColor(new Color(113, 113, 113)).append(member.getPlayerName())
                                .withBold()
                                .append("\n\t")
                                .append("Role: ").withColor(Color.WHITE).append(roleName).withBold()
                                .append("\n");
                    });
        }
        sender.sendMessage(msg[0].toMessage());
    }
}