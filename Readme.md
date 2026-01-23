# Technical Specification: Hytale Plugin - Factions & Guilds

## 1. Technological Stack & Architecture

- **Language:** Java 25 (utilizing latest JVM features like Project Loom for high-performance async processing).
- **Build Tool & Boilerplate:** Gradle + Lombok.
- **Security:** Proguard (Code Obfuscation).
- **Data Storage:** JSON-based storage logic (Primary) with structured logging for all events.
- **Design Pattern:** Singleton logic for core managers.
- **Interface:** Dual-system support for both text-based **Commands** and a graphical **GUI System**.

---

## 2. Configuration Parameters (config.yml)

Below is the comprehensive list of parameters to control system behavior, limits, and penalties.

### General Settings

- **pluginMode**: Sets the active system (Options: `FACTION`, `GUILD`, or `BOTH`).
- **allowedWorlds**: List of world IDs where claiming and group homes are permitted.
- **enableGui**: Toggle between graphical menus and standard text commands.
- **allCommandsPrefix**: Global prefix for all plugin-related commands.
- **hidePlayers**: Hides external players on the map for strategic depth.
- **toasterDuration**: Duration (in seconds) for on-screen notifications.
- **guiThemeColor**: Primary hex color code for GUI menus.

### Faction System (PvP/Power)

- **maxSize**: Maximum number of members allowed in a faction.
- **minNameLength** / **maxNameLength**: Character limits for faction names.
- **minTagLength** / **maxTagLength**: Character limits for faction tags.
- **disablePowerSystemDetails**: Hides specific power metrics from public view.
- **playerPowerMax** / **playerPowerMin**: Defined range for individual player power.
- **powerGainByKill**: Power gained upon killing an enemy.
- **powerGainByTime**: Passive power gained over time while online.
- **powerLooseByDeath**: Power lost upon player death.
- **powerRegenOffline**: Boolean to enable or disable power regeneration while offline.
- **claimRatio**: The multiplier determining chunk limit based on total power.
- **maxClaimsPerFaction**: An absolute hard cap on claimed chunks, regardless of power.
- **unraidableProtectionTime**: Minutes of protection granted after a faction loses its “raidable” status.

### Guild System (Progression/Economy)

- **guildLevels**: Defined hierarchy of guild levels.
- **levelPriceMultiplier**: Currency cost associated with each level upgrade.
- **slotQuantityForLevel**: Member slots unlocked per guild level.
- **enableTax**: Toggles the automated taxation system.
- **taxImport**: The fixed currency amount for periodic taxes.
- **taxInterval**: Frequency of tax collection (e.g., “24h”, “7d”).
- **maxLatePayment**: Limit of unpaid cycles allowed before penalties trigger.
- **latePayAction**: Penalty type for non-payment (Options: `KICK`, `DEMOTE`, `DISABLE_BUFF`).
- **latePayGracePeriod**: Hours allowed after a deadline before the penalty is applied.

### Territory & Roles

- **maxHome**: Maximum number of homes a group can set.
- **maxClaim**: Maximum chunks allowed (if not limited by the power system).
- **defaultRolesName**: Predefined role names (Leader, Officer, Member, Recruit).
- **defaultGrants**: Initial permissions assigned to default roles.
- **rolePriority**: Numerical hierarchy used to manage permissions.

---

## 3. Data Models (Data Structure)

This section outlines the core data classes required to support the plugin's functionality.

### `Group.java` (Base Class for Faction/Guild)

This class holds all common data for any group type.

- `UUID id`: Unique identifier for the group.
- `GroupType type`: Enum (`FACTION` or `GUILD`).
- `String name`: Unique, case-insensitive name.
- `String tag`: Unique, case-insensitive tag.
- `String description`: Optional text describing the group.
- `String color`: Hex color code for chat and GUI elements.
- `UUID leaderId`: The UUID of the player who is the group leader.
- `int level`: The current level of the group (for `/group upgrade`).
- `double bankBalance`: The amount of money in the group bank.
- `Set<GroupMember> members`: A set of all members in the group.
- `Set<GroupRole> roles`: A set of all available roles (default and custom).
- `Set<GroupHome> homes`: A set of all defined group homes.
- `Set<GroupClaimedChunk> claims`: A set of all claimed land chunks.
- `Map<UUID, DiplomacyStatus> diplomaticRelations`: Maps other group IDs to a diplomatic status (`ALLY`, `NEUTRAL`,
  `ENEMY`).
- `LocalDateTime createdAt`: Timestamp of when the group was created.

### `GroupMember.java`

Represents a player within a group.

- `UUID playerId`: The player's unique ID.
- `UUID roleId`: The ID of the role assigned to this member.
- `LocalDateTime joinDate`: Timestamp of when the player joined.

### `GroupRole.java`

Defines a role and its associated permissions.

