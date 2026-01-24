package dzve.command.management;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.service.group.GroupService;

import javax.annotation.Nonnull;

public class GroupInfoCommand extends AbstractPlayerCommand {
    private final GroupService groupService;
    @Nonnull
    private final OptionalArg<String> target = withOptionalArg("group", "Target group name", ArgTypes.STRING);

    public GroupInfoCommand(GroupService groupService) {
        super("info", "View group information");
        this.groupService = groupService;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        groupService.getGroupInfo(player, target.get(ctx));
    }
}