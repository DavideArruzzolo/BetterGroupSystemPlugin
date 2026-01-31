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

/**
 * Admin command to grant admin permission to a player.
 * Usage: /faction admin grantperm <playerName>
 */
public class AdminGrantPermCommand extends AbstractPlayerCommand {

    private final AdminService adminService;

    @Nonnull
    private final RequiredArg<String> playerName = withRequiredArg("player", "Name of the player", ArgTypes.STRING);

    public AdminGrantPermCommand(AdminService adminService) {
        super("grantperm", "Grant admin permission to a player");
        this.adminService = adminService;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        adminService.adminGrantPermission(player, playerName.get(ctx));
    }
}
