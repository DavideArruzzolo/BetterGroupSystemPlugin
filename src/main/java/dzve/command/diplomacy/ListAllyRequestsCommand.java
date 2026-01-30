package dzve.command.diplomacy;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.service.group.GroupService;

import javax.annotation.Nonnull;

/**
 * Command to list pending ally requests.
 * Usage: /faction allyrequests
 */
public class ListAllyRequestsCommand extends AbstractPlayerCommand {

    private final GroupService groupService;

    public ListAllyRequestsCommand(GroupService groupService) {
        super("allyRequests", "List pending alliance requests");
        this.groupService = groupService;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        groupService.getDiplomacyService().listAllyRequests(player);
    }
}
