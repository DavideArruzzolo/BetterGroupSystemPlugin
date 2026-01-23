package dzve.command.management;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
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
import java.util.Optional;

public class GroupInfoCommand extends AbstractPlayerCommand {
    private final GroupManagementService managementService;
    private final NotificationService notificationService;

    private final OptionalArg<String> groupName = withOptionalArg("name", "The name of group to get info for", ArgTypes.STRING);

    public GroupInfoCommand(final GroupServiceFactory serviceFactory) {
        super("info", "Shows group information");
        this.managementService = Objects.requireNonNull(serviceFactory.getManagementService(), "Management service cannot be null");
        this.notificationService = NotificationService.getInstance();
        setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void execute(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store,
                           @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef, @Nonnull final World world) {
        final UUID playerId = playerRef.getUuid();
        final String targetGroup = groupName.get(context);

        if (targetGroup != null && !targetGroup.isBlank()) {
            Optional<dzve.model.Group> groupOpt = managementService.getGroupInfo(targetGroup);
            if (groupOpt.isPresent()) {
                displayGroupInfo(playerRef, groupOpt.get());
                notificationService.sendNotification(
                        playerId,
                        "Group Information Retrieved",
                        NotificationStyle.Default
                );
            } else {
                sendError(playerRef, playerId, "Group '" + targetGroup + "' not found.");
            }
        } else {
            dzve.model.Group group = managementService.getGroupForPlayer(playerRef.getUuid());
            if (group != null) {
                displayGroupInfo(playerRef, group);
                notificationService.sendNotification(
                        playerId,
                        "Your Group Information",
                        NotificationStyle.Default
                );
            } else {
                sendError(playerRef, playerId, "You are not in a group. Use /group create to start one!");
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

        notificationService.sendNotification(
                playerId,
                errorMessage,
                NotificationStyle.Warning
        );
    }
}
