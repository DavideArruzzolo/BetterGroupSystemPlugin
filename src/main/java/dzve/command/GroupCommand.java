package dzve.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.command.diplomacy.GroupDiplomacyCommand;
import dzve.command.economy.GroupEconomyCommand;
import dzve.command.management.GroupManagementCommand;
import dzve.command.member.GroupMemberCommand;
import dzve.command.role.GroupRoleCommand;
import dzve.command.territory.GroupTerritoryCommand;
import dzve.service.NotificationService;
import dzve.service.group.GroupServiceFactory;
import dzve.utils.ChatFormatter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Objects;
import java.util.UUID;

public class GroupCommand extends AbstractPlayerCommand {
    private final GroupServiceFactory serviceFactory;
    private final NotificationService notificationService;

    public GroupCommand(final GroupServiceFactory serviceFactory) {
        super("group", "Group list of commands");
        this.serviceFactory = Objects.requireNonNull(serviceFactory, "ServiceFactory cannot be null");
        this.notificationService = NotificationService.getInstance();
        setPermissionGroup(GameMode.Adventure);

        // Register individual command subcommands
        addSubCommand(new GroupManagementCommand(serviceFactory));
        addSubCommand(new GroupMemberCommand(serviceFactory));
        addSubCommand(new GroupRoleCommand(serviceFactory));
        addSubCommand(new GroupTerritoryCommand(serviceFactory));
        addSubCommand(new GroupDiplomacyCommand(serviceFactory));
        addSubCommand(new GroupEconomyCommand(serviceFactory));
    }

    @Override
    protected void execute(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store,
                           @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef, @Nonnull final World world) {
        final UUID playerId = playerRef.getUuid();

        // Send enhanced help message with both chat and notification
        playerRef.sendMessage(
                ChatFormatter.of("=== Group System Commands ===")
                        .withGradient(Color.RED, Color.ORANGE)
                        .withBold()
                        .append("\nUsage: /group <command>\n")
                        .withColor(Color.GRAY)
                        .append("Available Commands:\n")
                        .withColor(Color.GREEN)
                        .append("- manage: Management commands\n")
                        .withColor(Color.WHITE)
                        .append("- member: Member commands\n")
                        .withColor(Color.WHITE)
                        .append("- role: Role commands\n")
                        .withColor(Color.WHITE)
                        .append("- territory: Territory commands\n")
                        .withColor(Color.WHITE)
                        .append("- diplomacy: Diplomacy commands\n")
                        .withColor(Color.WHITE)
                        .append("- economy: Economy commands")
                        .withColor(Color.WHITE)
                        .toMessage()
        );

        notificationService.sendCustomNotification(
                playerId,
                "Group System Help",
                "Use /group <command> to explore group commands",
                "Weapon_Sword_Nexus",
                NotificationStyle.Default
        );
    }
}
