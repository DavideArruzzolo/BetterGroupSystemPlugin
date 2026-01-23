package dzve.command.economy;

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
import dzve.service.group.economy.GroupEconomyService;
import dzve.utils.ChatFormatter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Objects;
import java.util.UUID;

public class GroupEconomyCommand extends AbstractPlayerCommand {
    private final GroupServiceFactory serviceFactory;
    private final GroupEconomyService economyService;
    private final NotificationService notificationService;

    public GroupEconomyCommand(final GroupServiceFactory serviceFactory) {
        super("economy", "Group economy and banking commands");
        this.serviceFactory = Objects.requireNonNull(serviceFactory, "ServiceFactory cannot be null");
        this.economyService = serviceFactory.getEconomyService();
        this.notificationService = NotificationService.getInstance();
        setPermissionGroup(GameMode.Adventure);

        // Economy Subcommands
        addSubCommand(new GroupDepositCommand(serviceFactory));
        addSubCommand(new GroupBalanceCommand(serviceFactory));
        //addSubCommand(new WithdrawCommand(this));
        //addSubCommand(new UpgradeCommand(this));
        //addSubCommand(new TransactionHistoryCommand(this));
        //addSubCommand(new SetTaxCommand(this));
    }

    /**
     * Handles base economy command when no subcommand is specified.
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
                ChatFormatter.of("=== Group Economy Commands ===")
                        .withGradient(Color.GOLD, Color.ORANGE)
                        .withBold()
                        .append("\nUsage: /group economy <subcommand>\n")
                        .withColor(Color.GRAY)
                        .append("Subcommands:\n")
                        .withColor(Color.GREEN)
                        .append("- deposit: Deposits money to group bank\n")
                        .withColor(Color.WHITE)
                        .append("- withdraw: Withdraws money from group bank\n")
                        .withColor(Color.WHITE)
                        .append("- balance: Shows group bank balance\n")
                        .withColor(Color.WHITE)
                        .append("- upgrade: Upgrades group level\n")
                        .withColor(Color.WHITE)
                        .append("- history: Shows transaction history\n")
                        .withColor(Color.WHITE)
                        .append("- tax: Sets group tax rate")
                        .withColor(Color.WHITE)
                        .toMessage()
        );

        notificationService.sendNotification(
                playerId,
                "Group Economy Management Help",
                NotificationStyle.Default
        );
    }
}
