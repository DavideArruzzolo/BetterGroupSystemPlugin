# Plugin Command Reference and Validation Checks (Definitive, Fully Audited)

This document provides a definitive, source-verified list of all user commands, their arguments, and all validation
checks, based on a full audit of the Hytale framework, service, and data model layers.

**Key Implementation Details**:

- **Dual Banking System**: The plugin features two distinct bank accounts: a **shared Group Bank** and a **Personal
  Member Bank** tied to each member. Commands can target one or both.
- **Framework Type-Checking**: The Hytale command framework (`ArgTypes`) performs initial validation (e.g., ensuring a
  numeric argument is a valid number). This is noted under "Framework Checks".
- **Service-Layer Validation**: All subsequent business logic and validation is centralized in `GroupService.java`.
- **Model-Layer Validation**: Some models (e.g., `Guild.java`) contain hardcoded validation logic.
- **Leader Bypass**: A group leader automatically passes all permission checks (`hasPerm`).
- **Input Normalization**: All `String` inputs for names and tags are automatically normalized (spaces become
  underscores).

---

## I. Management Commands

### `/group create <name> <tag> [color] [desc]`

- **Arguments**: `<name>` (String), `<tag>` (String), `[color]` (String), `[desc]` (String)
- **Framework Checks**: None.
- **Service Checks**:
    1. **Player State**: The sender must **not** already be in a group.
    2. **Argument Validation**: Name/Tag uniqueness, format (`^[\\p{L}\\p{N}_]+$`), and length; Color is valid hex;
       Description length.

### `/group info [name]`

- **Arguments**: `[name]` (Optional, String)
- **Framework Checks**: None.
- **Service Checks**:
    1. **Group Existence**: Checks if the group exists.
    2. **Player State**: If `[name]` is omitted, checks if the player is in a group.
    3. **Information Hiding**: Non-members are not shown sensitive data (Claims, Homes, Bank Balance).

### `/group update <type> <value>`

- **Arguments**: `<type>` (String), `<value>` (String)
- **Framework Checks**: None.
- **Service Checks**:
    1. **Player State**: Must be in a group.
    2. **Permissions**: Must be leader or have `CAN_UPDATE_GROUP` grant.
    3. **Redundancy**: Fails if `<value>` is identical to the current one.
    4. **Argument Validation**: Same as `/group create` for the corresponding field.

### `/group delete`

- **Arguments**: None.
- **Framework Checks**: None.
- **Service Checks**:
    1. **Player State**: Must be in a group.
    2. **Permissions**: Must be the **leader**.
    3. **No Confirmation**: Action is immediate.

### `/group leave`

- **Arguments**: None.
- **Framework Checks**: None.
- **Service Checks**:
    1. **Player State**: Must be in a group.
    2. **Leadership**: If leader of a group with >1 members, action is blocked.
- **Side Effect (Model Logic)**: Upon leaving, any funds in the member's **Personal Member Bank** are automatically
  transferred to the shared **Group Bank**.

---

## II. Member Commands

### `/group invite <name>`

- **Arguments**: `<name>` (String)
- **Framework Checks**: None.
- **Service Checks**:
    1. **Player State**: Sender must be in a group.
    2. **Permissions**: Must be leader or have `CAN_INVITE` grant.
    3. **Target State**: Target player must **not** be in another group.
    4. **Group Capacity**: Checks against `maxSize` (for Factions) or the dynamic level-based limit (for Guilds).

### `/group accept <name>`

- **Arguments**: `<name>` (String)
- **Framework Checks**: None.
- **Service Checks**:
    1. **Player State**: Must **not** be in a group.
    2. **Invitation**: Must have a pending invitation from the specified group.
    3. **Group Capacity**: Re-checks if the group is full (static `maxSize` check).

### `/group kick <name>`

- **Arguments**: `<name>` (String)
- **Framework Checks**: None.
- **Service Checks**:
    1. **Player State**: Sender must be in a group.
    2. **Permissions**: Must be leader or have `CAN_KICK` grant.
    3. **Target State**: Target must be in the same group and not be the sender.
    4. **Hierarchy**: Sender's role priority must be higher than the target's.
- **Side Effect (Model Logic)**: Upon being kicked, any funds in the member's **Personal Member Bank** are automatically
  transferred to the shared **Group Bank**.

### `/group transfer <name>`

