package dzve.service.admin;

import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import dzve.model.DiplomacyStatus;
import dzve.model.Group;
import dzve.model.GroupMember;
import dzve.model.GroupRole;
import dzve.service.NotificationService;
import dzve.service.group.GroupService;
import dzve.utils.LogService;

import java.util.UUID;

import static com.hypixel.hytale.protocol.packets.interface_.NotificationStyle.Success;
import static com.hypixel.hytale.protocol.packets.interface_.NotificationStyle.Warning;

public class AdminService {

    public static final String ADMIN_PERMISSION = "bettergroupsystem.admin";

    private final GroupService groupService;
    private final NotificationService notificationService;

    public AdminService(GroupService groupService, NotificationService notificationService) {
        this.groupService = groupService;
        this.notificationService = notificationService;
    }

    public boolean hasAdminPermission(PlayerRef player) {
        return PermissionsModule.get().hasPermission(player.getUuid(), ADMIN_PERMISSION);
    }

    public void adminDisband(PlayerRef admin, String groupName) {
        if (!checkAdmin(admin))
            return;

        Group group = groupService.getGroupByName(groupName);
        if (group == null) {
            groupService.notify(admin, "Group '" + groupName + "' not found.");
            return;
        }

        notificationService.broadcastGroup(
                group.getMembers().stream()
                        .map(GroupMember::getPlayerId)
                        .toList(),
                "Your group has been disbanded by an administrator.",
                Warning);

        group.getMembers().forEach(member -> groupService.removePlayerFromGroupMap(member.getPlayerId()));

        groupService.removeGroup(group);

        groupService.notify(admin, "Group '" + groupName + "' has been disbanded.", false);
        LogService.info("ADMIN", "Admin disbanded group", "admin", admin.getUsername(), "group", groupName);
    }

    public void adminKick(PlayerRef admin, String groupName, String targetName) {
        if (!checkAdmin(admin))
            return;

        Group group = groupService.getGroupByName(groupName);
        if (group == null) {
            groupService.notify(admin, "Group '" + groupName + "' not found.");
            return;
        }

        GroupMember targetMember = group.getMembers().stream()
                .filter(m -> m.getPlayerName().equalsIgnoreCase(targetName))
                .findFirst()
                .orElse(null);

        if (targetMember == null) {
            groupService.notify(admin, "Player '" + targetName + "' not found in group.");
            return;
        }

        if (group.isLeader(targetMember.getPlayerId())) {
            groupService.notify(admin, "Cannot kick the leader. Use 'admin setleader' first.");
            return;
        }

        group.removeMember(targetMember.getPlayerId());
        groupService.removePlayerFromGroupMap(targetMember.getPlayerId());

        groupService.persistRemoveMember(group.getId(), targetMember.getPlayerId());

        Universe.get().getPlayers().stream()
                .filter(p -> p.getUuid().equals(targetMember.getPlayerId()))
                .findFirst()
                .ifPresent(p -> groupService.notify(p,
                        "You have been kicked from " + group.getName() + " by an administrator."));

        groupService.notify(admin, "Kicked '" + targetName + "' from '" + groupName + "'.", false);
        LogService.info("ADMIN", "Admin kicked member", "admin", admin.getUsername(), "target", targetName, "group",
                groupName);
    }

    public void adminSetLeader(PlayerRef admin, String groupName, String targetName) {
        if (!checkAdmin(admin))
            return;

        Group group = groupService.getGroupByName(groupName);
        if (group == null) {
            groupService.notify(admin, "Group '" + groupName + "' not found.");
            return;
        }

        GroupMember targetMember = group.getMembers().stream()
                .filter(m -> m.getPlayerName().equalsIgnoreCase(targetName))
                .findFirst()
                .orElse(null);

        if (targetMember == null) {
            groupService.notify(admin, "Player '" + targetName + "' not found in group.");
            return;
        }

        UUID oldLeaderId = group.getLeaderId();
        UUID newLeaderId = targetMember.getPlayerId();

        GroupRole leaderRole = group.getRoles().stream()
                .max(java.util.Comparator.comparingInt(GroupRole::getPriority))
                .orElseThrow();

        group.changeMemberRole(newLeaderId, leaderRole.getId());
        group.setLeaderId(newLeaderId);

        GroupRole defaultRole = group.getRoles().stream()
                .filter(GroupRole::isDefault)
                .findFirst()
                .orElseThrow();

        GroupMember oldLeader = group.getMember(oldLeaderId);
        if (oldLeader != null) {
            group.changeMemberRole(oldLeaderId, defaultRole.getId());
        }

        groupService.persistUpdateGroup(group);
        if (oldLeader != null) {
            groupService.persistUpdateMember(group.getId(), oldLeader);
        }
        groupService.persistUpdateMember(group.getId(), group.getMember(newLeaderId));

        notificationService.broadcastGroup(
                group.getMembers().stream()
                        .map(GroupMember::getPlayerId)
                        .toList(),
                targetName + " is now the leader (changed by admin).",
                Success);

        groupService.notify(admin, "Set '" + targetName + "' as leader of '" + groupName + "'.", false);
        LogService.info("ADMIN", "Admin set leader", "admin", admin.getUsername(), "group", groupName, "new_leader",
                targetName);
    }

