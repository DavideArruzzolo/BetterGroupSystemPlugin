package dzve.command.management;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.service.NotificationService;
import dzve.service.group.GroupServiceFactory;
import dzve.service.group.management.GroupManagementService;
import dzve.utils.ChatFormatter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Objects;
import java.util.UUID;

public class GroupManagementCommand extends AbstractPlayerCommand {
    private final GroupServiceFactory serviceFactory;
    private final GroupManagementService managementService;
    private final NotificationService notificationService;

    public GroupManagementCommand(final GroupServiceFactory serviceFactory) {
        super("manage", "Group management commands");
        this.serviceFactory = Objects.requireNonNull(serviceFactory, "ServiceFactory cannot be null");
        this.managementService = serviceFactory.getManagementService();
        this.notificationService = NotificationService.getInstance();
        setPermissionGroup(GameMode.Adventure);

        // Management Subcommands
        addSubCommand(new CreateCommand(this));
        addSubCommand(new InfoCommand(this));
        addSubCommand(new UpdateCommand(this));
        addSubCommand(new DeleteCommand(this));
        addSubCommand(new LeaveCommand(this));
    }

    /**
     * Handles base management command when no subcommand is specified.
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
                ChatFormatter.of("=== Group Management Commands ===")
                        .withGradient(Color.ORANGE, Color.RED)
                        .withBold()
                        .append("\nUsage: /group manage <subcommand>\n")
                        .withColor(Color.GRAY)
                        .append("Subcommands:\n")
                        .withColor(Color.GREEN)
                        .append("- create: Creates a new group\n")
                        .withColor(Color.WHITE)
                        .append("- info: Shows group information\n")
                        .withColor(Color.WHITE)
                        .append("- update: Updates group settings\n")
                        .withColor(Color.WHITE)
                        .append("- delete: Deletes your group\n")
                        .withColor(Color.WHITE)
                        .append("- leave: Leaves your current group")
                        .withColor(Color.WHITE)
                        .toMessage()
        );

        notificationService.sendNotification(
                playerId,
                "Group Management Help",
                NotificationStyle.Default
        );
    }

    /**
     * Subcommand for creating a new group.
     * Requires group name, tag, and optionally description and color.
     */
    public static class CreateCommand extends AbstractPlayerCommand {
        @Nonnull
        private final RequiredArg<String> name = withRequiredArg("name", "The name of group", ArgTypes.STRING);
        @Nonnull
        private final RequiredArg<String> tag = withRequiredArg("tag", "The tag of group (2-6 characters)", ArgTypes.STRING);
        @Nonnull
        private final OptionalArg<String> description = withOptionalArg("description", "The description of group", ArgTypes.STRING);
        @Nonnull
        private final OptionalArg<String> color = withOptionalArg("color", "The color of group (hex format, e.g., #FF0000)", ArgTypes.STRING);

        private final GroupManagementCommand parent;

        /**
         * Constructs a new CreateCommand.
         *
         * @param parent The parent GroupManagementCommand instance
         * @throws NullPointerException if parent is null
         */
        public CreateCommand(final GroupManagementCommand parent) {
            super("create", "Creates a new group");
            this.parent = parent;
        }

        /**
         * Executes create group command.
         * Validates input and creates a new group with specified parameters.
         *
         * @param context   The command execution context
         * @param store     The entity store
         * @param ref       The entity reference
         * @param playerRef The player reference executing command
         * @param world     The world where command was executed
         */
        @Override
        protected void execute(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store,
                               @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef, @Nonnull final World world) {
            final UUID playerId = playerRef.getUuid();
            final String groupName = name.get(context);
            final String groupTag = tag.get(context);
            final String groupDescription = description.get(context);
            final String groupColor = color.get(context);

            // Validate input parameters
            if (groupName == null || groupName.trim().isEmpty()) {
                sendError(playerRef, playerId, "Group name cannot be empty!");
                return;
            }

            if (groupTag == null || groupTag.trim().isEmpty()) {
                sendError(playerRef, playerId, "Group tag cannot be empty!");
                return;
            }

            if (groupTag.length() < 2 || groupTag.length() > 6) {
                sendError(playerRef, playerId, "Group tag must be between 2-6 characters!");
                return;
            }

            // Send processing notification
            playerRef.sendMessage(
                    ChatFormatter.of("Creating group ")
                            .withColor(Color.YELLOW)
                            .append(groupName)
                            .withGradient(Color.GOLD, Color.ORANGE)
                            .withBold()
                            .append("...")
                            .withColor(Color.YELLOW)
                            .toMessage()
            );

            var request = new GroupManagementService.GroupCreationRequest(
                    groupName, groupTag, playerId);
            var result = parent.managementService.createGroup(request);

            if (result instanceof GroupManagementService.GroupCreationSuccess success) {
                // Enhanced success message
                playerRef.sendMessage(
                        ChatFormatter.of("✓ Group ")
                                .withColor(Color.GREEN)
                                .withBold()
                                .append(success.group().getName())
                                .withGradient(Color.GOLD, Color.YELLOW)
                                .withBold()
                                .append(" [" + success.group().getTag() + "] created successfully!")
                                .withColor(Color.GREEN)
                                .toMessage()
                );

                // Send success notification
                parent.notificationService.sendCustomNotification(
                        playerId,
                        "Group Created Successfully!",
                        "Your group '" + success.group().getName() + "' is now ready.",
                        "Weapon_Sword_Nexus",
                        NotificationStyle.Success
                );
            } else if (result instanceof GroupManagementService.GroupCreationFailure failure) {
                sendError(playerRef, playerId, "Failed to create group: " + failure.reason());
            }
        }

