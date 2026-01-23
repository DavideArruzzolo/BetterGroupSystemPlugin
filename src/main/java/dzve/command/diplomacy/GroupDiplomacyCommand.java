package dzve.command.diplomacy;

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
import dzve.service.group.diplomacy.GroupDiplomacyService;
import dzve.utils.ChatFormatter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Objects;
import java.util.UUID;

public class GroupDiplomacyCommand extends AbstractPlayerCommand {
    private final GroupServiceFactory serviceFactory;
    private final GroupDiplomacyService diplomacyService;
    private final NotificationService notificationService;

    public GroupDiplomacyCommand(final GroupServiceFactory serviceFactory) {
        super("diplomacy", "Group diplomacy and relations commands");
        this.serviceFactory = Objects.requireNonNull(serviceFactory, "ServiceFactory cannot be null");
        this.diplomacyService = serviceFactory.getDiplomacyService();
        this.notificationService = NotificationService.getInstance();
        setPermissionGroup(GameMode.Adventure);

        // Diplomacy Subcommands
        //addSubCommand(new AllianceCommand(this));
        //addSubCommand(new WarCommand(this));
        //addSubCommand(new PeaceCommand(this));
        //addSubCommand(new TreatyCommand(this));
        //addSubCommand(new ListRelationsCommand(this));
        //addSubCommand(new StatusCommand(this));
    }

    /**
     * Handles base diplomacy command when no subcommand is specified.
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
                ChatFormatter.of("=== Group Diplomacy Commands ===")
                        .withGradient(Color.BLUE, Color.CYAN)
                        .withBold()
                        .append("\nUsage: /group diplomacy <subcommand>\n")
                        .withColor(Color.GRAY)
                        .append("Subcommands:\n")
                        .withColor(Color.GREEN)
                        .append("- alliance: Manages group alliances\n")
                        .withColor(Color.WHITE)
                        .append("- war: Manages group wars\n")
                        .withColor(Color.WHITE)
                        .append("- peace: Manages peace treaties\n")
                        .withColor(Color.WHITE)
                        .append("- treaty: Manages custom treaties\n")
                        .withColor(Color.WHITE)
                        .append("- list: Lists all diplomatic relations\n")
                        .withColor(Color.WHITE)
                        .append("- status: Checks diplomatic status with a group")
                        .withColor(Color.WHITE)
                        .toMessage()
        );

        notificationService.sendNotification(
                playerId,
                "Group Diplomacy Management Help",
                NotificationStyle.Default
        );
    }
}
