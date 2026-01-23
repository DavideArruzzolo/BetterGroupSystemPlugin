package dzve.command.economy;

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
import dzve.service.group.economy.GroupEconomyService;
import dzve.utils.ChatFormatter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Objects;
import java.util.UUID;

public class GroupBalanceCommand extends AbstractPlayerCommand {
    private final GroupServiceFactory serviceFactory;
    private final GroupEconomyService economyService;
    private final NotificationService notificationService;

    public GroupBalanceCommand(final GroupServiceFactory serviceFactory) {
        super("balance", "Shows group bank balance");
        this.serviceFactory = Objects.requireNonNull(serviceFactory, "ServiceFactory cannot be null");
        this.economyService = serviceFactory.getEconomyService();
        this.notificationService = NotificationService.getInstance();
        setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void execute(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store,
                           @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef, @Nonnull final World world) {
        dzve.model.Group group = serviceFactory.getCoreGroupService().getGroupForPlayer(playerRef.getUuid());
        if (group == null) {
            playerRef.sendMessage(
                    ChatFormatter.of("✗ You are not in a group.")
                            .withColor(Color.RED)
                            .toMessage()
            );
            return;
        }

        GroupEconomyService.BalanceRequest request = new GroupEconomyService.BalanceRequest(playerRef.getUuid(), group.getId());
        GroupEconomyService.BalanceResult result = economyService.getBalance(request);

        if (result instanceof GroupEconomyService.BalanceSuccess success) {
            playerRef.sendMessage(
                    ChatFormatter.of("=== Group Bank Balance ===")
                            .withGradient(Color.GOLD, Color.ORANGE)
                            .withBold()
                            .append("\nBalance: ")
                            .withColor(Color.GRAY)
                            .append(String.format("%,.2f", success.balance()))
                            .withGradient(Color.GREEN, Color.LIME)
                            .withBold()
                            .append(" coins")
                            .withColor(Color.WHITE)
                            .toMessage()
            );

            notificationService.sendCustomNotification(
                    playerRef.getUuid(),
                    "Balance Retrieved",
                    "Group bank balance: " + String.format("%,.2f", success.balance()),
                    "Weapon_Sword_Nexus",
                    NotificationStyle.Default
            );
        } else if (result instanceof GroupEconomyService.BalanceFailure failure) {
            playerRef.sendMessage(ChatFormatter.of("Failed to get balance: " + failure.reason()).toMessage());
        }
    }
}
