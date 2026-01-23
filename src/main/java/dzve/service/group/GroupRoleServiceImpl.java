package dzve.service.group;

import dzve.model.Group;
import dzve.model.GroupRole;
import dzve.model.Permission;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of GroupRoleService following Java 25 best practices.
 * Uses pattern matching, records, and sealed interfaces for type safety and performance.
 */
public final class GroupRoleServiceImpl implements GroupRoleService {

    private static final int MAX_CUSTOM_ROLES = 10;
    private static final Set<String> PROTECTED_ROLES = Set.of("Leader", "Officer", "Member", "Recruit");
    private final GroupService groupService;

    public GroupRoleServiceImpl(GroupService groupService) {
        this.groupService = groupService;
    }

    @Override
    public RoleCreationResult createRole(RoleCreationRequest request) {
        try {
            Group group = groupService.getGroupForPlayer(request.playerId());
            if (group == null) {
                return new RoleCreationFailure("You are not in a group");
            }

            // Check permissions
            if (!groupService.hasPermission(request.playerId(), Permission.CAN_MANAGE_ROLE)) {
                return new RoleCreationFailure("You don't have permission to manage roles");
            }

            // Check if role name already exists
            if (group.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase(request.roleName()))) {
                return new RoleCreationFailure("Role with this name already exists");
            }

            // Check if it's a protected role name
            if (PROTECTED_ROLES.contains(request.roleName())) {
                return new RoleCreationFailure("Cannot create role with protected name: " + request.roleName());
            }

            // Check custom role limit
            long customRoleCount = group.getRoles().stream()
                    .filter(r -> !r.isDefault())
                    .count();
            if (customRoleCount >= MAX_CUSTOM_ROLES) {
                return new RoleCreationFailure("Maximum number of custom roles reached (" + MAX_CUSTOM_ROLES + ")");
            }

            // Validate permissions
            Set<Permission> validatedPermissions = validatePermissions(request.permissions());
            if (validatedPermissions.isEmpty() && !request.permissions().isEmpty()) {
                return new RoleCreationFailure("No valid permissions provided");
            }

            // Create the role
            GroupRole newRole = new GroupRole(
                    request.roleName(),
                    request.displayName(),
                    request.priority(),
                    false, // not default
                    validatedPermissions
            );

            group.getRoles().add(newRole);
            groupService.updateGroup(group);

            return new RoleCreationSuccess(newRole);

        } catch (Exception e) {
            return new RoleCreationFailure("Failed to create role: " + e.getMessage());
        }
    }

    @Override
    public RoleUpdateResult updateRole(RoleUpdateRequest request) {
        try {
            Group group = groupService.getGroupForPlayer(request.playerId());
            if (group == null) {
                return new RoleUpdateFailure("You are not in a group");
            }

            // Check permissions
            if (!groupService.hasPermission(request.playerId(), Permission.CAN_MANAGE_ROLE)) {
                return new RoleUpdateFailure("You don't have permission to manage roles");
            }

            // Find the role
            Optional<GroupRole> roleOpt = group.getRoles().stream()
                    .filter(r -> r.getName().equalsIgnoreCase(request.roleName()))
                    .findFirst();

            if (roleOpt.isEmpty()) {
                return new RoleUpdateFailure("Role not found");
            }

            GroupRole role = roleOpt.get();

            // Check if it's a protected role
            if (PROTECTED_ROLES.contains(role.getName())) {
                return new RoleUpdateFailure("Cannot modify protected role: " + role.getName());
            }

            // Validate permissions
            Set<Permission> validatedPermissions = validatePermissions(request.permissions());
            if (validatedPermissions.isEmpty() && !request.permissions().isEmpty()) {
                return new RoleUpdateFailure("No valid permissions provided");
            }

            // Update permissions
            role.setPermissions(validatedPermissions);
            groupService.updateGroup(group);

            return new RoleUpdateSuccess(role);

        } catch (Exception e) {
            return new RoleUpdateFailure("Failed to update role: " + e.getMessage());
        }
    }

    @Override
    public RoleDeletionResult deleteRole(RoleDeletionRequest request) {
        try {
            Group group = groupService.getGroupForPlayer(request.playerId());
            if (group == null) {
                return new RoleDeletionFailure("You are not in a group");
            }

            // Check permissions
            if (!groupService.hasPermission(request.playerId(), Permission.CAN_MANAGE_ROLE)) {
                return new RoleDeletionFailure("You don't have permission to manage roles");
            }

            // Find the role
            Optional<GroupRole> roleOpt = group.getRoles().stream()
                    .filter(r -> r.getName().equalsIgnoreCase(request.roleName()))
                    .findFirst();

            if (roleOpt.isEmpty()) {
                return new RoleDeletionFailure("Role not found");
            }

            GroupRole role = roleOpt.get();

            // Check if it's a protected role
            if (PROTECTED_ROLES.contains(role.getName())) {
                return new RoleDeletionFailure("Cannot delete protected role: " + role.getName());
            }

            // Check if any members have this role
            boolean membersWithRole = group.getMembers().stream()
                    .anyMatch(m -> m.getRoleId().equals(role.getId()));

            if (membersWithRole) {
                return new RoleDeletionFailure("Cannot delete role assigned to members. Reassign them first.");
            }

            // Delete the role
            UUID roleId = role.getId();
            group.getRoles().remove(role);
            groupService.updateGroup(group);

            return new RoleDeletionSuccess(roleId);

        } catch (Exception e) {
            return new RoleDeletionFailure("Failed to delete role: " + e.getMessage());
        }
    }

    @Override
    public Optional<List<GroupRole>> listRoles(UUID playerId) {
        Group group = groupService.getGroupForPlayer(playerId);
        if (group == null) {
            return Optional.empty();
        }

        List<GroupRole> sortedRoles = group.getRoles().stream()
                .sorted((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()))
                .collect(Collectors.toList());

        return Optional.of(sortedRoles);
    }

    @Override
    public RoleAssignmentResult assignRole(RoleAssignmentRequest request) {
        try {
            Group group = groupService.getGroupForPlayer(request.assignerId());
            if (group == null) {
                return new RoleAssignmentFailure("You are not in a group");
            }

            // Check if target is in the same group
            if (!group.isMember(request.targetId())) {
                return new RoleAssignmentFailure("Target player is not in your group");
            }

            // Check permissions
            if (!groupService.hasPermission(request.assignerId(), Permission.CAN_CHANGE_ROLE)) {
                return new RoleAssignmentFailure("You don't have permission to change roles");
            }

            // Find the role
            Optional<GroupRole> roleOpt = group.getRoles().stream()
                    .filter(r -> r.getName().equalsIgnoreCase(request.roleName()))
                    .findFirst();

            if (roleOpt.isEmpty()) {
                return new RoleAssignmentFailure("Role not found");
            }

            // Perform the assignment
            boolean success = groupService.setPlayerRole(request.targetId(), request.assignerId(), request.roleName());

            if (success) {
                return new RoleAssignmentSuccess(request.targetId(), request.roleName());
            } else {
                return new RoleAssignmentFailure("Cannot assign role of equal or higher rank");
            }

        } catch (Exception e) {
            return new RoleAssignmentFailure("Failed to assign role: " + e.getMessage());
        }
    }

    @Override
    public Optional<GroupRole> getRole(UUID playerId, String roleName) {
        Group group = groupService.getGroupForPlayer(playerId);
        if (group == null) {
            return Optional.empty();
        }

        return group.getRoles().stream()
                .filter(r -> r.getName().equalsIgnoreCase(roleName))
                .findFirst();
    }

    /**
     * Validates and converts permission strings to Permission enum values.
     */
    private Set<Permission> validatePermissions(Set<String> permissionStrings) {
        return permissionStrings.stream()
                .map(String::toUpperCase)
                .filter(name -> {
                    try {
                        Permission.valueOf(name);
                        return true;
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                })
                .map(Permission::valueOf)
                .collect(Collectors.toSet());
    }
}