- **Arguments**: `<name>` (String)
- **Framework Checks**: None.
- **Service Checks**:
    1. **Player State**: Sender must be in a group.
    2. **Permissions**: Must be the **leader**.
    3. **Target State**: Target must be a member of the same group.
    4. **No Confirmation**: Action is immediate.

---

## III. Role Commands

### `/group role create <name> [grants]`

- **Arguments**: `<name>` (String), `[grants]` (String)
- **Framework Checks**: None.
- **Service Checks**:
    1. **Permissions**: Must be leader or have `CAN_MANAGE_ROLE` grant.
    2. **Role Limit**: Group must have fewer than 10 roles (hardcoded).
    3. **Uniqueness**: Role `<name>` must not already exist in the group.
    4. **Grants Validation**: `[grants]` string is parsed; all parts must be valid `Permission` names.

### `/group set_role <member> <role>`

- **Arguments**: `<member>` (String), `<role>` (String)
- **Framework Checks**: None.
- **Service Checks**:
    1. **Permissions**: Must be leader or have `CAN_CHANGE_ROLE` grant.
    2. **Target State**: `<member>` must be in the same group.
    3. **Role Existence**: `<role>` must exist in the group.
    4. **Hierarchy**: Sender's priority must be > target's. A non-leader cannot promote to a rank >= their own.

---

## IV. Territory & Home Commands

### `/group sethome <name>`

- **Arguments**: `<name>` (String)
- **Framework Checks**: None.
- **Service Checks**:
    1. **Permissions**: Must be leader or have `CAN_MANAGE_HOME` grant.
    2. **Location**: Must be standing in a claimed chunk.
    3. **Home Limit**: If creating a new home, count must be < `maxHome`. An existing home can be overwritten.

### `/group home [name]`

- **Arguments**: `[name]` (Optional, String)
- **Framework Checks**: None.
- **Service Checks**:
    1. **Permissions**: Must be leader or have `CAN_TELEPORT_HOME` grant.
    2. **Home Existence & Fallback**: If `[name]` is omitted, uses player's default home -> first available group
       home -> fails if none.
    3. **Teleport Delay**: **Hardcoded 5-second delay**.
    4. **Missing Check**: No combat check.

### `/group claim`

- **Arguments**: None.
- **Framework Checks**: None.
- **Service Checks**:
    1. **Permissions**: Must be leader or have `CAN_MANAGE_CLAIM` grant.
    2. **Chunk State**: Chunk must not be claimed by any other group.
    3. **Group Requirements**: Checks Faction power (`claimRatio`) or Guild claim limit (`maxClaimsPerFaction`).
    4. **Missing Check**: No economy check for claim cost.

---

## V. Economy Commands

### `/group deposit <quantity> [type]`

- **Arguments**: `<quantity>` (Required, Double), `[type]` (Optional, String: `player` or `group`)
- **Framework Checks**: The `<quantity>` argument **must** be a valid number.
- **Service Checks**:
    1. **Player State**: Must be in a group.
    2. **Argument Validation**: `<quantity>` must be a positive number.
    3. **Behavior**: Deposits into shared **Group Bank** if `[type]` is `group`, otherwise into sender's **Personal
       Member Bank**.
    4. **Missing Check**: No integration with a real economy service.

### `/group withdraw <quantity> [type]`

- **Arguments**: `<quantity>` (Required, Double), `[type]` (Optional, String: `player` or `group`)
- **Framework Checks**: The `<quantity>` argument **must** be a valid number.
- **Service Checks**:
    1. **Player State**: Must be in a group.
    2. **Argument Validation**: `<quantity>` must be a positive number.
    3. **Behavior & Permissions**:
        - If `group`: Targets **Group Bank**, **requires** `CAN_MANAGE_BANK` grant, checks Group Bank funds.
        - If `player` or omitted: Targets **Personal Member Bank**, **no permission required**, checks Personal Member
          Bank funds.
    4. **Missing Check**: No integration with a real economy service.

### `/group upgrade`

- **Arguments**: None.
- **Framework Checks**: None.
- **Service Checks**:
    1. **Player State**: Must be in a group.
    2. **Permissions**: Must be leader or have `CAN_UPGRADE_GUILD` grant.
    3. **Group Type**: Must be a `GUILD`.
- **Model Checks (`Guild.java`)**:
    1. **Max Level**: Guild `level` must be **less than 10** (hardcoded).
    2. **Economy**: Group Bank balance must be >= the cost for the next level.
