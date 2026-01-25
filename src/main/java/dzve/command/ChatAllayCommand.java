package dzve.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
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
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        String[] args = commandContext.getInputString().trim().split("\\s+");
        groupService.sendAllyMessage(playerRef, args);
    }
}
