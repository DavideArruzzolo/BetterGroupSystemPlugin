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
 * Admin command to forcefully kick a member from a group.
 * Usage: /faction admin kick <groupName> <playerName>
 */
public class AdminKickCommand extends AbstractPlayerCommand {

    private final AdminService adminService;

    @Nonnull
    private final RequiredArg<String> groupName = withRequiredArg("group", "Name of the group", ArgTypes.STRING);
    @Nonnull
    private final RequiredArg<String> playerName = withRequiredArg("player", "Name of the player to kick",
            ArgTypes.STRING);

    public AdminKickCommand(AdminService adminService) {
        super("kick", "Forcefully kick a member from a group");
        this.adminService = adminService;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        adminService.adminKick(player, groupName.get(ctx), playerName.get(ctx));
    }
}
