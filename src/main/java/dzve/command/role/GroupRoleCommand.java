package dzve.command.role;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.service.NotificationService;
import dzve.service.group.GroupServiceFactory;
import dzve.service.group.role.GroupRoleService;
import dzve.utils.ChatFormatter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Objects;
import java.util.UUID;

public class GroupRoleCommand extends AbstractPlayerCommand {
    private final GroupServiceFactory serviceFactory;
    private final GroupRoleService roleService;
    private final NotificationService notificationService;

    public GroupRoleCommand(final GroupServiceFactory serviceFactory) {
        super("role", "Group role management commands");
        this.serviceFactory = Objects.requireNonNull(serviceFactory, "ServiceFactory cannot be null");
        this.roleService = serviceFactory.getRoleService();
        this.notificationService = NotificationService.getInstance();
        setPermissionGroup(GameMode.Adventure);

        // Role Subcommands
        //addSubCommand(new CreateRoleCommand(this));
        //addSubCommand(new EditRoleCommand(this));
        //addSubCommand(new DeleteRoleCommand(this));
        //addSubCommand(new ListRolesCommand(this));
        //addSubCommand(new AssignRoleCommand(this));
        //addSubCommand(new UnassignRoleCommand(this));
    }

    /**
     * Handles base role command when no subcommand is specified.
     * Displays help information to player.
     *
     * @param context   The command execution context
     * @param store     The entity store
     * @param ref       The entity reference
     * @param playerRef The player reference
     * @param world     The world where command was executed
     */
    @Override
    protected void execute(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store,
                           @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef, @Nonnull final World world) {
        final UUID playerId = playerRef.getUuid();

        // Send enhanced help message with both chat and notification
        playerRef.sendMessage(
                ChatFormatter.of("=== Group Role Commands ===")
                        .withGradient(Color.PURPLE, Color.MAGENTA)
                        .withBold()
                        .append("\nUsage: /group role <subcommand>\n")
                        .withColor(Color.GRAY)
                        .append("Subcommands:\n")
                        .withColor(Color.GREEN)
                        .append("- create: Creates a new group role\n")
                        .withColor(Color.WHITE)
                        .append("- edit: Edits an existing group role\n")
                        .withColor(Color.WHITE)
                        .append("- delete: Deletes a group role\n")
                        .withColor(Color.WHITE)
                        .append("- list: Lists all group roles\n")
                        .withColor(Color.WHITE)
                        .append("- assign: Assigns a role to a member\n")
                        .withColor(Color.WHITE)
                        .append("- unassign: Removes a role from a member")
                        .withColor(Color.WHITE)
                        .toMessage()
        );

        notificationService.sendNotification(
                playerId,
                "Group Role Management Help",
                NotificationStyle.Default
        );
    }
}
