package dzve.command.role;

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

public class SetRoleCommand extends AbstractPlayerCommand {
    private final GroupService groupService;
    @Nonnull
    private final RequiredArg<ProfileServiceClient.PublicGameProfile> target = withRequiredArg("target", "Member", ArgTypes.GAME_PROFILE_LOOKUP);
    @Nonnull
    private final RequiredArg<String> roleName = withRequiredArg("role", "Role Name", ArgTypes.STRING);

    public SetRoleCommand(GroupService groupService) {
        super("set_role", "Assign a role to a member");
        this.groupService = groupService;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        groupService.setRole(player, target.get(ctx).getUuid(), roleName.get(ctx));
    }
}