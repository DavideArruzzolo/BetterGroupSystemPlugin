package dzve.command.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.service.admin.AdminService;
import dzve.service.group.GroupService;
import dzve.utils.ChatFormatter;

import javax.annotation.Nonnull;

/**
 * Main admin command that contains subcommands for administrative operations.
 * Usage: /faction admin <subcommand>
 */
public class AdminCommand extends AbstractPlayerCommand {

    private final AdminService adminService;

    public AdminCommand(GroupService groupService, AdminService adminService) {
        super("admin", "Admin commands for group management");
        this.adminService = adminService;

        // Register subcommands
        addSubCommand(new AdminDisbandCommand(adminService));
        addSubCommand(new AdminKickCommand(adminService));
        addSubCommand(new AdminSetLeaderCommand(adminService));
        addSubCommand(new AdminSetDiplomacyCommand(adminService));
        addSubCommand(new AdminSetMoneyCommand(adminService));
        addSubCommand(new AdminSetPlayerMoneyCommand(adminService));
        addSubCommand(new AdminGrantPermCommand(adminService));
        addSubCommand(new AdminRevokePermCommand(adminService));
        addSubCommand(new AdminInfoCommand(groupService, adminService));
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        player.sendMessage(ChatFormatter.of("Admin commands:").toMessage());
        getSubCommands().forEach((name, cmd) -> player
                .sendMessage(ChatFormatter.of(" - admin " + name + ": " + cmd.getDescription()).toMessage()));
    }
}
