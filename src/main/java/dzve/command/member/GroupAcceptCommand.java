package dzve.command.member;

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
import dzve.service.group.member.GroupMemberService;
import dzve.utils.ChatFormatter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Objects;

public class GroupAcceptCommand extends AbstractPlayerCommand {
    private final GroupServiceFactory serviceFactory;
    private final GroupMemberService memberService;
    private final NotificationService notificationService;

    private final OptionalArg<String> groupName = withOptionalArg("group", "The name of group to accept invitation from", ArgTypes.STRING);

    public GroupAcceptCommand(final GroupServiceFactory serviceFactory) {
        super("accept", "Accepts a group invitation");
        this.serviceFactory = Objects.requireNonNull(serviceFactory, "ServiceFactory cannot be null");
        this.memberService = serviceFactory.getMemberService();
        this.notificationService = NotificationService.getInstance();
        setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void execute(@Nonnull final CommandContext context, @Nonnull final Store<EntityStore> store,
                           @Nonnull final Ref<EntityStore> ref, @Nonnull final PlayerRef playerRef, @Nonnull final World world) {
        final String targetGroup = groupName.get(context);
        GroupMemberService.AcceptanceRequest request = new GroupMemberService.AcceptanceRequest(playerRef.getUuid(), targetGroup);
        GroupMemberService.AcceptanceResult result = memberService.acceptInvitation(request);

        if (result instanceof GroupMemberService.AcceptanceSuccess success) {
            playerRef.sendMessage(
                    ChatFormatter.of("✓ You have joined the group!")
                            .withColor(Color.GREEN)
                            .withBold()
                            .toMessage()
            );

            notificationService.sendCustomNotification(
                    playerRef.getUuid(),
                    "Group Joined!",
                    "You have successfully joined the group.",
                    "Weapon_Sword_Nexus",
                    NotificationStyle.Success
            );
        } else if (result instanceof GroupMemberService.AcceptanceFailure failure) {
            playerRef.sendMessage(ChatFormatter.of("Failed to accept invitation: " + failure.reason()).toMessage());
        }
    }
}
