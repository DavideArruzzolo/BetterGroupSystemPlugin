package dzve.command.management;

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
import dzve.service.group.management.GroupManagementService;
import dzve.utils.ChatFormatter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Objects;
import java.util.UUID;

public class GroupLeaveCommand extends AbstractPlayerCommand {
    private final GroupManagementService managementService;
    private final NotificationService notificationService;

    public GroupLeaveCommand(final GroupServiceFactory serviceFactory) {
        super("leave", "Leaves your current group");
        this.managementService = Objects.requireNonNull(serviceFactory.getManagementService(), "Management service cannot be null");
        this.notificationService = NotificationService.getInstance();
        setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void execute(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store,
                           @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef, @Nonnull final World world) {
        GroupManagementService.GroupLeaveResult result = managementService.leaveGroup(playerRef.getUuid());

        if (result instanceof GroupManagementService.GroupLeaveSuccess success) {
            playerRef.sendMessage(
                    ChatFormatter.of("You have left the group.")
                            .withColor(Color.YELLOW)
                            .toMessage()
            );
            
            notificationService.sendCustomNotification(
                    playerRef.getUuid(),
                    "Group Left",
                    "You have successfully left your group.",
                    "Weapon_Sword_Nexus",
                    NotificationStyle.Warning
            );
        } else if (result instanceof GroupManagementService.GroupLeaveFailure failure) {
            playerRef.sendMessage(ChatFormatter.of("Failed to leave group: " + failure.reason()).toMessage());
        }
    }
}
