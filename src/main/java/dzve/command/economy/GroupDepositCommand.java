package dzve.command.economy;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import dzve.command.arguments.economy.EconomyArguments;
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

public class GroupDepositCommand extends AbstractPlayerCommand {
    private final GroupServiceFactory serviceFactory;
    private final GroupEconomyService economyService;
    private final NotificationService notificationService;

    private final EconomyArguments.AmountArgument amount = withRequiredArg(EconomyArguments.amount());

    public GroupDepositCommand(final GroupServiceFactory serviceFactory) {
        super("deposit", "Deposits money to group bank");
        this.serviceFactory = Objects.requireNonNull(serviceFactory, "ServiceFactory cannot be null");
        this.economyService = serviceFactory.getEconomyService();
        this.notificationService = NotificationService.getInstance();
        setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void execute(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store,
                           @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef, @Nonnull final World world) {
        final UUID playerId = playerRef.getUuid();
        final double depositAmount = amount.get(context);

        if (depositAmount <= 0) {
            sendError(playerRef, playerId, "Amount must be positive!");
            return;
        }

        if (depositAmount > 1000000) {
            sendError(playerRef, playerId, "Amount cannot exceed 1,000,000!");
            return;
        }

        dzve.model.Group group = serviceFactory.getCoreGroupService().getGroupForPlayer(playerId);
        if (group == null) {
            sendError(playerRef, playerId, "You are not in a group!");
            return;
        }

        playerRef.sendMessage(
                ChatFormatter.of("Depositing ")
                        .withColor(Color.YELLOW)
                        .append(String.format("%,.2f", depositAmount))
                        .withGradient(Color.GOLD, Color.ORANGE)
                        .withBold()
                        .append(" to group bank...")
                        .withColor(Color.YELLOW)
                        .toMessage()
        );

        GroupEconomyService.DepositRequest request = new GroupEconomyService.DepositRequest(
                playerId, group.getId(), depositAmount);
        GroupEconomyService.DepositResult result = economyService.deposit(request);

        if (result instanceof GroupEconomyService.DepositSuccess success) {
            playerRef.sendMessage(
                    ChatFormatter.of("✓ Successfully deposited ")
                            .withColor(Color.GREEN)
                            .withBold()
                            .append(String.format("%,.2f", depositAmount))
                            .withGradient(Color.GOLD, Color.ORANGE)
                            .withBold()
                            .append(" to ")
                            .withColor(Color.GREEN)
                            .append(group.getName())
                            .withGradient(Color.CYAN, Color.AQUA)
                            .withBold()
                            .append("'s bank!")
                            .withColor(Color.GREEN)
                            .toMessage()
            );

            notificationService.sendCustomNotification(
                    playerId,
                    "Deposit Successful!",
                    String.format("%,.2f", depositAmount) + " has been added to " + group.getName() + " bank",
                    "Weapon_Sword_Nexus",
                    NotificationStyle.Success
            );
        } else if (result instanceof GroupEconomyService.DepositFailure failure) {
            sendError(playerRef, playerId, "Failed to deposit: " + failure.reason());
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