        private void sendError(final PlayerRef playerRef, final UUID playerId, final String errorMessage) {
            playerRef.sendMessage(
                    ChatFormatter.of("✗ ")
                            .withColor(Color.RED)
                            .withBold()
                            .append(errorMessage)
                            .withColor(Color.RED)
                            .toMessage()
            );

            parent.notificationService.sendNotification(
                    playerId,
                    errorMessage,
                    NotificationStyle.Danger
            );
        }
    }

    /**
     * Subcommand for showing group information.
     */
    public static class InfoCommand extends AbstractPlayerCommand {
        @Nonnull
        private final OptionalArg<String> groupName = withOptionalArg("group", "The name of group to view info about, if empty view info of your group", ArgTypes.STRING);

        private final GroupManagementCommand parent;

        public InfoCommand(final GroupManagementCommand parent) {
            super("info", "Shows information about a group");
            this.parent = parent;
        }

        @Override
        protected void execute(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store,
                               @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef, @Nonnull final World world) {
            final UUID playerId = playerRef.getUuid();
            final String targetGroup = groupName.get(context);

            if (targetGroup != null && !targetGroup.isBlank()) {
                var groupOpt = parent.managementService.getGroupInfo(targetGroup);
                if (groupOpt.isPresent()) {
                    displayGroupInfo(playerRef, groupOpt.get());
                    parent.notificationService.sendNotification(
                            playerId,
                            "Group Information Retrieved",
                            NotificationStyle.Default
                    );
                } else {
                    sendError(playerRef, playerId, "Group '" + targetGroup + "' not found.");
                }
            } else {
                var group = parent.managementService.getGroupForPlayer(playerId);
                if (group != null) {
                    displayGroupInfo(playerRef, group);
                    parent.notificationService.sendNotification(
                            playerId,
                            "Your Group Information",
                            NotificationStyle.Default
                    );
                } else {
                    sendError(playerRef, playerId, "You are not in a group. Use /group manage create to start one!");
                }
            }
        }

        private void displayGroupInfo(final PlayerRef playerRef, final dzve.model.Group group) {
            playerRef.sendMessage(
                    ChatFormatter.of("=== Group Information ===")
                            .withGradient(Color.GOLD, Color.ORANGE)
                            .withBold()
                            .append("\nName: ")
                            .withColor(Color.GRAY)
                            .append(group.getName() + " [" + group.getTag() + "]\n")
                            .withGradient(Color.GOLD, Color.YELLOW)
                            .append("Members: ")
                            .withColor(Color.GRAY)
                            .append(String.valueOf(group.getMemberCount()) + "\n")
                            .withColor(Color.GRAY)
                            .append("Leader: ")
                            .withColor(Color.YELLOW)
                            .append(group.getLeaderId().toString().substring(0, 8) + "...")
                            .toMessage()
            );

            if (group.getDescription() != null && !group.getDescription().isBlank()) {
                playerRef.sendMessage(
                        ChatFormatter.of("Description: ")
                                .withColor(Color.GRAY)
                                .append(group.getDescription())
                                .withColor(Color.WHITE)
                                .toMessage()
                );
            }
        }

        private void sendError(final PlayerRef playerRef, final UUID playerId, final String errorMessage) {
            playerRef.sendMessage(
                    ChatFormatter.of("✗ ")
                            .withColor(Color.RED)
                            .withBold()
                            .append(errorMessage)
                            .withColor(Color.RED)
                            .toMessage()
            );

            parent.notificationService.sendNotification(
                    playerId,
                    errorMessage,
                    NotificationStyle.Warning
            );
        }
    }

