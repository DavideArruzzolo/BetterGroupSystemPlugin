package dzve.command.member;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.auth.ProfileServiceClient;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.service.group.GroupService;

import javax.annotation.Nonnull;

public class KickMemberCommand extends AbstractPlayerCommand {
    private final GroupService groupService;
    // Usiamo GAME_PROFILE_LOOKUP per ottenere l'UUID anche se offline
    @Nonnull
    private final RequiredArg<ProfileServiceClient.PublicGameProfile> target = withRequiredArg("target", "Member to kick", ArgTypes.GAME_PROFILE_LOOKUP);

    public KickMemberCommand(GroupService groupService) {
        super("kick", "Kick a member from the group");
        this.groupService = groupService;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        groupService.kickMember(player, target.get(ctx).getUuid());
    }
}