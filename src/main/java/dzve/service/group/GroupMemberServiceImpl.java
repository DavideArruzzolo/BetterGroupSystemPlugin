package dzve.service.group;

import dzve.config.BetterGroupSystemPluginConfig;
import dzve.model.Group;
import dzve.model.GroupMember;
import dzve.model.GroupRole;
import dzve.model.Permission;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of GroupMemberService following Java 25 best practices.
 * Uses pattern matching, records, and sealed interfaces for type safety and performance.
 */
public final class GroupMemberServiceImpl implements GroupMemberService {

    private final GroupService groupService;
    private final BetterGroupSystemPluginConfig config;
    private final ConcurrentHashMap<UUID, Set<UUID>> invitations;

    public GroupMemberServiceImpl(GroupService groupService, BetterGroupSystemPluginConfig config) {
        this.groupService = groupService;
        this.config = config;
        this.invitations = new ConcurrentHashMap<>();
    }

    @Override
    public InvitationResult invitePlayer(InvitationRequest request) {
        try {
            // Check if inviter is in a group
            Group group = groupService.getGroup(request.groupId());
            if (group == null) {
                return new InvitationFailure("Group not found");
            }

            // Check if inviter has permission
            if (!groupService.hasPermission(request.inviterId(), Permission.CAN_INVITE)) {
                return new InvitationFailure("You don't have permission to invite members");
            }

            // Check if target is already in a group
            if (groupService.getGroupForPlayer(request.targetId()) != null) {
                return new InvitationFailure("Target player is already in a group");
            }

            // Check if group is full
            if (group.getMemberCount() >= config.getMaxSize()) {
                return new InvitationFailure("Group is full");
            }

            // Add invitation
            invitations.computeIfAbsent(request.targetId(), k -> ConcurrentHashMap.newKeySet())
                    .add(request.groupId());

            return new InvitationSuccess(request.targetId(), request.groupId());

        } catch (Exception e) {
            return new InvitationFailure("Failed to invite player: " + e.getMessage());
        }
    }

    @Override
    public Set<UUID> getPendingInvitations(UUID playerId) {
        return invitations.getOrDefault(playerId, Set.of());
    }

    @Override
    public AcceptanceResult acceptInvitation(AcceptanceRequest request) {
        try {
            // Check if player is already in a group
            if (groupService.getGroupForPlayer(request.playerId()) != null) {
                return new AcceptanceFailure("You are already in a group");
            }

            // Find the group
            Group group = groupService.findGroupByNameOrTag(request.groupIdentifier());
            if (group == null) {
                return new AcceptanceFailure("Group not found");
            }

            UUID groupId = group.getId();

            // Check if player has an invitation
            Set<UUID> playerInvitations = invitations.get(request.playerId());
            if (playerInvitations == null || !playerInvitations.contains(groupId)) {
                return new AcceptanceFailure("You don't have an invitation from this group");
            }

            // Check if group is still not full
            if (group.getMemberCount() >= config.getMaxSize()) {
                // Remove the invitation since group is full
                playerInvitations.remove(groupId);
                if (playerInvitations.isEmpty()) {
                    invitations.remove(request.playerId());
                }
                return new AcceptanceFailure("Group is now full");
            }

            // Accept the invitation
            boolean success = groupService.acceptInvitation(request.playerId(), request.groupIdentifier());

            if (success) {
                // Clear all invitations for the player
                invitations.remove(request.playerId());
                return new AcceptanceSuccess(request.playerId(), group);
            } else {
                return new AcceptanceFailure("Failed to accept invitation");
            }

        } catch (Exception e) {
            return new AcceptanceFailure("Failed to accept invitation: " + e.getMessage());
        }
    }

    @Override
    public KickResult kickMember(KickRequest request) {
        try {
            Group group = groupService.getGroupForPlayer(request.kickerId());
            if (group == null) {
                return new KickFailure("You are not in a group");
            }

            // Check if target is in the same group
            if (!group.isMember(request.targetId())) {
                return new KickFailure("Target player is not in your group");
            }

            // Prevent self-kick
            if (request.kickerId().equals(request.targetId())) {
                return new KickFailure("You cannot kick yourself");
            }

            // Check permissions and hierarchy
            if (!groupService.hasPermission(request.kickerId(), Permission.CAN_KICK)) {
                return new KickFailure("You don't have permission to kick members");
            }

            // Perform the kick
            boolean success = groupService.kickMember(request.targetId(), request.kickerId());

            if (success) {
                return new KickSuccess(request.targetId(), group.getId());
            } else {
                return new KickFailure("Cannot kick member of equal or higher rank");
            }

        } catch (Exception e) {
            return new KickFailure("Failed to kick member: " + e.getMessage());
        }
    }