- `UUID roleId`: Unique identifier for the role.
- `String roleName`: The name of the role (e.g., "Leader", "Raider").
- `int priority`: A number to determine hierarchy (e.g., Leader=100, Member=10). Used for promote/demote checks.
- `Set<String> grants`: A set of permission strings (e.g., `CAN_INVITE`, `CAN_KICK`).

### Supporting Data Classes

- **`GroupHome.java`**: Contains `String name` and `Location location`.
- **`GroupClaimedChunk.java`**: Contains `int chunkX`, `int chunkZ`, and `String world`.

---

## 4. Command Reference

Every command supports execution via chat or the interactive GUI.

| Category       | Command              | Arguments                     | Description                         |
|----------------|----------------------|-------------------------------|-------------------------------------|
| **Management** | `/group create`      | `<name> <tag> [color] [desc]` | Create a new Faction or Guild.      |
|                | `/group info`        | `[name]`                      | View details of a group.            |
|                | `/group update`      | `[name/tag/color/desc]`       | Modify group information.           |
|                | `/group delete`      | N/A                           | Dissolve the current group.         |
|                | `/group leave`       | N/A                           | Leave the current group.            |
| **Members**    | `/group invite`      | `<name>`                      | Invite a player to the group.       |
|                | `/group invitations` | N/A                           | List all active invitations.        |
|                | `/group accept`      | `<name>`                      | Accept a group invitation.          |
|                | `/group refuse`      | `<name>`                      | Refuse a group invitation.          |
|                | `/group kick`        | `<name>`                      | Remove a member.                    |
|                | `/group transfer`    | `<name>`                      | Transfer group leadership.          |
| **Roles**      | `/group promote`     | `<name> [role]`               | Advance a member’s rank.            |
|                | `/group demote`      | `<name> [role]`               | Lower a member’s rank.              |
|                | `/group role create` | `<name> [grants]`             | Create a new custom role.           |
|                | `/group role update` | `<name> [grants]`             | Edit role permissions.              |
|                | `/group role delete` | `<name>`                      | Remove a custom role.               |
|                | `/group role list`   | N/A                           | View all roles and permissions.     |
| **Territory**  | `/group sethome`     | `<name>`                      | Set a group teleport point.         |
|                | `/group home`        | `[name]`                      | Teleport to a group home.           |
|                | `/group edithome`    | `[name]`                      | Modify an existing home.            |
|                | `/group delhome`     | `<name>`                      | Delete a group home.                |
|                | `/group setdefault`  | `<name>`                      | Set the primary home point.         |
|                | `/group claim`       | N/A                           | Claim the current chunk.            |
|                | `/group unclaim`     | N/A                           | Remove a claim from a chunk.        |
|                | `/group map`         | N/A                           | View the territory map.             |
| **Diplomacy**  | `/group diplomacy`   | `<name> <status>`             | Set Enemy, Ally, or Neutral.        |
|                | `/group diplolist`   | N/A                           | List all diplomatic relations.      |
| **Economy**    | `/group deposit`     | `<quantity>`                  | Add money to the group bank.        |
|                | `/group withdraw`    | `<quantity>`                  | Take money from the group bank.     |
|                | `/group balance`     | `[type]`                      | View player or group funds.         |
|                | `/group upgrade`     | N/A                           | Purchase a guild level upgrade.     |
| **Social/UI**  | `/group gui`         | N/A                           | Open the graphical interface.       |
|                | `/group chat`        | `<type>`                      | Switch chat (Global/Internal/Ally). |

---

## 5. Permissions (Grants)

These are the permission strings stored in the `grants` set within each `GroupRole`.

- `CAN_MANAGE_GROUP_INFO`: Allows use of `/group update`.
- `CAN_INVITE`: Allows use of `/group invite`.
- `CAN_KICK`: Allows use of `/group kick`.
- `CAN_PROMOTE_DEMOTE`: Allows use of `/group promote` and `/group demote`.
- `CAN_MANAGE_ROLES`: Allows use of `/group role create/update/delete`.
- `CAN_MANAGE_HOMES`: Allows use of `/group sethome/edithome/delhome`.
- `CAN_MANAGE_CLAIMS`: Allows use of `/group claim/unclaim`.
- `CAN_MANAGE_DIPLOMACY`: Allows use of `/group diplomacy`.
- `CAN_MANAGE_BANK`: Allows use of `/group withdraw`.
- `CAN_INTERACT_IN_CLAIM`: Allows interaction (breaking/placing blocks) in claimed territory.

---

## 6. Extra Features & Admin Tools

- **Notification System:** Toaster pop-ups for individual, group, and global alerts. This includes border alerts when
  entering enemy or allied territory.
- **Logging:** Complete tracking system for every command execution, transaction, and death event.
- **Admin Management:** Comprehensive staff tools for bypassing claims, deleting any group, or editing group data.
