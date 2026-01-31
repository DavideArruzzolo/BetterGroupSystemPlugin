package dzve.command.diplomacy;

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

/**
 * Command to deny an ally request from another group.
 * Usage: /faction denyally <groupName>
 */
public class DenyAllyCommand extends AbstractPlayerCommand {

    private final GroupService groupService;

    @Nonnull
    private final RequiredArg<String> groupName = withRequiredArg("group", "Name of the group to deny alliance from",
            ArgTypes.STRING);

    public DenyAllyCommand(GroupService groupService) {
        super("denyally", "Deny an alliance request from another group");
        this.groupService = groupService;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        groupService.getDiplomacyService().denyAllyRequest(player, groupName.get(ctx));
    }
}
