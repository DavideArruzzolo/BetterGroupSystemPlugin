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
import dzve.model.DiplomacyStatus;
import dzve.service.admin.AdminService;

import javax.annotation.Nonnull;

/**
 * Admin command to forcefully set diplomacy between two groups.
 * Usage: /faction admin setdiplomacy <group1> <group2> <status>
 */
public class AdminSetDiplomacyCommand extends AbstractPlayerCommand {

    private final AdminService adminService;

    @Nonnull
    private final RequiredArg<String> group1Name = withRequiredArg("group1", "Name of the first group",
            ArgTypes.STRING);
    @Nonnull
    private final RequiredArg<String> group2Name = withRequiredArg("group2", "Name of the second group",
            ArgTypes.STRING);
    @Nonnull
    private final RequiredArg<String> status = withRequiredArg("status", "Diplomacy status (ALLY, NEUTRAL, ENEMY)",
            ArgTypes.STRING);

    public AdminSetDiplomacyCommand(AdminService adminService) {
        super("setdiplomacy", "Forcefully set diplomacy between two groups");
        this.adminService = adminService;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        try {
            DiplomacyStatus diplomacyStatus = DiplomacyStatus.valueOf(status.get(ctx).toUpperCase());
            adminService.adminSetDiplomacy(player, group1Name.get(ctx), group2Name.get(ctx), diplomacyStatus);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Message.raw("Invalid status. Use: ALLY, NEUTRAL, ENEMY"));
        }
    }
}
