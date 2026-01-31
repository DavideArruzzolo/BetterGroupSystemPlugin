package dzve.command.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.service.admin.AdminService;
import dzve.service.group.GroupService;

import javax.annotation.Nonnull;

/**
 * Admin command to get detailed info about a group.
 * Usage: /faction admin info <groupName>
 */
public class AdminInfoCommand extends AbstractPlayerCommand {

    private final GroupService groupService;
    private final AdminService adminService;

    @Nonnull
    private final RequiredArg<String> groupName = withRequiredArg("group", "Name of the group", ArgTypes.STRING);

    public AdminInfoCommand(GroupService groupService, AdminService adminService) {
        super("info", "Get detailed info about a group");
        this.groupService = groupService;
        this.adminService = adminService;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        adminService.adminInfo(player, groupName.get(ctx));
    }
}