    public void adminSetDiplomacy(PlayerRef admin, String group1Name, String group2Name, DiplomacyStatus status) {
        if (!checkAdmin(admin))
            return;

        Group group1 = groupService.getGroupByName(group1Name);
        Group group2 = groupService.getGroupByName(group2Name);

        if (group1 == null) {
            groupService.notify(admin, "Group '" + group1Name + "' not found.");
            return;
        }
        if (group2 == null) {
            groupService.notify(admin, "Group '" + group2Name + "' not found.");
            return;
        }
        if (group1.getId().equals(group2.getId())) {
            groupService.notify(admin, "Cannot set diplomacy between the same group.");
            return;
        }

        group1.setDiplomacyStatus(group2.getId(), status);
        if (status == DiplomacyStatus.ALLY) {
            group2.setDiplomacyStatus(group1.getId(), status);
        }

        groupService.persistSetDiplomacy(group1.getId(), group2.getId(), status);
        if (status == DiplomacyStatus.ALLY) {
            groupService.persistSetDiplomacy(group2.getId(), group1.getId(), status);
        }

        String message = "Diplomacy with "
                + (status == DiplomacyStatus.ALLY ? "alliance" : status.toString().toLowerCase())
                + " status set by administrator.";

        notificationService.broadcastGroup(
                group1.getMembers().stream().map(GroupMember::getPlayerId).toList(),
                "Relation with " + group2.getName() + ": " + status,
                Success);

        if (status == DiplomacyStatus.ALLY) {
            notificationService.broadcastGroup(
                    group2.getMembers().stream().map(GroupMember::getPlayerId).toList(),
                    "Relation with " + group1.getName() + ": " + status,
                    Success);
        }

        groupService.notify(admin, "Set diplomacy " + group1Name + " <-> " + group2Name + " to " + status, false);
        LogService.info("ADMIN", "Admin set diplomacy", "admin", admin.getUsername(), "group1", group1Name, "group2",
                group2Name, "status", status);
    }

    public void adminInfo(PlayerRef admin, String groupName) {
        if (!checkAdmin(admin))
            return;

        groupService.getGroupInfo(admin, groupName);
    }

    public void adminSetMoney(PlayerRef admin, String groupName, double amount) {
        if (!checkAdmin(admin))
            return;

        Group group = groupService.getGroupByName(groupName);
        if (group == null) {
            groupService.notify(admin, "Group '" + groupName + "' not found.");
            return;
        }

        if (amount < 0) {
            groupService.notify(admin, "Amount cannot be negative.");
            return;
        }

        double oldBalance = group.getBankBalance();
        group.setBankBalance(amount);

        groupService.persistUpdateGroup(group);

        groupService.notify(admin, "Set " + groupName + " bank balance from " +
                String.format("%.2f", oldBalance) + " to " + String.format("%.2f", amount), false);

        notificationService.broadcastGroup(
                group.getMembers().stream().map(GroupMember::getPlayerId).toList(),
                "Group bank balance set to " + String.format("%.2f", amount) + " by administrator.",
                Success);

        LogService.info("ADMIN", "Admin set group money", "admin", admin.getUsername(), "group", groupName,
                "old_balance", oldBalance, "new_balance", amount);
    }

    public void adminSetPlayerMoney(PlayerRef admin, String playerName, double amount) {
        if (!checkAdmin(admin))
            return;

        if (amount < 0) {
            groupService.notify(admin, "Amount cannot be negative.");
            return;
        }

        UUID targetPlayerId = groupService.findPlayerUuidByName(playerName);
        if (targetPlayerId == null) {
            groupService.notify(admin, "Player '" + playerName + "' not found.");
            return;
        }

        Group group = groupService.getGroupByPlayerId(targetPlayerId);
        if (group == null) {
            groupService.notify(admin, "Player '" + playerName + "' is not in any group.");
            return;
        }

        GroupMember member = group.getMember(targetPlayerId);
        if (member == null) {
            groupService.notify(admin, "Player member data not found.");
            return;
        }

        double oldBalance = member.getBankBalance();
        member.setBankBalance(amount);

        groupService.persistUpdateMember(group.getId(), member);

        groupService.notify(admin, "Set " + playerName + "'s balance from " +
                String.format("%.2f", oldBalance) + " to " + String.format("%.2f", amount), false);

        LogService.info("ADMIN", "Admin set player money", "admin", admin.getUsername(), "player", playerName,
                "old_balance", oldBalance, "new_balance", amount);
    }

    public void adminGrantPermission(PlayerRef admin, String targetName) {

        if (!hasAdminPermission(admin)) {
            groupService.notify(admin, "You don't have admin permission.");
            return;
        }

        UUID targetPlayerId = groupService.findPlayerUuidByName(targetName);
        if (targetPlayerId == null) {
            groupService.notify(admin, "Player '" + targetName + "' not found.");
            return;
        }

        try {
            PermissionsModule.get().addUserPermission(targetPlayerId, java.util.Set.of(ADMIN_PERMISSION));
            groupService.notify(admin, "Granted admin permission to " + targetName, false);
            LogService.info("ADMIN", "Granted admin permission", "admin", admin.getUsername(), "target", targetName);
        } catch (Exception e) {
            groupService.notify(admin, "Failed to grant permission: " + e.getMessage());
        }
    }

    public void adminRevokePermission(PlayerRef admin, String targetName) {
        if (!checkAdmin(admin))
            return;

        UUID targetPlayerId = groupService.findPlayerUuidByName(targetName);
        if (targetPlayerId == null) {
            groupService.notify(admin, "Player '" + targetName + "' not found.");
            return;
        }

        try {
            PermissionsModule.get().removeUserPermission(targetPlayerId, java.util.Set.of(ADMIN_PERMISSION));
            groupService.notify(admin, "Revoked admin permission from " + targetName, false);
            LogService.info("ADMIN", "Revoked admin permission", "admin", admin.getUsername(), "target", targetName);
        } catch (Exception e) {
            groupService.notify(admin, "Failed to revoke permission: " + e.getMessage());
        }
    }

    private boolean checkAdmin(PlayerRef player) {
        if (!hasAdminPermission(player)) {
            groupService.notify(player, "You don't have admin permission.");
            return false;
        }
        return true;
    }
}
