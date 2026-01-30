package dzve.service.economy;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import dzve.model.Group;
import dzve.model.GroupMember;
import dzve.model.Permission;
import dzve.service.group.GroupService;
import dzve.utils.LogService;

public class EconomyService {
    // private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final GroupService groupService;

    public EconomyService(GroupService groupService) {
        this.groupService = groupService;
    }

    public void withdraw(PlayerRef sender, double amount) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null)
            return;

        if (!groupService.hasPerm(group, sender, Permission.CAN_MANAGE_BANK)) {
            return;
        }

        if (amount <= 0) {
            groupService.notify(sender, "Amount must be positive.");
            return;
        }

        double groupBankBalance = group.getBankBalance();
        if (groupBankBalance < amount) {
            groupService.notify(sender,
                    "Insufficient group bank funds. Group has " + groupBankBalance + " but need " + amount);
            return;
        }

        group.withdrawFromGroup(amount, sender.getUuid());

        // Transfer to player balance (Symmetric to depositToGroup)
        group.deposit(amount, sender.getUuid());
        LogService.info("ECONOMY", "Player " + sender.getUsername() + " withdrew " + amount + " from group "
                + group.getName() + " to personal balance");

        groupService.persistUpdateGroup(group);
        groupService.persistUpdateMember(group.getId(), group.getMember(sender.getUuid()));

        groupService.notify(sender, "Withdrawn " + amount + " to your personal balance.", false);
    }

    public void withdrawFromGroup(PlayerRef sender, double amount) {
        withdraw(sender, amount);
    }

    public void deposit(PlayerRef sender, double amount) {
        if (amount <= 0) {
            groupService.notify(sender, "Amount must be positive.");
            return;
        }

        Group group = groupService.getGroupOrNotify(sender);
        if (group == null)
            return;

        group.deposit(amount, sender.getUuid());
        if (group instanceof dzve.model.Guild) {
            GroupMember member = group.getMember(sender.getUuid());
            if (member != null) {
                member.setContribution(member.getContribution() + amount);
            }
        }
        LogService.info("ECONOMY", "Player " + sender.getUsername() + " deposited " + amount
                + " to personal balance in group " + group.getName());

        // Assuming this affects Member balance inside Group
        groupService.persistUpdateMember(group.getId(), group.getMember(sender.getUuid()));
        if (group instanceof dzve.model.Guild) {
            groupService.persistUpdateGroup(group); // Guild contributions updated
        }

        groupService.notify(sender, "Deposited " + amount, false);
    }

    public void depositToGroup(PlayerRef sender, double amount) {
        if (amount <= 0) {
            groupService.notify(sender, "Amount must be positive.");
            return;
        }

        Group group = groupService.getGroupOrNotify(sender);
        if (group == null)
            return;

        double playerBalance = group.getBalance(sender.getUuid());
        if (playerBalance < amount) {
            groupService.notify(sender,
                    "Insufficient personal balance. You have " + playerBalance + " but need " + amount);
            return;
        }

        group.withdraw(amount, sender.getUuid());
        group.depositToGroup(amount);
        if (group instanceof dzve.model.Guild) {
            GroupMember member = group.getMember(sender.getUuid());
            if (member != null) {
                member.setContribution(member.getContribution() + amount);
            }
        }
        LogService.info("ECONOMY",
                "Player " + sender.getUsername() + " deposited " + amount + " to group bank " + group.getName());

        groupService.persistUpdateGroup(group);
        groupService.persistUpdateMember(group.getId(), group.getMember(sender.getUuid()));

        groupService.notify(sender, "Deposited " + amount + " to group bank.", false);
    }

    public void getBalance(PlayerRef sender, String type) {
        Group group = groupService.getGroupOrNotify(sender);
        if (group == null)
            return;

        if (type == null || type.equalsIgnoreCase("player")) {
            groupService.notify(sender, "Player balance: " + group.getBalance(sender.getUuid()), false);
        } else if (type.equalsIgnoreCase("group")) {
            groupService.notify(sender, "Group Bank Balance: " + group.getBankBalance(), false);
        } else {
            groupService.notify(sender, "Invalid type. Use 'player' or 'group'.");
        }
    }
}
