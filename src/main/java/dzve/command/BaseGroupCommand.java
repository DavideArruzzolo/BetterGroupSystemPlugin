package dzve.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.command.management.CreateGroupCommand;
import dzve.config.BetterGroupSystemPluginConfig;
import dzve.service.group.GroupService;
import dzve.utils.ChatFormatter;

import javax.annotation.Nonnull;

public class BaseGroupCommand extends AbstractPlayerCommand {
    private final BetterGroupSystemPluginConfig betterGroupSystemPluginConfig = BetterGroupSystemPluginConfig.getInstance();
    private final GroupService groupService = GroupService.getInstance();

    public BaseGroupCommand() {
        super(BetterGroupSystemPluginConfig.getInstance().getAllCommandsPrefix(), "Super test command!");
        setPermissionGroup(GameMode.Adventure);

        addSubCommand(new CreateGroupCommand(groupService));
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Message message = ChatFormatter.of(BetterGroupSystemPluginConfig.MOD_NAME + " List of commands:\n").withMonospace().withBold().toMessage();
        playerRef.sendMessage(message);
    }
}