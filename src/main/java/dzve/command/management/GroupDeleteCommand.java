package dzve.command.management;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
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

public class GroupDeleteCommand extends AbstractPlayerCommand {

    private final GroupManagementService managementService;

    public GroupDeleteCommand(GroupServiceFactory serviceFactory) {
        super("delete", "Deletes your group");
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

        GroupManagementService.GroupDeletionRequest request = new GroupManagementService.GroupDeletionRequest(group.getId(), playerId);
        GroupManagementService.GroupDeletionResult result = managementService.deleteGroup(request);

        if (result instanceof GroupManagementService.GroupDeletionSuccess success) {
            playerRef.sendMessage(
                    ChatFormatter.of("✓ Group deleted successfully!")
                            .withColor(Color.GREEN)
                            .toMessage()
            );
        } else if (result instanceof GroupManagementService.GroupDeletionFailure failure) {
            playerRef.sendMessage(
                    ChatFormatter.of("✗ Error deleting group: ")
                            .withColor(Color.RED)
                            .append(failure.reason())
                            .toMessage()
            );
        }
    }
}
