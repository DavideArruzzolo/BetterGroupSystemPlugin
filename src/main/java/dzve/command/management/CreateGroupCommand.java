package dzve.command.management;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.service.group.GroupService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;

public class CreateGroupCommand extends AbstractPlayerCommand {
    final GroupService groupService;
    @Nonnull
    private final RequiredArg<String> name = withRequiredArg("name", "The name of the group", ArgTypes.STRING);
    @Nonnull
    private final RequiredArg<String> tag = withRequiredArg("tag", "The tag of the group", ArgTypes.STRING);
    @Nonnull
    private final OptionalArg<String> color = withOptionalArg("color", "The color of the group (hex format, e.g., #FF0000)", ArgTypes.STRING);
    @Nonnull
    private final OptionalArg<String> description = withOptionalArg("description", "The description of the group", ArgTypes.STRING);

    public CreateGroupCommand(GroupService groupService) {
        super("create", "Create a new group");
        this.groupService = groupService;
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        final String groupName = name.get(commandContext);
        final String groupTag = tag.get(commandContext);
        final String groupColor = color.get(commandContext);
        final String groupDescription = description.get(commandContext);
        groupService.createGroup(playerRef, groupName, groupTag, groupColor, groupDescription);
    }
}