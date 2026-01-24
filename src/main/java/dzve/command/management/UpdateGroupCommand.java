package dzve.command.management;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.service.group.GroupService;

import javax.annotation.Nonnull;

public class UpdateGroupCommand extends AbstractPlayerCommand {
    private final GroupService groupService;
    @Nonnull
    private final RequiredArg<String> type = withRequiredArg("type", "Setting to update (name, tag, color, desc)", ArgTypes.STRING);
    @Nonnull
    private final RequiredArg<String> value = withRequiredArg("value", "New value", ArgTypes.STRING);

    public UpdateGroupCommand(GroupService groupService) {
        super("update", "Update group settings");
        this.groupService = groupService;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        groupService.updateGroup(player, type.get(ctx), value.get(ctx));
    }
}