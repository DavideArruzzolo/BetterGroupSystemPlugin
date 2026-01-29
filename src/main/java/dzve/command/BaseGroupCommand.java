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
import dzve.command.diplomacy.ListDiplomacyCommand;
import dzve.command.economy.DepositCommand;
import dzve.command.economy.GetBalanceCommand;
import dzve.command.economy.GetPowerCommand;
import dzve.command.economy.WithdrawCommand;
import dzve.command.management.*;
import dzve.command.member.*;
import dzve.command.role.*;
import dzve.command.territory.*;
import dzve.config.BetterGroupSystemPluginConfig;
import dzve.service.group.GroupService;
import dzve.utils.ChatFormatter;

import javax.annotation.Nonnull;

public class BaseGroupCommand extends AbstractPlayerCommand {

    public BaseGroupCommand(BetterGroupSystemPluginConfig betterGroupSystemPluginConfig) {
        super(betterGroupSystemPluginConfig.getAllCommandsPrefix(), "Main command for group system");
        setPermissionGroup(GameMode.Adventure);

        GroupService groupService = GroupService.getInstance();
        addSubCommand(new CreateGroupCommand(groupService));
        addSubCommand(new UpdateGroupCommand(groupService));
        addSubCommand(new DisbandGroupCommand(groupService));
        addSubCommand(new LeaveGroupCommand(groupService));
        addSubCommand(new UpgradeGuildCommand(groupService));
        addSubCommand(new ReloadCommand(groupService));

        addSubCommand(new InvitePlayerCommand(groupService));
        addSubCommand(new AcceptInvitationCommand(groupService));
        addSubCommand(new KickMemberCommand(groupService));
        addSubCommand(new TransferLeadershipCommand(groupService));

        addSubCommand(new CreateRoleCommand(groupService));
        addSubCommand(new SetRoleCommand(groupService));
        addSubCommand(new DeleteRoleCommand(groupService));
        addSubCommand(new UpdateRoleCommand(groupService));
        addSubCommand(new ListRolesCommand(groupService));

        addSubCommand(new ClaimChunkCommand(groupService));
        addSubCommand(new ClaimMapCommand(groupService));
        addSubCommand(new UnclaimChunkCommand(groupService));
        addSubCommand(new SetHomeCommand(groupService));
        addSubCommand(new HomeCommand(groupService));
        addSubCommand(new DeleteHomeCommand(groupService));
        addSubCommand(new SetDefaultHomeCommand(groupService));
        addSubCommand(new ListHomesCommand(groupService));

        addSubCommand(new DepositCommand(groupService));
        addSubCommand(new WithdrawCommand(groupService));
        addSubCommand(new GetBalanceCommand(groupService));
        addSubCommand(new GetPowerCommand(groupService));
        addSubCommand(new GroupInfoCommand(groupService));

        addSubCommand(new DiplomacyCommand(groupService));
        addSubCommand(new ListDiplomacyCommand(groupService));
        addSubCommand(new ListInvitationsCommand(groupService));
        addSubCommand(new ListMembersCommand(groupService));
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Message message = ChatFormatter.of(BetterGroupSystemPluginConfig.MOD_NAME + " Available commands:").toMessage();
        playerRef.sendMessage(message);
        getSubCommands().forEach((name, cmd) -> playerRef
                .sendMessage(ChatFormatter.of(" - " + name + ": " + cmd.getDescription()).toMessage()));
    }
}