    @Override
    public TransferResult transferLeadership(TransferRequest request) {
        try {
            Group group = groupService.getGroupForPlayer(request.currentLeaderId());
            if (group == null) {
                return new TransferFailure("You are not in a group");
            }

            // Check if current player is leader
            if (!group.isLeader(request.currentLeaderId())) {
                return new TransferFailure("Only the group leader can transfer leadership");
            }

            // Check if target is in the group
            if (!group.isMember(request.newLeaderId())) {
                return new TransferFailure("Target player is not in your group");
            }

            // Perform the transfer
            boolean success = groupService.transferLeadership(request.newLeaderId(), request.currentLeaderId());

            if (success) {
                return new TransferSuccess(request.currentLeaderId(), request.newLeaderId(), group.getId());
            } else {
                return new TransferFailure("Failed to transfer leadership");
            }

        } catch (Exception e) {
            return new TransferFailure("Failed to transfer leadership: " + e.getMessage());
        }
    }

    @Override
    public PromotionResult promoteMember(PromotionRequest request) {
        try {
            Group group = groupService.getGroupForPlayer(request.promoterId());
            if (group == null) {
                return new PromotionFailure("You are not in a group");
            }

            // Check if target is in the same group
            if (!group.isMember(request.targetId())) {
                return new PromotionFailure("Target player is not in your group");
            }

            // Check permissions
            if (!groupService.hasPermission(request.promoterId(), Permission.CAN_CHANGE_ROLE)) {
                return new PromotionFailure("You don't have permission to promote members");
            }

            // Perform the promotion
            boolean success;
            if (request.targetRole() != null && !request.targetRole().isBlank()) {
                success = groupService.setPlayerRole(request.targetId(), request.promoterId(), request.targetRole());
            } else {
                success = groupService.promotePlayer(request.targetId(), request.promoterId());
            }

            if (success) {
                // Get the new role name
                GroupMember targetMember = group.getMember(request.targetId());
                if (targetMember != null) {
                    GroupRole newRole = groupService.getRole(group, targetMember.getRoleId()).orElse(null);
                    String roleName = newRole != null ? newRole.getName() : "Unknown";
                    return new PromotionSuccess(request.targetId(), roleName);
                }
            }

            return new PromotionFailure("Failed to promote member");

        } catch (Exception e) {
            return new PromotionFailure("Failed to promote member: " + e.getMessage());
        }
    }

    @Override
    public DemotionResult demoteMember(DemotionRequest request) {
        try {
            Group group = groupService.getGroupForPlayer(request.demoterId());
            if (group == null) {
                return new DemotionFailure("You are not in a group");
            }

            // Check if target is in the same group
            if (!group.isMember(request.targetId())) {
                return new DemotionFailure("Target player is not in your group");
            }

            // Check permissions
            if (!groupService.hasPermission(request.demoterId(), Permission.CAN_CHANGE_ROLE)) {
                return new DemotionFailure("You don't have permission to demote members");
            }

            // Perform the demotion
            boolean success;
            if (request.targetRole() != null && !request.targetRole().isBlank()) {
                success = groupService.setPlayerRole(request.targetId(), request.demoterId(), request.targetRole());
            } else {
                success = groupService.demotePlayer(request.targetId(), request.demoterId());
            }

            if (success) {
                // Get the new role name
                GroupMember targetMember = group.getMember(request.targetId());
                if (targetMember != null) {
                    GroupRole newRole = groupService.getRole(group, targetMember.getRoleId()).orElse(null);
                    String roleName = newRole != null ? newRole.getName() : "Unknown";
                    return new DemotionSuccess(request.targetId(), roleName);
                }
            }

            return new DemotionFailure("Failed to demote member");

        } catch (Exception e) {
            return new DemotionFailure("Failed to demote member: " + e.getMessage());
        }
    }
}
