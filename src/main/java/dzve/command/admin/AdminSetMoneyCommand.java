package dzve.command.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
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
 * Admin command to set a group's bank balance.
 * Usage: /faction admin setmoney <groupName> <amount>
 */
public class AdminSetMoneyCommand extends AbstractPlayerCommand {

    private final AdminService adminService;

    @Nonnull
    private final RequiredArg<String> groupName = withRequiredArg("group", "Name of the group", ArgTypes.STRING);
    @Nonnull
    private final RequiredArg<Double> amount = withRequiredArg("amount", "New bank balance amount", ArgTypes.DOUBLE);

    public AdminSetMoneyCommand(AdminService adminService) {
        super("setmoney", "Set a group's bank balance");
        this.adminService = adminService;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        try {
            adminService.adminSetMoney(player, groupName.get(ctx), amount.get(ctx));
        } catch (Exception e) {
            player.sendMessage(Message.raw("Invalid amount. Please provide a valid number."));
        }
    }
}
