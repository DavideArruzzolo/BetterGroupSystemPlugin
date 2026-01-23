package dzve.service.group;

import dzve.config.BetterGroupSystemPluginConfig;
import dzve.model.Group;
import dzve.model.Guild;
import dzve.model.Permission;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of GroupEconomyService following Java 25 best practices.
 * Uses pattern matching, records, and sealed interfaces for type safety and performance.
 */
public final class GroupEconomyServiceImpl implements GroupEconomyService {

    private final GroupService groupService;
    private final BetterGroupSystemPluginConfig config;

    public GroupEconomyServiceImpl(GroupService groupService, BetterGroupSystemPluginConfig config) {
        this.groupService = groupService;
        this.config = config;
    }

    @Override
    public DepositResult deposit(DepositRequest request) {
        try {
            Group group = groupService.getGroupForPlayer(request.playerId());
            if (group == null) {
                return new DepositFailure("You are not in a group");
            }

            // Check player's balance (mock implementation - integrate with actual economy service)
            Optional<Double> playerBalance = getPlayerBalance(request.playerId());
            if (playerBalance.isEmpty()) {
                return new DepositFailure("Unable to verify player balance");
            }

            if (playerBalance.get() < request.amount()) {
                return new DepositFailure("Insufficient funds");
            }

            // Withdraw from player (mock implementation)
            if (!withdrawFromPlayer(request.playerId(), request.amount())) {
                return new DepositFailure("Failed to withdraw from player account");
            }

            // Deposit to group
            double oldBalance = group.getBankBalance();
            group.deposit(request.amount());
            groupService.updateGroup(group);

            return new DepositSuccess(request.playerId(), request.amount(), group.getBankBalance());

        } catch (Exception e) {
            return new DepositFailure("Failed to deposit: " + e.getMessage());
        }
    }

    @Override
    public WithdrawalResult withdraw(WithdrawalRequest request) {
        try {
            Group group = groupService.getGroupForPlayer(request.playerId());
            if (group == null) {
                return new WithdrawalFailure("You are not in a group");
            }

            // Check permissions
            if (!groupService.hasPermission(request.playerId(), Permission.CAN_MANAGE_BANK)) {
                return new WithdrawalFailure("You don't have permission to manage the group bank");
            }

            // Check group balance
            if (group.getBankBalance() < request.amount()) {
                return new WithdrawalFailure("Insufficient group funds");
            }

            // Withdraw from group
            double oldBalance = group.getBankBalance();
            boolean success = group.withdraw(request.amount());

            if (success) {
                // Deposit to player (mock implementation)
                if (depositToPlayer(request.playerId(), request.amount())) {
                    groupService.updateGroup(group);
                    return new WithdrawalSuccess(request.playerId(), request.amount(), group.getBankBalance());
                } else {
                    // Rollback the group withdrawal if player deposit fails
                    group.deposit(request.amount());
                    return new WithdrawalFailure("Failed to deposit to player account");
                }
            } else {
                return new WithdrawalFailure("Failed to withdraw from group bank");
            }

        } catch (Exception e) {
            return new WithdrawalFailure("Failed to withdraw: " + e.getMessage());
        }
    }

    @Override
    public Optional<Double> getBalance(UUID playerId) {
        Group group = groupService.getGroupForPlayer(playerId);
        if (group == null) {
            return Optional.empty();
        }

        return Optional.of(group.getBankBalance());
    }

    @Override
    public Optional<Double> getPlayerBalance(UUID playerId) {
        // Mock implementation - integrate with actual economy service
        // In a real implementation, you would call the economy service here
        return Optional.of(10000.0); // Mock balance
    }

    @Override
    public UpgradeResult upgradeGroup(UUID playerId) {
        try {
            Group group = groupService.getGroupForPlayer(playerId);
            if (group == null) {
                return new UpgradeFailure("You are not in a group");
            }

            if (!(group instanceof Guild guild)) {
                return new UpgradeFailure("Only guilds can be upgraded");
            }

            // Check permissions
            if (!groupService.hasPermission(playerId, Permission.CAN_UPGRADE_GUILD)) {
                return new UpgradeFailure("You don't have permission to upgrade the group");
            }

            int currentLevel = guild.getLevel();
            int maxLevel = config.getGuildLevels().length;

            if (currentLevel >= maxLevel) {
                return new UpgradeFailure("Group is already at maximum level");
            }

            // Calculate upgrade cost
            double upgradeCost = guild.calculateCostToNextLevel();

            // Check if group has enough funds
            if (guild.getBankBalance() < upgradeCost) {
                return new UpgradeFailure("Insufficient group funds. Required: " + upgradeCost);
            }

            // Perform the upgrade
            if (guild.withdraw(upgradeCost)) {
                guild.levelUp(); // Assuming levelUp method exists in Guild
                groupService.updateGroup(guild);

                return new UpgradeSuccess(guild.getId(), guild.getLevel(), upgradeCost);
            } else {
                return new UpgradeFailure("Failed to process upgrade payment");
            }

        } catch (Exception e) {
            return new UpgradeFailure("Failed to upgrade group: " + e.getMessage());
        }
    }

    @Override
    public Optional<Integer> getGroupLevel(UUID playerId) {
        Group group = groupService.getGroupForPlayer(playerId);
        if (group instanceof Guild guild) {
            return Optional.of(guild.getLevel());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Double> getUpgradeCost(UUID playerId) {
        Group group = groupService.getGroupForPlayer(playerId);
        if (group instanceof Guild guild) {
            if (guild.getLevel() >= config.getGuildLevels().length) {
                return Optional.empty(); // Already max level
            }
            return Optional.of(guild.calculateCostToNextLevel());
        }
        return Optional.empty();
    }

    /**
     * Mock implementation for withdrawing from player.
     * In a real implementation, integrate with your economy service.
     */
    private boolean withdrawFromPlayer(UUID playerId, double amount) {
        // Mock implementation - always succeeds for demo
        return true;
    }

    /**
     * Mock implementation for depositing to player.
     * In a real implementation, integrate with your economy service.
     */
    private boolean depositToPlayer(UUID playerId, double amount) {
        // Mock implementation - always succeeds for demo
        return true;
    }
}
