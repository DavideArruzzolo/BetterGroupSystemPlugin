# Project Status Recap: BetterGroupSystemPlugin

## 1. Introduction and General Overview

This document provides a complete and detailed recap of the current state of the "BetterGroupSystemPlugin" project. The
plugin is a server-side module for Hytale, developed in Java 25, designed to manage Factions (PvP/Power) and Guilds (
Progression/Economy).

The architecture is based on a Singleton design pattern for the main managers, with data persistence handled through a
JSON-based file system. The user interface supports both text commands and a configurable GUI system.

### Key Features:

- **Group Management**: Create, update, and manage groups (Factions/Guilds).
- **Member Management**: Invite, kick, promote, and demote members.
- **Territory System**: Claim chunks, manage homes, and view a territory map.
- **Economy**: Group banks, automatic taxation, and level upgrades.
- **Diplomacy**: Manage alliances and rivalries between groups.
- **Roles and Permissions**: A granular system for defining custom roles with specific permissions.

## 2. Current State of Advancement

### Completed:

- **Core Architecture**: The main components, including the `GroupService`, `JsonStorage`, and `NotificationService`,
  are defined.
- **Data Models**: The data structures for `Group`, `GroupMember`, `GroupRole`, etc., are specified.
- **Command Structure**: A comprehensive list of commands for managing groups, members, territories, and the economy has
  been defined.
- **Configuration Parameters**: A detailed list of configuration parameters for `config.yml` has been created.
- **Permissions System**: A complete set of grants for role-based access control is in place.

### In Progress:

- **Implementation of Command Logic**: While the commands are defined, the implementation of the checks and business
  logic is ongoing.
- **Integration with External Systems**: Integration with a real economy service and a combat logging manager is still
  pending.
- **GUI System**: The GUI system is designed but needs to be fully implemented.

### To Be Addressed:

- **Missing Configuration Properties**: Several useful configuration properties are currently hardcoded or missing.
- **Security and Logic Gaps**: There are some critical security and logic checks that need to be implemented (e.g.,
  combat logging).
- **Offline Player Management**: The handling of offline players for commands like `kick` needs to be improved.
- **Testing**: While a test plan is defined, the implementation of automated tests (unit and integration) is still
  pending.

## 3. Thematic Sections

### 3.1. System Architecture and Implemented Components

- **Architecture**: The system follows a layered architecture, with a clear separation between the command layer,
  service layer, and persistence layer.
- **Main Components**:
    - `BetterGroupSystemPlugin`: The main plugin class, responsible for initialization.
    - `GroupService`: The core component that handles all business logic.
    - `JsonStorage`: Manages data persistence to JSON files.
    - `NotificationService`: Handles in-game notifications and logging.
- **Data Persistence**: Data is stored in JSON files, with a write-through caching strategy for performance.

### 3.2. Dependencies and Active Integrations

- **Java 25**: The project is built using Java 25.
- **Gradle**: Used as the build tool.
- **Lombok**: Used to reduce boilerplate code.
- **Jackson**: Used for JSON serialization and deserialization.
- **Hytale Server API**: The plugin depends on the Hytale Server API for server interactions.

### 3.3. Configurations and Defined Parameters

A comprehensive list of configuration parameters is defined in `config.yml`, allowing for extensive customization of the
plugin's behavior. These include settings for plugin mode, allowed worlds, GUI, command prefixes, and detailed
parameters for both the Faction and Guild systems.

### 3.4. Implemented Workflows and Processes

- **Group Creation**: A player can create a group with a unique name and tag.
- **Member Invitation**: Members with the appropriate permissions can invite other players to the group.
- **Territory Claiming**: Groups can claim chunks of the world, with limits based on power (for Factions) or a fixed
  number (for Guilds).
- **Economy**: Groups have a bank where members can deposit and withdraw money.

### 3.5. Developed APIs and Interfaces

- **Command-Line Interface (CLI)**: A full set of commands is available for interacting with the plugin.
- **Internal API**: The `GroupService` exposes a set of methods that form the internal API for managing groups and
  related entities.

### 3.6. Defined Data Models and Schemas

The data models are well-defined, with classes for `Group`, `GroupMember`, `GroupRole`, `GroupHome`, and
`GroupClaimedChunk`. The data is persisted in a versioned JSON format.

### 3.7. Implemented Business Logic

The core business logic for most of the features is defined in the `GroupService`. This includes logic for creating and
managing groups, handling invitations, managing roles and permissions, and processing economic transactions.

### 3.8. Completed vs. Planned Functionality

- **Completed**: The core functionality for group and member management, as well as the basic territory and economy
  systems, are designed.
- **Planned**:
    - Full implementation of the GUI system.
    - Integration with external economy and combat logging systems.
    - Improved handling of offline players.
    - Implementation of automated tests.

## 4. Contradictory or Inconsistent Information

- **`version` in `gradle.properties` vs. `manifest.json`**: The `version` in `gradle.properties` is `0.0.0`, while the
  version in `manifest.json` is `0.0.132`. These should be synchronized.
- **`config.yml` vs. `config.json`**: The documentation sometimes refers to `config.yml` and sometimes to `config.json`.
  The file format and name should be consistent.
- **Command for setting a role**: The documentation mentions `/group promote`/`/group demote` and `/group set_role` for
  changing a member's role. The exact command and its syntax should be clarified.

## 5. Missing or Incomplete Elements in the Documentation

- **`CombatManager`**: The documentation mentions the need for a `CombatManager` to prevent combat logging, but there
  are no details on its implementation or interface.
- **`EconomyService`**: Similarly, the integration with a real economy service is mentioned, but the details of this
  service are not specified.
- **GUI System Implementation**: While the GUI is mentioned as a feature, there is no detailed documentation on its
  implementation, the libraries used, or the structure of the GUI menus.
- **Automated Testing Framework**: The test plan is detailed, but there is no information on the framework or tools to
  be used for automated testing (e.g., JUnit, Mockito).

## 6. Aspects Requiring Clarification or Further Specification

- **Offline Player Handling**: The strategy for handling offline players needs to be detailed. This includes how to
  resolve player UUIDs and how to apply actions like kicking to offline players.
- **Teleport Delay**: The implementation of the `teleportDelay` needs to be specified, including how it interacts with
  the combat logging system.
- **Invite Expiration**: The mechanism for handling invite expiration (e.g., a scheduled task) needs to be designed and
  documented.
- **Error Handling and Rollback**: While mentioned, the specific strategies for handling errors (e.g., I/O errors,
  corrupted JSON) and for performing rollbacks need to be detailed.
- **Database Migration**: The documentation mentions the possibility of migrating to a database in the future. A plan
  for this migration, including schema changes and data migration strategies, would be beneficial.
