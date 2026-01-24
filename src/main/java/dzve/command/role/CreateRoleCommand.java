package dzve.command.role;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.service.group.GroupService;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;

public class CreateRoleCommand extends AbstractPlayerCommand {
    private final GroupService groupService;
    @Nonnull
    private final RequiredArg<String> name = withRequiredArg("name", "Role name", ArgTypes.STRING);
    @Nonnull
    private final OptionalArg<String> grants = withOptionalArg("grants", "Permissions (space separated)", ArgTypes.STRING);

    public CreateRoleCommand(GroupService groupService) {
        super("create_role", "Create a new custom role");
        this.groupService = groupService;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        String grantsStr = grants.get(ctx);
        groupService.createRole(player, name.get(ctx),
                grantsStr != null ? Arrays.asList(grantsStr.split(" ")) : Collections.emptyList());
    }
}