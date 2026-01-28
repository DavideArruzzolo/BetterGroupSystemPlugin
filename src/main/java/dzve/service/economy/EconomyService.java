package dzve.service.economy;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import dzve.model.Group;
import dzve.model.Permission;
import dzve.service.group.GroupService;

public class EconomyService {

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
        group.deposit(amount, sender.getUuid());

        groupService.saveGroups();
        groupService.notify(sender, "Withdrawn " + amount + ". New balance: " + group.getBalance(sender.getUuid()),
                false);
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
        if (group instanceof dzve.model.Guild guild) {
            guild.getMoneyContributions().merge(sender.getUuid(), amount, Double::sum);
        }

        groupService.saveGroups();
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
        if (group instanceof dzve.model.Guild guild) {
            guild.getMoneyContributions().merge(sender.getUuid(), amount, Double::sum);
        }

        groupService.saveGroups();
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
