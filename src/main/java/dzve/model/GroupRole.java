package dzve.model;

import lombok.*;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GroupRole {
    private UUID id;
    private String name;
    private String displayName;
    private int priority;
    private Set<Permission> permissions;
    private boolean isDefault;

    public GroupRole(String name, String displayName, int priority, boolean isDefault, Set<Permission> permissions) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.displayName = displayName;
        this.priority = priority;
        this.isDefault = isDefault;
        this.permissions = permissions;
    }

    public static Set<GroupRole> initializeRoles() {
        return Set.of(
                new GroupRole("Recruit", "Recruit", 0, true, Set.of(
                        Permission.CAN_TELEPORT_HOME,
                        Permission.CAN_CHAT_INTERNAL
                )),
                new GroupRole("Member", "Member", 50, false, Set.of(
                        Permission.CAN_TELEPORT_HOME,
                        Permission.CAN_CHAT_INTERNAL,
                        Permission.CAN_CHAT_ALLY,
                        Permission.CAN_INTERACT_IN_CLAIM
                )),
                new GroupRole("Officer", "Officer", 100, false, Set.of(
                        Permission.CAN_TELEPORT_HOME,
                        Permission.CAN_CHAT_INTERNAL,
                        Permission.CAN_CHAT_ALLY,
                        Permission.CAN_INTERACT_IN_CLAIM,
                        Permission.CAN_INVITE,
                        Permission.CAN_KICK,
                        Permission.CAN_CHANGE_ROLE,
                        Permission.CAN_MANAGE_CLAIM,
                        Permission.CAN_MANAGE_DIPLOMACY,
                        Permission.CAN_MANAGE_HOME
                )),
                new GroupRole("Leader", "Leader", Integer.MAX_VALUE, false, Set.of(
                        Permission.CAN_TELEPORT_HOME,
                        Permission.CAN_CHAT_INTERNAL,
                        Permission.CAN_CHAT_ALLY,
                        Permission.CAN_INTERACT_IN_CLAIM,
                        Permission.CAN_INVITE,
                        Permission.CAN_KICK,
                        Permission.CAN_CHANGE_ROLE,
                        Permission.CAN_MANAGE_CLAIM,
                        Permission.CAN_MANAGE_DIPLOMACY,
                        Permission.CAN_MANAGE_HOME,
                        Permission.CAN_MANAGE_ROLE,
                        Permission.CAN_MANAGE_BANK,
                        Permission.CAN_UPDATE_GROUP,
                        Permission.CAN_UPGRADE_GUILD
                ))
        );
    }

    public boolean hasPermission(Permission permission) {
        return permissions != null && permissions.contains(permission);
    }

    public void addPermission(Permission permission) {
        if (permissions != null) {
            permissions.add(permission);
        }
    }

    public void removePermission(Permission permission) {
        if (permissions != null) {
            permissions.remove(permission);
        }
    }

    public boolean canManage(GroupRole other) {
        return this.priority > other.priority;
    }
}
