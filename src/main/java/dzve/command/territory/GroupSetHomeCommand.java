package dzve.command.territory;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.world.location.Location;
import dzve.service.NotificationService;
import dzve.service.group.GroupServiceFactory;
import dzve.service.group.territory.GroupTerritoryService;
import dzve.utils.ChatFormatter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Objects;
import java.util.UUID;

public class GroupSetHomeCommand extends AbstractPlayerCommand {
    private final GroupServiceFactory serviceFactory;
    private final GroupTerritoryService territoryService;
    private final NotificationService notificationService;

    private final RequiredArg<String> homeName = withRequiredArg("name", "The name of home to set", ArgTypes.STRING);

    public GroupSetHomeCommand(final GroupServiceFactory serviceFactory) {
        super("sethome", "Sets a group home");
        this.serviceFactory = Objects.requireNonNull(serviceFactory, "ServiceFactory cannot be null");
        this.territoryService = serviceFactory.getTerritoryService();
        this.notificationService = NotificationService.getInstance();
        setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void execute(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store,
                           @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef, @Nonnull final World world) {
        final UUID playerId = playerRef.getUuid();
        final String name = homeName.get(context);

        if (name == null || name.trim().isEmpty()) {
            sendError(playerRef, playerId, "Home name cannot be empty!");
            return;
        }

        if (name.length() < 2 || name.length() > 32) {
            sendError(playerRef, playerId, "Home name must be between 2-32 characters!");
            return;
        }

        dzve.model.Group group = serviceFactory.getCoreGroupService().getGroupForPlayer(playerId);
        if (group == null) {
            sendError(playerRef, playerId, "You are not in a group!");
            return;
        }

        playerRef.sendMessage(
                ChatFormatter.of("Setting home ")
                        .withColor(Color.YELLOW)
                        .append(name)
                        .withGradient(Color.GREEN, Color.LIME)
                        .withBold()
                        .append(" at your location...")
                        .withColor(Color.YELLOW)
                        .toMessage()
        );

        com.hypixel.hytale.api.util.Location apiLocation = playerRef.getLocation();
        Location location = new Location(apiLocation.x(), apiLocation.y(), apiLocation.z());
        GroupTerritoryService.HomeCreationRequest request = new GroupTerritoryService.HomeCreationRequest(
                playerId, name, location);
        GroupTerritoryService.HomeCreationResult result = territoryService.setHome(request);

        if (result instanceof GroupTerritoryService.HomeCreationSuccess success) {
            playerRef.sendMessage(
                    ChatFormatter.of("✓ Home ")
                            .withColor(Color.GREEN)
                            .withBold()
                            .append(success.createdHome().getName())
                            .withGradient(Color.GREEN, Color.LIME)
                            .withBold()
                            .append(" set successfully!\nLocation: ")
                            .withColor(Color.GRAY)
                            .append(String.format("%.1f, %.1f, %.1f", location.getX(), location.getY(), location.getZ()))
                            .withColor(Color.WHITE)
                            .toMessage()
            );

            notificationService.sendCustomNotification(
                    playerId,
                    "Home Set!",
                    "Home '" + success.createdHome().getName() + "' has been created for " + group.getName(),
                    "Weapon_Sword_Nexus",
                    NotificationStyle.Success
            );
        } else if (result instanceof GroupTerritoryService.HomeCreationFailure failure) {
            sendError(playerRef, playerId, "Failed to set home: " + failure.reason());
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
                NotificationStyle.Danger
        );
    }
}
