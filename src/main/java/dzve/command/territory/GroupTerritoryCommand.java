package dzve.command.territory;

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
import dzve.service.group.territory.GroupTerritoryService;
import dzve.utils.ChatFormatter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Objects;
import java.util.UUID;

public class GroupTerritoryCommand extends AbstractPlayerCommand {
    private final GroupServiceFactory serviceFactory;
    private final GroupTerritoryService territoryService;
    private final NotificationService notificationService;

    public GroupTerritoryCommand(final GroupServiceFactory serviceFactory) {
        super("territory", "Group territory and home management commands");
        this.serviceFactory = Objects.requireNonNull(serviceFactory, "ServiceFactory cannot be null");
        this.territoryService = serviceFactory.getTerritoryService();
        this.notificationService = NotificationService.getInstance();
        setPermissionGroup(GameMode.Adventure);

        // Territory Subcommands
        //addSubCommand(new SetHomeCommand(this));
        //addSubCommand(new HomeCommand(this));
        //addSubCommand(new ListHomeCommand(this));
        //addSubCommand(new DelHomeCommand(this));
        //addSubCommand(new EditHomeCommand(this));
        //addSubCommand(new SetDefaultHomeCommand(this));
        //addSubCommand(new ClaimCommand(this));
        //addSubCommand(new UnclaimCommand(this));
        //addSubCommand(new MapCommand(this));
    }

    /**
     * Handles base territory command when no subcommand is specified.
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
                ChatFormatter.of("=== Group Territory Commands ===")
                        .withGradient(Color.GREEN, Color.DARK_GREEN)
                        .withBold()
                        .append("\nUsage: /group territory <subcommand>\n")
                        .withColor(Color.GRAY)
                        .append("Subcommands:\n")
                        .withColor(Color.GREEN)
                        .append("- sethome: Sets a group home\n")
                        .withColor(Color.WHITE)
                        .append("- home: Teleports to a group home\n")
                        .withColor(Color.WHITE)
                        .append("- listhomes: Lists all group homes\n")
                        .withColor(Color.WHITE)
                        .append("- delhome: Deletes a group home\n")
                        .withColor(Color.WHITE)
                        .append("- edithome: Edits a group home\n")
                        .withColor(Color.WHITE)
                        .append("- setdefault: Sets default group home\n")
                        .withColor(Color.WHITE)
                        .append("- claim: Claims current chunk for group\n")
                        .withColor(Color.WHITE)
                        .append("- unclaim: Unclaims current chunk\n")
                        .withColor(Color.WHITE)
                        .append("- map: Shows group territory map")
                        .withColor(Color.WHITE)
                        .toMessage()
        );

        notificationService.sendNotification(
                playerId,
                "Group Territory Management Help",
                NotificationStyle.Default
        );
    }
}