    /**
     * Subcommand for updating group information.
     */
    public static class UpdateCommand extends AbstractPlayerCommand {
        @Nonnull
        private final RequiredArg<String> field = withRequiredArg("field", "The field to update (name/tag/color/desc)", ArgTypes.STRING);
        @Nonnull
        private final RequiredArg<String> value = withRequiredArg("value", "The new value for the field", ArgTypes.STRING);

        private final GroupManagementCommand parent;

        public UpdateCommand(final GroupManagementCommand parent) {
            super("update", "Updates group information");
            this.parent = parent;
        }

        @Override
        protected void execute(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store,
                               @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef, @Nonnull final World world) {
            final String fieldName = field.get(context);
            final String fieldValue = value.get(context);
            final UUID playerId = playerRef.getUuid();
            dzve.model.Group group = parent.managementService.getGroupForPlayer(playerId);
            if (group == null) {
                playerRef.sendMessage(ChatFormatter.of("You are not in a group.").withColor(Color.RED).toMessage());
                return;
            }

            var request = new GroupManagementService.GroupUpdateRequest(group.getId(),
                    "name".equalsIgnoreCase(fieldName) ? fieldValue : null,
                    "tag".equalsIgnoreCase(fieldName) ? fieldValue : null,
                    "desc".equalsIgnoreCase(fieldName) ? fieldValue : null
            );

            var result = parent.managementService.updateGroup(request);

            if (result instanceof GroupManagementService.GroupUpdateSuccess) {
                playerRef.sendMessage(ChatFormatter.of("Group " + fieldName + " updated successfully.").toMessage());
            } else if (result instanceof GroupManagementService.GroupUpdateFailure failure) {
                playerRef.sendMessage(ChatFormatter.of("Failed to update group " + fieldName + ": " + failure.reason()).toMessage());
            }
        }
    }

    /**
     * Subcommand for deleting a group.
     */
    public static class DeleteCommand extends AbstractPlayerCommand {
        @Nonnull
        private final OptionalArg<String> confirmation = withOptionalArg("confirm", "Type 'confirm' to delete your group", ArgTypes.STRING);

        private final GroupManagementCommand parent;

        public DeleteCommand(final GroupManagementCommand parent) {
            super("delete", "Deletes your group (leader only)");
            this.parent = parent;
        }

        @Override
        protected void execute(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store,
                               @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef, @Nonnull final World world) {
            final String confirm = confirmation.get(context);
            final UUID playerId = playerRef.getUuid();
            dzve.model.Group group = parent.managementService.getGroupForPlayer(playerId);
            if (group == null) {
                playerRef.sendMessage(ChatFormatter.of("You are not in a group.").withColor(Color.RED).toMessage());
                return;
            }

            if (!"confirm".equalsIgnoreCase(confirm)) {
                playerRef.sendMessage(ChatFormatter.of("Are you sure you want to delete group? This cannot be undone. Type /group manage delete confirm.").toMessage());
                return;
            }

            var result = parent.managementService.deleteGroup(new GroupManagementService.GroupDeletionRequest(group.getId(), playerId));

            if (result instanceof GroupManagementService.GroupDeletionSuccess) {
                playerRef.sendMessage(ChatFormatter.of("Group has been deleted.").toMessage());
            } else if (result instanceof GroupManagementService.GroupDeletionFailure failure) {
                playerRef.sendMessage(ChatFormatter.of("Failed to delete group: " + failure.reason()).toMessage());
            }
        }
    }

    /**
     * Subcommand for leaving a group.
     */
    public static class LeaveCommand extends AbstractPlayerCommand {
        private final GroupManagementCommand parent;

        public LeaveCommand(final GroupManagementCommand parent) {
            super("leave", "Leaves your current group");
            this.parent = parent;
        }

        @Override
        protected void execute(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store,
                               @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef, @Nonnull final World world) {
            var result = parent.managementService.leaveGroup(playerRef.getUuid());

            if (result instanceof GroupManagementService.GroupLeaveSuccess) {
                playerRef.sendMessage(ChatFormatter.of("You have left the group.").toMessage());
            } else if (result instanceof GroupManagementService.GroupLeaveFailure failure) {
                playerRef.sendMessage(ChatFormatter.of("Failed to leave group: " + failure.reason()).toMessage());
            }
        }
    }
}
