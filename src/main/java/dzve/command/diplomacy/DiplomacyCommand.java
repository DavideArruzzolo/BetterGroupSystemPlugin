package dzve.command.diplomacy;

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
import dzve.service.group.GroupService;

import javax.annotation.Nonnull;

public class DiplomacyCommand extends AbstractPlayerCommand {
    private final GroupService groupService;
    @Nonnull
    private final RequiredArg<String> targetGroup = withRequiredArg("group", "Target group name", ArgTypes.STRING);
    @Nonnull
    private final RequiredArg<String> status = withRequiredArg("status", "Status (ALLY, NEUTRAL, ENEMY)", ArgTypes.STRING);

    public DiplomacyCommand(GroupService groupService) {
        super("diplomacy", "Set relation with another group");
        this.groupService = groupService;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        try {
            DiplomacyStatus diploStatus = DiplomacyStatus.valueOf(status.get(ctx).toUpperCase());
            groupService.setDiplomacy(player, targetGroup.get(ctx), diploStatus);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Message.raw("Invalid status. Use: ALLY, NEUTRAL, ENEMY"));
        }
    }
}