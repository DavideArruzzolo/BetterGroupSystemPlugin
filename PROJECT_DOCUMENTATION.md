# BetterGroupSystemPlugin - Comprehensive Project Documentation

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture & Design](#architecture--design)
3. [Core Components](#core-components)
4. [Configuration System](#configuration-system)
5. [Group Types & Features](#group-types--features)
6. [Command System](#command-system)
7. [Data Management](#data-management)
8. [Security & Permissions](#security--permissions)
9. [Territory & Claim System](#territory--claim-system)
10. [Economy Integration](#economy-integration)
11. [Chat & Communication](#chat--communication)
12. [Build & Deployment](#build--deployment)
13. [Technical Specifications](#technical-specifications)
14. [Development Guidelines](#development-guidelines)

---

## Project Overview

### Plugin Information

- **Name**: BetterGroupSystemPlugin
- **Group**: Dzve
- **Version**: 0.0.211
- **Main Class**: `dzve.BetterGroupSystemPlugin`
- **Target Platform**: Hytale Server
- **Development Team**: DZVE Development Team
- **License**: Proprietary

### Purpose & Scope

The BetterGroupSystemPlugin is a comprehensive group management solution for Hytale servers that provides two distinct
modes:

- **FACTION Mode**: PvP-focused faction system with power mechanics and territory control
- **GUILD Mode**: Cooperative guild system with leveling and economic progression

### Key Features

- Dual-mode operation (Factions/Guilds)
- Advanced permission system with hierarchical roles
- Territory claiming and protection mechanics
- Integrated economy with group banks
- Power-based PvP system (Factions only)
- Guild leveling system (Guilds only)
- Multi-home teleportation system
- Diplomatic relations between groups
- Real-time chat formatting and group chat
- Comprehensive configuration management

---

## Architecture & Design

### Design Patterns

- **Singleton Pattern**: `GroupService` for centralized group management
- **Builder Pattern**: Configuration and model creation
- **Strategy Pattern**: Different behavior for Faction vs Guild types
- **Observer Pattern**: Event-driven architecture for player interactions
- **Factory Pattern**: Command creation and registration

### Package Structure

```
dzve/
├── BetterGroupSystemPlugin.java     # Main plugin class
├── command/                         # Command implementations
│   ├── BaseGroupCommand.java        # Root command handler
│   ├── ChatAllayCommand.java        # Chat management
│   ├── ChatGroupCommand.java        # Group chat
│   ├── ReloadCommand.java           # Configuration reload
│   ├── diplomacy/                   # Diplomacy commands
│   ├── economy/                     # Economy commands
│   ├── management/                  # Group management
│   ├── member/                      # Member management
│   ├── role/                        # Role management
│   └── territory/                   # Territory commands
├── config/
│   └── BetterGroupSystemPluginConfig.java  # Configuration model
├── listener/                        # Event listeners
│   ├── ChatListener.java            # Chat event handling
│   └── MapPlayerListener.java       # Player event handling
├── model/                          # Data models
│   ├── Group.java                   # Abstract base class
│   ├── Faction.java                 # Faction implementation
│   ├── Guild.java                   # Guild implementation
│   ├── GroupMember.java             # Member data
│   ├── GroupRole.java               # Role system
│   ├── Permission.java              # Permission enumeration
│   └── [Additional models]
├── service/
│   └── group/
│       └── GroupService.java        # Core business logic
└── systems/                        # Game systems
    ├── claim/                       # Claim protection systems
    └── [Additional systems]
```

### Component Interaction Flow

```
Player Command → Command Handler → GroupService → Model Updates → Storage
     ↓                    ↓                ↓              ↓
Event Listener → Validation → Business Logic → Data Persistence → Notification
```

---

## Core Components

### 1. Main Plugin Class (`BetterGroupSystemPlugin`)

**Responsibilities:**

- Plugin lifecycle management (setup, start, shutdown)
- Command registration
- Event listener registration
- Configuration management
- System initialization

**Key Methods:**

- `setup()`: Initialize core systems and register commands
- `start()`: Register event listeners and start background systems
- `shutdown()`: Clean shutdown and data persistence

### 2. Group Service (`GroupService`)

**Responsibilities:**

- Centralized group management (Singleton)
- Business logic implementation
- Data persistence coordination
- Permission validation
- Player-to-group mapping

**Key Features:**

- Thread-safe operations using `ConcurrentHashMap`
- Asynchronous data persistence
- Comprehensive validation system
- Real-time notification system

### 3. Configuration System (`BetterGroupSystemPluginConfig`)

**Responsibilities:**

- Plugin behavior configuration
- Game balance parameters
- Feature toggles
- World restrictions

---

## Configuration System

### Configuration Categories

#### 1. Basic Plugin Settings

```json
{
  "PluginMode": "FACTION",
  // FACTION or GUILD
  "AllowedWorlds": "*",
  // Comma-separated world names or "*"
  "AllCommandsPrefix": "faction",
  // Command prefix
  "HidePlayers": true
  // Hide players from other groups
}
```

#### 2. Group Size & Naming

```json
{
  "MaxSize": 10,
  // Maximum members per group
  "MinNameLength": 3,
  // Minimum group name length
  "MaxNameLength": 15,
  // Maximum group name length
  "MinTagLength": 2,
  // Minimum tag length
  "MaxTagLength": 5,
  // Maximum tag length
  "MaxDescriptionLength": 255
  // Maximum description length
}
```

#### 3. Power System (Factions Only)

```json
{
  "DisablePowerSystemDetails": true,
  "PlayerInitialPower": 5.0,
  // Starting power per player
  "PlayerPowerMax": 100.0,
  // Maximum power per player
  "PlayerPowerMin": -100.0,
  // Minimum power per player
  "PowerGainByKill": 1.0,
  // Power gained per kill
  "PowerGainByTime": 0.001,
  // Power gained over time
  "PowerLooseByDeath": 1.0,
  // Power lost per death
  "PowerRegenOffline": 0.0001
  // Offline power regeneration
}
```

#### 4. Territory System

```json
{
  "ClaimRatio": 0.5,
  // Power-to-claim ratio
  "MaxClaimsPerFaction": 100,
  // Maximum claims per faction
  "MaxHome": 5
  // Maximum homes per group
}
```

#### 5. Guild System (Guilds Only)

```json
{
  "GuildLevels": [
    1,
    2,
    3,
    4,
    5
  ],
  // Available guild levels
  "InitialPrice": 2500.0,
  // Cost for first level
  "LevelPriceMultiplier": 1.2,
  // Price increase per level
  "SlotQuantityGainForLevel": 10
  // Additional slots per level
}
```

#### 6. Economy System

```json
{
  "EnableTax": false,
  // Enable group taxation
  "TaxImport": 0.0,
  // Tax rate (percentage)
  "TaxInterval": 86400.0,
  // Tax collection interval (seconds)
  "MaxLatePayment": 5,
  // Maximum late payments
  "LatePayAction": "NONE",
  // Action for late payment
  "LatePayGracePeriod": 28800.0
  // Grace period before penalties
}
```

#### 7. Chat System

```json
{
  "ChatMessageMaxLength": 40,
  // Maximum chat message length
  "PvpEnabled": true
  // Enable PvP features
}
```

---

## Group Types & Features

### Faction System

#### Core Features

- **Power-Based Mechanics**: Players have individual power that contributes to faction power
- **Territory Control**: Claim land based on faction power
- **PvP Focus**: Raidable status when power is low
- **K/D Tracking**: Kill/death statistics and ratios
- **Raidable Mechanics**: Factions become vulnerable when power < claims

#### Power Calculation

```
Total Faction Power = Σ Individual Player Powers
Max Claims = Total Faction Power × ClaimRatio
Raidable Status = Total Power < Claims Count
```

#### Power Dynamics

- **Gain**: Kills (1.0), Time (0.001/sec), Offline regeneration (0.0001/sec)
- **Loss**: Deaths (1.0)
- **Limits**: Min (-100), Max (100)
- **Starting**: 5.0 power per player

### Guild System

#### Core Features

- **Leveling System**: Progressive guild levels (1-5)
- **Economic Focus**: Bank management and upgrades
- **Cooperative Gameplay**: No PvP mechanics
- **Slot Expansion**: More members as guild levels up
- **Upgrade Costs**: Exponential cost increase

#### Level Progression

```
Level 1: Base size (10 members)
Level 2: +10 members (20 total)
Level 3: +10 members (30 total)
Level 4: +10 members (40 total)
Level 5: +10 members (50 total)
```

#### Upgrade Costs

```
Cost = InitialPrice × (LevelPriceMultiplier ^ CurrentLevel)
Level 1→2: 2500.0
Level 2→3: 3000.0
Level 3→4: 3600.0
Level 4→5: 4320.0
```

---

## Command System

### Command Hierarchy

```
/faction (or /guild based on mode)
├── create <name> <tag> [color] [description]
├── disband
├── leave
├── info [groupname]
├── update <type> <value>
│   ├── name <newname>
│   ├── tag <newtag>
│   ├── color <hexcolor>
│   └── desc <newdescription>
├── invite <player>
├── accept <groupname>
├── kick <player>
├── transfer <player>
├── role
│   ├── create <name> [permissions...]
│   ├── delete <name>
│   ├── list
│   ├── update <name> [addpermissions...] [removepermissions...]
│   └── set <player> <role>
├── claim
├── unclaim
├── home
│   ├── set [name]
│   ├── delete <name>
│   ├── list
│   ├── setdefault <name>
│   └── [name] (teleport)
├── balance
├── deposit <amount>
├── withdraw <amount>
├── upgrade (guilds only)
├── diplomacy
│   └── list
├── chat <message>
├── ally <message>
└── reload
```

### Permission System

#### Permission Hierarchy

1. **CAN_INVITE**: Invite new members
2. **CAN_KICK**: Remove members (with hierarchy checks)
3. **CAN_UPDATE_GROUP**: Modify group properties
4. **CAN_MANAGE_ROLE**: Create/delete/modify roles
5. **CAN_CHANGE_ROLE**: Assign roles to others
6. **CAN_MANAGE_HOME**: Create/delete group homes
7. **CAN_TELEPORT_HOME**: Use group teleportation
8. **CAN_UPGRADE_GUILD**: Upgrade guild level
9. **CAN_CLAIM**: Claim territory
10. **CAN_UNCLAIM**: Unclaim territory

#### Role System

- **Default Role**: Automatically assigned to new members
- **Priority System**: Higher priority roles can modify lower priority roles
- **Permission Inheritance**: Roles can have multiple permissions
- **Custom Roles**: Up to 10 custom roles per group

---

## Data Management

### Storage Architecture

- **Format**: JSON serialization using Jackson
- **Location**: `mods/Dzve_BetterGroupSystemPlugin/data/groups.json`
- **Strategy**: Asynchronous persistence with immediate memory updates
- **Backup**: Automatic saving on all data modifications

### Data Models

#### Group Data Structure

```json
{
  "type": "FACTION|GUILD",
  "id": "UUID",
  "name": "string",
  "tag": "string",
  "description": "string|null",
  "color": "#RRGGBB",
  "leaderId": "UUID",
  "createdAt": "ISO-8601 datetime",
  "bankBalance": "double",
  "homes": [
    "GroupHome objects"
  ],
  "claims": [
    "GroupClaimedChunk objects"
  ],
  "members": [
    "GroupMember objects"
  ],
  "roles": [
    "GroupRole objects"
  ],
  "diplomaticRelations": {
    "UUID": "DiplomacyStatus"
  }
}
```

#### Faction-Specific Data

```json
{
  "totalPower": "double",
  "playerPower": {
    "UUID": "double"
  },
  "kills": "int",
  "deaths": "int",
  "raidable": "boolean",
  "lastRaidNotificationTimestamp": "long"
}
```

#### Guild-Specific Data

```json
{
  "level": "int",
  "moneyToNextLevel": "double"
}
```

### Caching Strategy

- **Memory-First**: All operations work on in-memory data
- **Lazy Loading**: Data loaded on plugin startup
- **Validation Caches**: Name/tag uniqueness caches
- **Player Mapping**: Quick UUID-to-group lookups

---

## Security & Permissions

### Access Control

- **Hierarchical Permissions**: Role-based access control
- **Ownership Checks**: Leaders have all permissions
- **Modification Restrictions**: Cannot modify higher-ranked members
- **Command Validation**: Comprehensive permission checks per command

### Security Measures

- **Input Validation**: All user inputs validated and sanitized
- **SQL Injection Prevention**: No direct database queries
- **XSS Protection**: Chat message formatting and length limits
- **Resource Limits**: Maximum roles, homes, claims per group

### Validation Rules

- **Name/Tag Validation**: Alphanumeric characters only, length limits
- **Color Validation**: Hex color format (#RRGGBB)
- **Permission Validation**: Enum-based permission system
- **Hierarchy Validation**: Role priority enforcement

---

## Territory & Claim System

### Claim Mechanics

- **Chunk-Based**: 16x16 block chunks
- **Power Requirements**: Factions need sufficient power for claims
- **World Restrictions**: Configurable allowed worlds
- **Overlap Prevention**: No duplicate claims allowed

### Protection Systems

- **Block Protection**: Prevent unauthorized block placement/breaking
- **Interaction Protection**: Control container access and device usage
- **PvP Protection**: Configurable PvP in claimed territory
- **Alert System**: Notifications for claim interactions

### Home System

- **Multiple Homes**: Up to configured maximum (default: 5)
- **Claim Requirement**: Homes must be in claimed territory
- **Teleportation**: 5-second delay with history tracking
- **Default Home**: Per-member default home setting

---

## Economy Integration

### Group Banking

- **Shared Balance**: Group-wide bank account
- **Transaction Logging**: Track deposits/withdrawals
- **Permission Control**: Role-based access to funds
- **Tax System**: Optional automatic taxation

### Economic Features

- **Guild Upgrades**: Level-based costs
- **Territory Costs**: Power-based claim requirements
- **Member Contributions**: Individual banking permissions
- **Financial Management**: Withdraw/deposit commands

### Currency Flow

```
Player → Group Bank → Guild Upgrades/Operations
Group Bank ← Player Deposits/Taxes
```

---

## Chat & Communication

### Chat Features

- **Group Chat**: Dedicated chat channels for groups
- **Ally Chat**: Cross-group communication for allied groups
- **Formatting**: Color-coded messages with group tags
- **Length Limits**: Configurable message length restrictions

### Message Formatting

- **Group Colors**: Custom hex colors for group identity
- **Rank Display**: Role-based chat formatting
- **World Filtering**: Per-world chat restrictions
- **Anti-Spam**: Message length and rate limiting

---

## Build & Deployment

### Build System

- **Gradle**: Modern build automation
- **Java Toolchain**: Configurable Java version
- **Hytale Integration**: Automatic Hytale installation detection
- **Asset Pack**: Included resource pack support

### Build Configuration

```gradle
plugins {
    id 'java'
    id 'org.jetbrains.gradle.plugin.idea-ext' version '1.3'
}

dependencies {
    implementation("org.projectlombok:lombok:1.18.42")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.0")
    // Hytale Server JAR (auto-detected)
}
```

### Deployment Process

1. **Build**: `./gradlew build`
2. **Version Management**: Automatic version increment
3. **Asset Packaging**: Included asset pack compilation
4. **Server Integration**: Automatic mod deployment to Hytale

---

## Technical Specifications

### Performance Characteristics

- **Memory Usage**: Efficient concurrent data structures
- **I/O Operations**: Asynchronous JSON persistence
- **Thread Safety**: ConcurrentHashMap usage throughout
- **Scalability**: Designed for large server populations

### Dependencies

- **Hytale Server API**: Core server integration
- **Lombok**: Code generation and boilerplate reduction
- **Jackson**: JSON serialization/deserialization
- **Java 17+**: Modern Java features and performance

### Resource Requirements

- **Memory**: Base plugin ~50MB, scales with group count
- **Storage**: JSON data file, typically <10MB for 1000+ groups
- **CPU**: Minimal overhead, event-driven architecture
- **Network**: No external dependencies

---

## Development Guidelines

### Code Standards

- **Java Conventions**: Standard Java naming and structure
- **Lombok Usage**: Reduce boilerplate with annotations
- **Documentation**: Comprehensive JavaDoc coverage
- **Error Handling**: Graceful failure with user feedback

### Testing Strategy

- **Unit Tests**: Core business logic validation
- **Integration Tests**: Command and event system testing
- **Load Testing**: Performance under high concurrency
- **Regression Testing**: Feature compatibility verification

### Maintenance Considerations

- **Configuration Migration**: Backward compatibility for config changes
- **Data Migration**: Safe JSON schema evolution
- **Version Management**: Semantic versioning with changelog
- **Monitoring**: Built-in logging and error reporting

### Extension Points

- **Command System**: Easy addition of new commands
- **Permission System**: Extensible permission framework
- **Event System**: Plugin integration through event listeners
- **Configuration**: Flexible configuration model for new features

---

## Conclusion

The BetterGroupSystemPlugin represents a comprehensive, production-ready solution for group management in Hytale
servers. Its dual-mode architecture supports both competitive faction gameplay and cooperative guild experiences, with
extensive customization options through its robust configuration system.

The plugin's modular design, efficient data management, and comprehensive feature set make it suitable for servers of
all sizes, from small private communities to large public networks. The emphasis on security, performance, and
maintainability ensures long-term reliability and extensibility.

**Key Strengths:**

- Versatile dual-mode system (Factions/Guilds)
- Comprehensive permission and role system
- Efficient territory claiming and protection
- Integrated economy and banking features
- Robust configuration and customization options
- Production-ready architecture with proper error handling

**Future Enhancement Opportunities:**

- Web dashboard for group management
- Advanced diplomacy system (alliances, wars)
- Custom achievement system
- Integration with popular economy plugins
- Advanced analytics and reporting

This documentation serves as a comprehensive reference for developers, server administrators, and contributors to the
BetterGroupSystemPlugin project.

---

## Hardcoded Values Analysis & Configuration Suggestions

### Identified Hardcoded Values

After analyzing the codebase, several hardcoded values have been identified that could benefit from configuration
options. These values control various aspects of plugin behavior and should be made configurable for better flexibility.

### Suggested New Configuration Properties

#### 1. Teleportation System

```json
{
  "TeleportDelaySeconds": 5,
  // Current: hardcoded 5 seconds
  "TeleportTitleFadeIn": 0.5,
  // Current: hardcoded 0.5f
  "TeleportTitleStay": 2.0,
  // Current: hardcoded 2.0f  
  "TeleportTitleFadeOut": 0.5
  // Current: hardcoded 0.5f
}
```

**Location**: `GroupService.java:541` and `ClaimAlertSystem.java:59`
**Impact**: Controls home teleportation delay and title display timing

#### 2. Role System Limits

```json
{
  "MaxRolesPerGroup": 10,
  // Current: hardcoded 10
  "DefaultRolePriority": 0,
  // Current: hardcoded 0 (Recruit)
  "MemberRolePriority": 50,
  // Current: hardcoded 50 (Member)
  "OfficerRolePriority": 100,
  // Current: hardcoded 100 (Officer)
  "CustomRoleDefaultPriority": 10
  // Current: hardcoded 10 (new roles)
}
```

**Locations**:

- `GroupService.java:373` (max roles check)
- `GroupService.java:279` (member role priority)
- `GroupService.java:389` (custom role priority)
- `GroupRole.java:33,37,43` (default role priorities)

#### 3. Claim Alert System

```json
{
  "ClaimAlertTitleColor": "#FFD700",
  // Current: hardcoded Color(255, 215, 0) - Gold
  "ClaimAlertEnabled": true,
  // Current: always enabled
  "ClaimAlertShowModName": true
  // Current: hardcoded MOD_NAME display
}
```

**Location**: `ClaimAlertSystem.java:53`
**Impact**: Controls the appearance of claim entry titles

#### 4. Raid Notification System

```json
{
  "RaidNotificationCooldownMinutes": 5,
  // Current: hardcoded 5 minutes
  "RaidNotificationsEnabled": true
  // Current: always enabled
}
```

**Location**: `ClaimProtectionSystems.java:27`
**Impact**: Controls cooldown between raid notifications

#### 5. Storage System

```json
{
  "StorageShutdownTimeoutSeconds": 5,
  // Current: hardcoded 5 seconds
  "StorageAsyncSaveEnabled": true
  // Current: always enabled
}
```

**Location**: `JsonStorage.java:138`
**Impact**: Controls graceful shutdown timeout for storage operations

#### 6. Chat Formatting Colors

```json
{
  "ChatColorBlack": "#000000",
  // Current: hardcoded Color(0, 0, 0)
  "ChatColorDarkBlue": "#0000AA",
  // Current: hardcoded Color(0, 0, 170)
  "ChatColorDarkGreen": "#00AA00",
  // Current: hardcoded Color(0, 170, 0)
  "ChatColorDarkAqua": "#00AAAA",
  // Current: hardcoded Color(0, 170, 170)
  "ChatColorDarkRed": "#AA0000",
  // Current: hardcoded Color(170, 0, 0)
  "ChatColorDarkPurple": "#AA00AA",
  // Current: hardcoded Color(170, 0, 170)
  "ChatColorGold": "#FFAA00",
  // Current: hardcoded Color(255, 170, 0)
  "ChatColorGray": "#AAAAAA",
  // Current: hardcoded Color(170, 170, 170)
  "ChatColorDarkGray": "#555555",
  // Current: hardcoded Color(85, 85, 85)
  "ChatColorBlue": "#5555FF",
  // Current: hardcoded Color(85, 85, 255)
  "ChatColorGreen": "#55FF55",
  // Current: hardcoded Color(85, 255, 85)
  "ChatColorAqua": "#55FFFF",
  // Current: hardcoded Color(85, 255, 255)
  "ChatColorRed": "#FF5555",
  // Current: hardcoded Color(255, 85, 85)
  "ChatColorLightPurple": "#FF55FF",
  // Current: hardcoded Color(255, 85, 255)
  "ChatColorYellow": "#FFFF55",
  // Current: hardcoded Color(255, 255, 85)
  "ChatColorWhite": "#FFFFFF"
  // Current: hardcoded Color(255, 255, 255)
}
```

**Location**: `ChatFormatter.java:18-33`
**Impact**: Allows customization of chat color palette

#### 7. Chunk Calculation

```json
{
  "ChunkSizeBits": 5
  // Current: hardcoded 5 (for 32-block chunks)
}
```

**Location**: Multiple files use `>> 5` for chunk calculations
**Impact**: Controls chunk size calculation (currently 32x32 blocks)
**Note**: This should be changed with caution as it affects the entire claim system

#### 8. Power System Defaults

```json
{
  "DefaultPlayerPower": 0.0,
  // Current: hardcoded 0.0 for new players
  "PowerCalculationPrecision": 2
  // Current: implicit from double operations
}
```

**Location**: `Faction.java:70,75,83`
**Impact**: Controls default power values for new faction members

### Implementation Priority

#### High Priority (Recommended for immediate implementation)

1. **Teleportation System** - Frequently used by players
2. **Role System Limits** - Affects group management flexibility
3. **Raid Notification Cooldown** - Important for gameplay balance

#### Medium Priority (Nice to have)

1. **Claim Alert System** - Visual customization
2. **Storage System** - Operational flexibility
3. **Chat Formatting Colors** - Aesthetic customization

#### Low Priority (Advanced/Expert)

1. **Chunk Size** - Requires extensive testing
2. **Power System Defaults** - Core gameplay mechanics

### Configuration Migration Strategy

When implementing these new configuration properties:

1. **Backward Compatibility**: Ensure existing configurations continue to work
2. **Default Values**: Use current hardcoded values as defaults
3. **Validation**: Add appropriate validation for new properties
4. **Documentation**: Update configuration documentation
5. **Testing**: Thoroughly test with various configuration combinations

### Code Changes Required

#### Example: Teleportation Configuration

```java
// In BetterGroupSystemPluginConfig.java
private final int teleportDelaySeconds = 5;
private final float teleportTitleFadeIn = 0.5f;
private final float teleportTitleStay = 2.0f;
private final float teleportTitleFadeOut = 0.5f;

// In GroupService.java
notify(sender, "Teleporting to "+home.getName() +" in "+

getConfig().

getTeleportDelaySeconds() +"sec...",false);

        HytaleServer.SCHEDULED_EXECUTOR.

schedule(() ->world.

execute(() ->{
        // teleport logic
        }),

getConfig().

getTeleportDelaySeconds(),TimeUnit.SECONDS);
```

#### Example: Role System Configuration

```java
// In BetterGroupSystemPluginConfig.java
private final int maxRolesPerGroup = 10;
private final int defaultRolePriority = 0;
private final int memberRolePriority = 50;
private final int officerRolePriority = 100;
private final int customRoleDefaultPriority = 10;

// In GroupService.java
if(group.

getRoles().

size() >=

getConfig().

getMaxRolesPerGroup()){

notify(sender, "Max roles reached.");
    return;
            }

modifyRoles(group, roles ->roles.

add(new GroupRole(name, name,
    getConfig().

getCustomRoleDefaultPriority(), false,perms)));
```

### Benefits of Configuration Changes

1. **Flexibility**: Server administrators can fine-tune gameplay
2. **Balance**: Easy adjustment of game mechanics
3. **Customization**: Tailor plugin behavior to server needs
4. **Testing**: Easy experimentation with different values
5. **Maintenance**: Reduce need for code changes for adjustments

### Risk Assessment

- **Low Risk**: Visual and timing configurations
- **Medium Risk**: Gameplay-affecting configurations
- **High Risk**: Core mechanic configurations (chunk size, power system)

All new configuration properties should include:

- Proper validation
- Sensible defaults
- Clear documentation
- Migration path for existing configurations
