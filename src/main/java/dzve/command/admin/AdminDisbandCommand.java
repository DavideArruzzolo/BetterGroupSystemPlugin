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

import javax.annotation.Nonnull;

public class AdminDisbandCommand extends AbstractPlayerCommand {

    private final AdminService adminService;

    @Nonnull
    private final RequiredArg<String> groupName = withRequiredArg("group", "Name of the group to disband",
            ArgTypes.STRING);

    public AdminDisbandCommand(AdminService adminService) {
        super("disband", "Forcefully disband a group");
        this.adminService = adminService;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        dzve.utils.LogService.info("ADMIN_COMMAND",
                "Player " + player.getUsername() + " executed /faction admin disband " + groupName.get(ctx));
        adminService.adminDisband(player, groupName.get(ctx));
    }
}
