package dzve.command.territory;

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
import dzve.service.group.territory.GroupTerritoryService;
import dzve.utils.ChatFormatter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Objects;
import java.util.UUID;

public class GroupHomeCommand extends AbstractPlayerCommand {
    private final GroupServiceFactory serviceFactory;
    private final GroupTerritoryService territoryService;
    private final NotificationService notificationService;

    private final OptionalArg<String> homeName = withOptionalArg("name", "The name of home to teleport to", ArgTypes.STRING);

    public GroupHomeCommand(final GroupServiceFactory serviceFactory) {
        super("home", "Teleports to a group home");
        this.serviceFactory = Objects.requireNonNull(serviceFactory, "ServiceFactory cannot be null");
        this.territoryService = serviceFactory.getTerritoryService();
        this.notificationService = NotificationService.getInstance();
        setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void execute(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store,
                           @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef, @Nonnull final World world) {
        //final String name = homeName.get(context);

        //dzve.model.Group group = serviceFactory.getCoreGroupService().getGroupForPlayer(playerRef.getUuid());
        //if (group == null) {
        //    playerRef.sendMessage(
        //            ChatFormatter.of("✗ You are not in a group.")
        //                    .withColor(Color.RED)
        //                    .toMessage()
        //    );
        //    return;
        //}

        //GroupTerritoryService.HomeTeleportRequest request = new GroupTerritoryService.HomeTeleportRequest(
        //        playerRef.getUuid(), group.getId(), name);
        //GroupTerritoryService.HomeTeleportResult result = territoryService.teleportToHome(request);

        //switch (result) {
        //    case GroupTerritoryService.HomeTeleportSuccess success -> {
        //        playerRef.sendMessage(
        //                ChatFormatter.of("✓ Teleporting to home...")
        //                        .withColor(Color.GREEN)
        //                        .withBold()
        //                        .toMessage()
        //        );

        //        notificationService.sendCustomNotification(
        //                playerRef.getUuid(),
        //                "Teleporting",
        //                "Teleporting to group home...",
        //                "Weapon_Sword_Nexus",
        //                NotificationStyle.Default
        //        );
        //    }
        //    case GroupTerritoryService.HomeTeleportFailure failure ->
        //            playerRef.sendMessage(ChatFormatter.of("Failed to teleport to home: " + failure.reason()).toMessage());
        //}
    }
}
