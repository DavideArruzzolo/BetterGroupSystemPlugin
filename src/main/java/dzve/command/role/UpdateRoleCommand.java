package dzve.command.role;

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

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;

public class UpdateRoleCommand extends AbstractPlayerCommand {
    private final GroupService groupService;
    @Nonnull
    private final RequiredArg<String> name = withRequiredArg("name", "Role name", ArgTypes.STRING);
    @Nonnull
    private final OptionalArg<String> addGrants = withOptionalArg("add_grants", "Permissions to add (space separated)",
            ArgTypes.STRING);
    @Nonnull
    private final OptionalArg<String> removeGrants = withOptionalArg("remove_grants",
            "Permissions to remove (space separated)", ArgTypes.STRING);

    public UpdateRoleCommand(GroupService groupService) {
        super("updateRole", "Update a role's permissions");
        this.groupService = groupService;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        String addGrantsStr = addGrants.get(ctx);
        String removeGrantsStr = removeGrants.get(ctx);
        groupService.updateRole(player, name.get(ctx),
                addGrantsStr != null ? Arrays.asList(addGrantsStr.split("[,\\s]+")) : Collections.emptyList(),
                removeGrantsStr != null ? Arrays.asList(removeGrantsStr.split("[,\\s]+")) : Collections.emptyList());
    }
}