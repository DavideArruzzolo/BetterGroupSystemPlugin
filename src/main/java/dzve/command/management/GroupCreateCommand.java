package dzve.command.management;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dzve.service.group.GroupServiceFactory;
import dzve.service.group.management.GroupManagementService;
import dzve.utils.ChatFormatter;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.Objects;
import java.util.UUID;

public class GroupCreateCommand extends AbstractPlayerCommand {

    private final GroupManagementService managementService;

    private final RequiredArg<String> groupName = withRequiredArg("name", "The name of the group to create", ArgTypes.STRING);
    private final RequiredArg<String> groupTag = withRequiredArg("tag", "The tag of the group to create", ArgTypes.STRING);

    public GroupCreateCommand(GroupServiceFactory serviceFactory) {
        super("create", "Creates a new group");
        this.managementService = Objects.requireNonNull(serviceFactory.getManagementService(), "Management service cannot be null");
        setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        final String name = groupName.get(context);
        final String tag = groupTag.get(context);
        final UUID leaderId = playerRef.getUuid();

        GroupManagementService.GroupCreationRequest request = new GroupManagementService.GroupCreationRequest(name, tag, leaderId);
        GroupManagementService.GroupCreationResult result = managementService.createGroup(request);

        if (result instanceof GroupManagementService.GroupCreationSuccess success) {
            playerRef.sendMessage(
                    ChatFormatter.of("✓ Group '")
                            .withColor(Color.GREEN)
                            .append(success.group().getName())
                            .append("' created successfully!")
                            .toMessage()
            );
        } else if (result instanceof GroupManagementService.GroupCreationFailure failure) {
            playerRef.sendMessage(
                    ChatFormatter.of("✗ Error creating group: ")
                            .withColor(Color.RED)
                            .append(failure.reason())
                            .toMessage()
            );
        }
    }
}
