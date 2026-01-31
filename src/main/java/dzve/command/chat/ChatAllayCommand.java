package dzve.command.chat;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.service.group.GroupService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ChatAllayCommand extends AbstractPlayerCommand {
    private final GroupService groupService;

    public ChatAllayCommand(GroupService groupService) {
        super("chatAllay", "Chat with group or ally");
        this.groupService = groupService;
        this.setAllowsExtraArguments(true);
        setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        dzve.utils.LogService.debug("COMMAND", "Player " + playerRef.getUsername() + " executed /chatAllay");
        String[] args = commandContext.getInputString().trim().split("\\s+");
        groupService.sendAllyMessage(playerRef, args);
    }
}
