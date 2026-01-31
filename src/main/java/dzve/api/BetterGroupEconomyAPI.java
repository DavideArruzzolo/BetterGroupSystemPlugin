package dzve.api;

import dzve.model.Group;
import dzve.model.GroupMember;
import dzve.service.group.GroupService;
import dzve.utils.LogService;

import java.util.UUID;

/**
 * Public API for accessing and modifying Economy data in
 * BetterGroupSystemPlugin.
 * This class provides thread-safe access to Player and Group balances.
 * All modification methods automatically trigger persistence (database save).
 */
public class BetterGroupEconomyAPI {

    private static BetterGroupEconomyAPI instance;
    private final GroupService groupService;

    private BetterGroupEconomyAPI() {
        this.groupService = GroupService.getInstance();
    }

    /**
     * Get the singleton instance of the API.
     *
     * @return BetterGroupEconomyAPI instance
     */
    public static synchronized BetterGroupEconomyAPI getInstance() {
        if (instance == null) {
            instance = new BetterGroupEconomyAPI();
        }
        return instance;
    }

    // ==========================================
    // PLAYER FINANCE
    // ==========================================

    /**
     * Get a player's personal bank balance within their current group.
     *
     * @param playerId UUID of the player
     * @return Balance amount, or 0.0 if not in a group.
     */
    public double getMemberBalance(UUID playerId) {
        Group group = groupService.getPlayerGroup(playerId);
        if (group == null)
            return 0.0;
        return group.getBalance(playerId);
    }

    /**
     * Set a player's personal bank balance.
     *
     * @param playerId UUID of the player
     * @param amount   New balance amount (must be positive)
     * @return true if successful, false if player not in a group or invalid amount.
     */
    public boolean setMemberBalance(UUID playerId, double amount) {
        if (amount < 0)
            return false;
        Group group = groupService.getPlayerGroup(playerId);
        if (group == null)
            return false;

        GroupMember member = group.getMember(playerId);
        if (member == null)
            return false;

        member.setBankBalance(amount);
        groupService.persistUpdateMember(group.getId(), member);
        LogService.info("API", "Set member balance for " + playerId + " to " + amount);
        return true;
    }

    /**
     * Deposit money into a player's personal bank balance.
     *
     * @param playerId UUID of the player
     * @param amount   Amount to add
     * @return true if successful
     */
    public boolean depositMemberBalance(UUID playerId, double amount) {
        if (amount <= 0)
            return false;
        Group group = groupService.getPlayerGroup(playerId);
        if (group == null)
            return false;

        group.deposit(amount, playerId);
        groupService.persistUpdateMember(group.getId(), group.getMember(playerId));
        return true;
    }

    /**
     * Withdraw money from a player's personal bank balance.
     *
     * @param playerId UUID of the player
     * @param amount   Amount to remove
     * @return true if successful (balance sufficient), false otherwise
     */
    public boolean withdrawMemberBalance(UUID playerId, double amount) {
        if (amount <= 0)
            return false;
        Group group = groupService.getPlayerGroup(playerId);
        if (group == null)
            return false;

        GroupMember member = group.getMember(playerId);
        if (member == null || member.getBankBalance() < amount)
            return false;

        group.withdraw(amount, playerId);
        groupService.persistUpdateMember(group.getId(), member);
        return true;
    }

    // ==========================================
    // GROUP FINANCE
    // ==========================================

    /**
     * Get a Group's shared bank balance.
     *
     * @param groupId UUID of the group
     * @return Bank balance, or 0.0 if group not found.
     */
    public double getGroupBalance(UUID groupId) {
        Group group = groupService.getGroup(groupId);
        if (group == null)
            return 0.0;
        return group.getBankBalance();
    }

    /**
     * Set a Group's shared bank balance.
     *
     * @param groupId UUID of the group
     * @param amount  New balance amount
     * @return true if successful
     */
    public boolean setGroupBalance(UUID groupId, double amount) {
        if (amount < 0)
            return false;
        Group group = groupService.getGroup(groupId);
        if (group == null)
            return false;

        group.setBankBalance(amount);
        groupService.persistUpdateGroup(group);
        LogService.info("API", "Set group balance for " + group.getName() + " to " + amount);
        return true;
    }

    /**
     * Deposit money into a Group's shared bank.
     *
     * @param groupId UUID of the group
     * @param amount  Amount to add
     * @return true if successful
     */
    public boolean depositGroupBalance(UUID groupId, double amount) {
        if (amount <= 0)
            return false;
        Group group = groupService.getGroup(groupId);
        if (group == null)
            return false;

        group.depositToGroup(amount);
        groupService.persistUpdateGroup(group);
        return true;
    }

    /**
     * Withdraw money from a Group's shared bank.
     * Note: This bypasses permission checks as it is an API call.
     *
     * @param groupId UUID of the group
     * @param amount  Amount to remove
     * @return true if successful (funds available)
     */
    public boolean withdrawGroupBalance(UUID groupId, double amount) {
        if (amount <= 0)
            return false;
        Group group = groupService.getGroup(groupId);
        if (group == null)
            return false;

        if (group.getBankBalance() < amount)
            return false;

        group.setBankBalance(group.getBankBalance() - amount);
        groupService.persistUpdateGroup(group);
        return true;
    }

    /**
     * Get the Group ID of a player.
     *
     * @param playerId UUID of the player
     * @return Group UUID, or null if not in a group.
     */
    public UUID getPlayerGroup(UUID playerId) {
        Group group = groupService.getPlayerGroup(playerId);
        return (group != null) ? group.getId() : null;
    }
}
