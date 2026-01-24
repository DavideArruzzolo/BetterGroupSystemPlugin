# Group Command Checks

This document provides a detailed list of checks to be performed for each command, based on the provided command table.

---

## 1. General Checks

These apply to most commands and should be checked first.

1. **Sender is Player**: Ensure the command is run by a player, not the console.
2. **Player is in a Group**: Verify the player is a member of a group for commands that operate on an existing group.
3. **Permissions**: Check if the player has the specific permission node required for the command (e.g.,
   `group.invite`).
4. **Role Permissions**: For actions within a group, check if the player's assigned role has the necessary grants (e.g.,
   the "Invite Members" grant).

---

## I. Management Commands

### `/group create <name> <tag> [color] [desc]`

1. **Player State**: Check that the player is **not** already in a group.
2. **Argument Validation**:
    * Ensure `<name>` and `<tag>` are provided.
    * Validate format of `<name>`, `<tag>`, and `[desc]` (length, allowed characters).
    * If `[color]` is provided, ensure it's a valid color code.
    * If `[desc]` is provided, ensure it's a valid not blank.
    * Group name and tag must be unique and contains only alphanumeric characters.
3. **System State**: Check if the proposed `<name>` or `<tag>` are already in use.

### `/group info [name]`

1. **If `[name]` is provided**: Check if a group with that name exists.
2. **If `[name]` is omitted**: Check if the player is in a group.

### `/group update [name/tag/color/desc]`

1. **Player State**: Check if the player is in a group.
2. **Permissions**: Check if the player has the required role/permission to update group settings.
3. **Argument Validation**:
    * If updating `name` or `tag`, check for uniqueness and format.
    * If updating `color`, check for a valid color code.
    * If updating `desc`, check for valid format/length.

### `/group delete`

1. **Player State**: Check if the player is in a group.
2. **Permissions**: Check if the player is the **leader** of the group.
   3**Confirmation**: Implement a confirmation step (e.g., `/group delete confirm`) to prevent accidental deletion.

### `/group leave`

1. **Player State**: Check if the player is in a group.
2. **Leadership Check**:
    * If the player is the leader, check if there are other members.
    * If so, block the action and require them to use `/group transfer <name>` first.
    * If they are the last member, the group should be deleted automatically.

---

## II. Member Commands

### `/group invite <name>`

1. **Player State**: Check if the sender is in a group.
2. **Permissions**: Check if the sender's role allows them to invite members.
3. **Target State**:
    * Check if the target player `<name>` is online.
    * Check if the target player is **not** already in a group.
4. **Group State**: Check if the group has reached its maximum member limit.

### `/group invitations`

1. **Player State**: Check if the player has any pending invitations to view.

### `/group accept <name>`

1. **Player State**:
    * Check that the player is **not** already in a group.
    * Check that the player has a pending invitation from the group `<name>`.
2. **Group State**: Check if the inviting group is still accepting members (i.e., not full).

### `/group kick <name>`

1. **Player State**: Check if the sender is in a group.
2. **Permissions**: Check if the sender's role allows them to kick members.
3. **Target State**:
    * Check if the target `<name>` is a member of the same group.
    * Check that the sender is not trying to kick themselves.
4. **Hierarchy**: Check if the sender's rank is higher than the target's rank. (You can't kick someone of equal or
   higher rank).

### `/group transfer <name>`

1. **Player State**: Check if the sender is in a group.
2. **Permissions**: Check if the sender is the **leader** of the group.
3. **Target State**: Check if the target `<name>` is a member of the same group.
4. **Confirmation**: Implement a confirmation step.

---

## III. Role Commands

### `/group promote <name> [role]` & `/group demote <name> [role]`

1. **Permissions**: Check if the sender has permission to manage roles.
2. **Target State**: Check if the target `<name>` is in the sender's group.
3. **Hierarchy**:
    * Check that the sender's rank is superior to the target's rank.
    * Check that the target is not being promoted to a rank equal to or higher than the sender's.
4. **Role Validation**: If `[role]` is specified, check that it's a valid, existing role.

### `/group role create <name> [grants]`

1. **Permissions**: Check if the sender has permission to create roles.
2. **Argument Validation**: Check that a role with `<name>` doesn't already exist.
3. **Group State**: Check if the group has reached the maximum number of custom roles.
4. **Grants Validation**: If `[grants]` are provided, check that they are valid permission strings.

### `/group role update <name> [grants]`

1. **Permissions**: Check if the sender has permission to edit roles.
2. **Argument Validation**: Check that the role `<name>` exists and is not a protected/default role.
3. **Grants Validation**: Check that `[grants]` are valid permission strings.

### `/group role delete <name>`

1. **Permissions**: Check if the sender has permission to delete roles.
2. **Argument Validation**: Check that the role `<name>` exists and is not a protected/default role.
3. **Dependency Check**: Check if any members are currently assigned to this role. If so, require them to be reassigned
   before deletion.

### `/group role list`

1. **Player State**: Check if the player is in a group.

---

## IV. Territory Commands

### `/group sethome <name>`

1. **Permissions**: Check if the sender's role has permission to set homes.
2. **Location**: Check if the player is standing in a chunk claimed by their group.
3. **Group State**: Check if the group has reached its maximum number of homes.
4. **Argument Validation**: Check if a home with `<name>` already exists.

### `/group home [name]`

1. **Group State**:
    * If `[name]` is provided, check that home exists.
    * If omitted, check if a default home is set.
3. **State**: Check if the player is in combat, which should prevent teleporting.

### `/group edithome [name]`, `/group delhome <name>`, `/group setdefault <name>`

1. **Permissions**: Check if the sender's role has permission to manage homes.
2. **Argument Validation**: Check if the home `[name]` or `<name>` exists.
3. **For `edithome`**: Check if the player is standing in a valid new location (claimed chunk).

### `/group claim` & `/group unclaim`

1. **Permissions**: Check if the sender's role has permission to manage claims.
2. **For `claim`**:
    * Check if the current chunk is already claimed by anyone.
    * Check if the group has enough "power" or has met other requirements to claim.
    * Check if the group has reached its maximum claim limit.
    * **Economy**: If guild check if the group bank has enough money for the claim cost.
3. **For `unclaim`**:
    * Check if the current chunk is claimed by the sender's group.

### `/group map`

---

## V. Diplomacy Commands

### `/group diplomacy <name> <status>`

1. **Permissions**: Check if the sender's role has permission to manage diplomacy.
2. **Argument Validation**:
    * Check if the target group `<name>` exists.
    * Check that the target is not the sender's own group.
    * Check that `<status>` is a valid diplomatic state (Ally, Neutral, Enemy).
3. **Logic**:
    * For **Ally**, this should send a request. The other group must accept.
    * Check if the proposed status is already active to prevent redundancy.

### `/group diplolist`

1. **Player State**: Check if the player is in a group.

---

## VI. Economy Commands

### `/group deposit <quantity>`

1. **Argument Validation**: Check that `<quantity>` is a valid, positive number.
2. **Player Economy**: Check if the player has enough money to deposit.

### `/group withdraw <quantity>`

1. **Permissions**: Check if the sender's role has permission to withdraw from the bank.
2. **Argument Validation**: Check that `<quantity>` is a valid, positive number.
3. **Group Economy**: Check if the group bank has sufficient funds.

### `/group balance [type]`

1. **Permissions**: If viewing the group's balance, check if the player has permission.

### `/group upgrade`

1. **Permissions**: Check if the sender's role has permission to purchase upgrades.
2. **Group State**: Check that the group is not already at the maximum level.
3. **Group Economy**: Check if the group bank has enough money for the upgrade cost.
