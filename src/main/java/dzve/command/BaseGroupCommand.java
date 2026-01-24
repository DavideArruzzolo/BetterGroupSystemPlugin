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
import dzve.command.diplomacy.DiplomacyCommand;
import dzve.command.economy.DepositCommand;
import dzve.command.economy.WithdrawCommand;
import dzve.command.management.*;
import dzve.command.member.*;
import dzve.command.role.CreateRoleCommand;
import dzve.command.role.DeleteRoleCommand;
import dzve.command.role.SetRoleCommand;
import dzve.command.territory.*;
import dzve.config.BetterGroupSystemPluginConfig;
import dzve.service.group.GroupService;
import dzve.utils.ChatFormatter;

import javax.annotation.Nonnull;

public class BaseGroupCommand extends AbstractPlayerCommand {
    private final BetterGroupSystemPluginConfig betterGroupSystemPluginConfig = BetterGroupSystemPluginConfig.getInstance();

    public BaseGroupCommand() {
        super(BetterGroupSystemPluginConfig.getInstance().getAllCommandsPrefix(), "Main command for group system");
        setPermissionGroup(GameMode.Adventure);

        // --- I. Management ---
        GroupService groupService = GroupService.getInstance();
        addSubCommand(new CreateGroupCommand(groupService));
        addSubCommand(new UpdateGroupCommand(groupService));
        addSubCommand(new DeleteGroupCommand(groupService));
        addSubCommand(new LeaveGroupCommand(groupService));
        addSubCommand(new UpgradeGuildCommand(groupService));

        // --- II. Members ---
        addSubCommand(new InvitePlayerCommand(groupService));
        addSubCommand(new AcceptInvitationCommand(groupService));
        addSubCommand(new KickMemberCommand(groupService));
        addSubCommand(new TransferLeadershipCommand(groupService));

        // --- III. Roles ---
        addSubCommand(new CreateRoleCommand(groupService));
        addSubCommand(new SetRoleCommand(groupService));
        addSubCommand(new DeleteRoleCommand(groupService));
        addSubCommand(new ListRolesCommand(groupService));

        // --- IV. Territory ---
        addSubCommand(new ClaimChunkCommand(groupService));
        addSubCommand(new UnclaimChunkCommand(groupService));
        addSubCommand(new SetHomeCommand(groupService));
        addSubCommand(new HomeCommand(groupService));
        addSubCommand(new DeleteHomeCommand(groupService));

        // --- V. Economy & Misc ---
        addSubCommand(new DepositCommand(groupService));
        addSubCommand(new WithdrawCommand(groupService));
        addSubCommand(new GroupInfoCommand(groupService));
        addSubCommand(new DiplomacyCommand(groupService));
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        // Mostra la lista dei comandi se viene digitato solo il comando base
        Message message = ChatFormatter.of(BetterGroupSystemPluginConfig.MOD_NAME + " Available commands:\n")
                .withMonospace().withBold().toMessage();

        playerRef.sendMessage(message);

        // Qui potresti iterare sui subcommands per generare una help list dinamica
        getSubCommands().forEach((name, cmd) -> playerRef.sendMessage(ChatFormatter.of(" - " + name).toMessage()));
    }
}