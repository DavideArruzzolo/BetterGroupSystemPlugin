package dzve.command.management;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.service.group.GroupServiceFactory;
import dzve.service.group.management.GroupManagementService;
import dzve.utils.ChatFormatter;
import dzve.model.Group;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.Objects;
import java.util.UUID;

public class GroupUpdateCommand extends AbstractPlayerCommand {

    private final GroupManagementService managementService;

    private final OptionalArg<String> groupName = withOptionalArg("name", "The new name of the group", ArgTypes.STRING);
    private final OptionalArg<String> groupTag = withOptionalArg("tag", "The new tag of the group", ArgTypes.STRING);
    private final OptionalArg<String> groupDescription = withOptionalArg("description", "The new description of the group", ArgTypes.STRING);

    public GroupUpdateCommand(GroupServiceFactory serviceFactory) {
        super("update", "Updates your group's settings");
        this.managementService = Objects.requireNonNull(serviceFactory.getManagementService(), "Management service cannot be null");
        setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        final UUID playerId = playerRef.getUuid();
        Group group = managementService.getGroupForPlayer(playerId);
        if (group == null) {
            playerRef.sendMessage(ChatFormatter.of("You are not in a group.").withColor(Color.RED).toMessage());
            return;
        }

        final String name = groupName.get(context);
        final String tag = groupTag.get(context);
        final String description = groupDescription.get(context);

        GroupManagementService.GroupUpdateRequest request = new GroupManagementService.GroupUpdateRequest(group.getId(), name, tag, description);
        GroupManagementService.GroupUpdateResult result = managementService.updateGroup(request);

        if (result instanceof GroupManagementService.GroupUpdateSuccess success) {
            playerRef.sendMessage(
                    ChatFormatter.of("✓ Group '")
                            .withColor(Color.GREEN)
                            .append(success.updatedGroup().getName())
                            .append("' updated successfully!")
                            .toMessage()
            );
        } else if (result instanceof GroupManagementService.GroupUpdateFailure failure) {
            playerRef.sendMessage(
                    ChatFormatter.of("✗ Error updating group: ")
                            .withColor(Color.RED)
                            .append(failure.reason())
                            .toMessage()
            );
        }
    }
}
