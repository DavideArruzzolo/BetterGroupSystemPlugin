package dzve.command.member;

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
import dzve.service.NotificationService;
import dzve.service.group.GroupServiceFactory;
import dzve.service.group.member.GroupMemberService;
import dzve.utils.ChatFormatter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Objects;
import java.util.UUID;

public class GroupInviteCommand extends AbstractPlayerCommand {
    private final GroupServiceFactory serviceFactory;
    private final GroupMemberService memberService;
    private final NotificationService notificationService;

    private final RequiredArg<String> playerName = withRequiredArg("player", "The name of player to invite", ArgTypes.STRING);

    public GroupInviteCommand(final GroupServiceFactory serviceFactory) {
        super("invite", "Invites a player to your group");
        this.serviceFactory = Objects.requireNonNull(serviceFactory, "ServiceFactory cannot be null");
        this.memberService = serviceFactory.getMemberService();
        this.notificationService = NotificationService.getInstance();
        setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void execute(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store,
                           @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef, @Nonnull final World world) {
        final UUID playerId = playerRef.getUuid();
        final String targetPlayer = playerName.get(context);

        if (targetPlayer == null || targetPlayer.trim().isEmpty()) {
            sendError(playerRef, playerId, "Player name cannot be empty!");
            return;
        }

        PlayerRef target = world.getPlayerByName(targetPlayer);

        if (target == null) {
            sendError(playerRef, playerId, "Player '" + targetPlayer + "' not found or is not online.");
            return;
        }

        dzve.model.Group group = serviceFactory.getCoreGroupService().getGroupForPlayer(playerId);
        if (group == null) {
            sendError(playerRef, playerId, "You are not in a group. Create or join one first!");
            return;
        }

        if (target.getUuid().equals(playerId)) {
            sendError(playerRef, playerId, "You cannot invite yourself to the group!");
            return;
        }

        playerRef.sendMessage(
                ChatFormatter.of("Inviting ")
                        .withColor(Color.YELLOW)
                        .append(targetPlayer)
                        .withGradient(Color.CYAN, Color.AQUA)
                        .withBold()
                        .append(" to your group...")
                        .withColor(Color.YELLOW)
                        .toMessage()
        );

        GroupMemberService.InvitationRequest request = new GroupMemberService.InvitationRequest(playerId, target.getUuid(), group.getId());
        GroupMemberService.InvitationResult result = memberService.invitePlayer(request);

        if (result instanceof GroupMemberService.InvitationSuccess success) {
            playerRef.sendMessage(
                    ChatFormatter.of("✓ Successfully invited ")
                            .withColor(Color.GREEN)
                            .withBold()
                            .append(targetPlayer)
                            .withGradient(Color.CYAN, Color.AQUA)
                            .withBold()
                            .append(" to your group!")
                            .withColor(Color.GREEN)
                            .toMessage()
            );

            notificationService.sendCustomNotification(
                    playerId,
                    "Invitation Sent!",
                    targetPlayer + " has been invited to join " + group.getName(),
                    "Weapon_Sword_Nexus",
                    NotificationStyle.Success
            );

            target.sendMessage(
                    ChatFormatter.of("✉ You have been invited to join ")
                            .withGradient(Color.GOLD, Color.ORANGE)
                            .withBold()
                            .append(group.getName())
                            .withGradient(Color.GOLD, Color.YELLOW)
                            .withBold()
                            .append("!\nUse ")
                            .withColor(Color.GRAY)
                            .append("/group accept " + group.getName() + " to join.")
                            .withColor(Color.WHITE)
                            .toMessage()
            );

            notificationService.sendCustomNotification(
                    target.getUuid(),
                    "Group Invitation!",
                    "You've been invited to join " + group.getName(),
                    "Weapon_Sword_Nexus",
                    NotificationStyle.Default
            );
        } else if (result instanceof GroupMemberService.InvitationFailure failure) {
            sendError(playerRef, playerId, "Failed to invite player: " + failure.reason());
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
