package dzve.command.member;

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
import dzve.service.group.member.GroupMemberService;
import dzve.utils.ChatFormatter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Objects;
import java.util.UUID;

public class GroupMemberCommand extends AbstractPlayerCommand {
    private final GroupServiceFactory serviceFactory;
    private final GroupMemberService memberService;
    private final NotificationService notificationService;

    public GroupMemberCommand(final GroupServiceFactory serviceFactory) {
        super("member", "Group member management commands");
        this.serviceFactory = Objects.requireNonNull(serviceFactory, "ServiceFactory cannot be null");
        this.memberService = serviceFactory.getMemberService();
        this.notificationService = NotificationService.getInstance();
        setPermissionGroup(GameMode.Adventure);

        // Member Subcommands
        addSubCommand(new GroupInviteCommand(serviceFactory));
        addSubCommand(new GroupAcceptCommand(serviceFactory));
        //addSubCommand(new ListInvitesCommand(this));
        //addSubCommand(new KickCommand(this));
        //addSubCommand(new PromoteCommand(this));
        //addSubCommand(new DemoteCommand(this));
        //addSubCommand(new TransferCommand(this));
    }

    /**
     * Handles base member command when no subcommand is specified.
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
                ChatFormatter.of("=== Group Member Commands ===")
                        .withGradient(Color.CYAN, Color.BLUE)
                        .withBold()
                        .append("\nUsage: /group member <subcommand>\n")
                        .withColor(Color.GRAY)
                        .append("Subcommands:\n")
                        .withColor(Color.GREEN)
                        .append("- invite: Invites a player to your group\n")
                        .withColor(Color.WHITE)
                        .append("- invitations: Lists pending invitations\n")
                        .withColor(Color.WHITE)
                        .append("- accept: Accepts a group invitation\n")
                        .withColor(Color.WHITE)
                        .append("- kick: Kicks a player from your group\n")
                        .withColor(Color.WHITE)
                        .append("- promote: Promotes a group member\n")
                        .withColor(Color.WHITE)
                        .append("- demote: Demotes a group member\n")
                        .withColor(Color.WHITE)
                        .append("- transfer: Transfers group leadership")
                        .withColor(Color.WHITE)
                        .toMessage()
        );

        notificationService.sendNotification(
                playerId,
                "Group Member Management Help",
                NotificationStyle.Default
        );
    }
}
