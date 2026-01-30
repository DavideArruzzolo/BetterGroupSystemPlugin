package dzve.database.dao;

import com.hypixel.hytale.logger.HytaleLogger;
import dzve.database.DatabaseManager;
import dzve.model.*;

import java.sql.*;
import java.util.*;

public class GroupDao {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final DatabaseManager dbManager;

    public GroupDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // --- Loading ---

    // --- Loading ---

    public Map<UUID, Group> loadAllGroups() {
        Map<UUID, Group> groups = new HashMap<>();
        String sqlGroups = "SELECT * FROM groups";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlGroups);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                try {
                    UUID id = UUID.fromString(rs.getString("id"));
                    String name = rs.getString("name");
                    String tag = rs.getString("tag");
                    String desc = rs.getString("description");
                    String color = rs.getString("color");
                    long createdAtEpoch = rs.getLong("created_at");
                    UUID leaderId = UUID.fromString(rs.getString("leader_id"));
                    String typeStr = rs.getString("type");

                    Group group;

                    // Construct placeholder objects to populate
                    // Note: We are bypassing the constructor that requires PlayerRef
                    if ("GUILD".equals(typeStr)) {
                        group = new Guild(name, tag, desc, color, null);
                        if (group instanceof Guild guild) {
                            guild.setLevel(rs.getInt("level"));
                            // guild.setMoneyToNextLevel(rs.getDouble("money_to_next_level")); // method
                            // must exist
                        }
                    } else {
                        group = new Faction(name, tag, desc, color, null);
                        if (group instanceof Faction faction) {
                            faction.setKills(rs.getInt("kills"));
                            faction.setDeaths(rs.getInt("deaths"));
                        }
                    }

                    // Force ID
                    try {
                        java.lang.reflect.Field idField = dzve.model.Group.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(group, id);
                    } catch (Exception e) {
                        LOGGER.atSevere().log("Could not force set Group ID");
                    }

                    // Force LeaderID (constructor might have failed if player null, or set it to
                    // null)
                    group.setLeaderId(leaderId);

                    group.setBankBalance(rs.getDouble("bank_balance"));
                    if (createdAtEpoch > 0) {
                        group.setCreatedAt(new Timestamp(createdAtEpoch).toLocalDateTime());
                    }

                    groups.put(id, group);

                } catch (Exception e) {
                    LOGGER.atSevere().withCause(e).log("Failed to load a group row");
                }
            }
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to load groups");
        }

        loadMembers(groups);
        loadRoles(groups);
        loadHomes(groups);
        loadDiplomacy(groups);
        loadClaims(groups); // Populate Group.claims

        return groups;
    }

    private void loadClaims(Map<UUID, Group> groups) {
        String sql = "SELECT * FROM claims";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                UUID groupId = UUID.fromString(rs.getString("group_id"));
                Group group = groups.get(groupId);
                if (group == null)
                    continue;

                GroupClaimedChunk claim = new GroupClaimedChunk(
                        rs.getInt("chunk_x"),
                        rs.getInt("chunk_z"),
                        rs.getString("world_name"));
                group.addClaim(claim);
            }
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to load claims into groups");
        }
    }

    private void loadRoles(Map<UUID, Group> groups) {
        String sql = "SELECT * FROM roles";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UUID groupId = UUID.fromString(rs.getString("group_id"));
                Group group = groups.get(groupId);
                if (group == null)
                    continue;

                UUID roleId = UUID.fromString(rs.getString("role_id"));
                String name = rs.getString("name");
                int priority = rs.getInt("priority");
                String permsJson = rs.getString("permissions_json");

                Set<Permission> perms = new HashSet<>();
                if (permsJson != null && !permsJson.isEmpty()) {
                    // Simple parsing for comma-separated list [A, B, C]
                    String clean = permsJson.replace("[", "").replace("]", "").replace("\"", "");
                    for (String s : clean.split(",")) {
                        if (s.trim().isEmpty())
                            continue;
                        try {
                            perms.add(Permission.valueOf(s.trim()));
                        } catch (IllegalArgumentException e) {
                            // ignore invalid perm
                        }
                    }
                }

                GroupRole role = new GroupRole(name, name, priority, false, perms);
                // Need to force set ID
                try {
                    java.lang.reflect.Field f = GroupRole.class.getDeclaredField("id");
                    f.setAccessible(true);
                    f.set(role, roleId);
                } catch (Exception e) {
                }

                group.getRoles().add(role);
            }
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to load roles");
        }
    }

    private void loadHomes(Map<UUID, Group> groups) {
        String sql = "SELECT * FROM homes";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                UUID groupId = UUID.fromString(rs.getString("group_id"));
                Group group = groups.get(groupId);
                if (group == null)
                    continue;

                GroupHome home = new GroupHome(
                        rs.getString("name"),
                        rs.getString("world_name"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        (float) rs.getDouble("yaw"),
                        (float) rs.getDouble("pitch"));
                group.getHomes().add(home);
            }
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to load homes");
        }
    }

    private void loadDiplomacy(Map<UUID, Group> groups) {
        String sql = "SELECT * FROM diplomacy";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                UUID g1 = UUID.fromString(rs.getString("group_id_1"));
                UUID g2 = UUID.fromString(rs.getString("group_id_2"));
                String statusStr = rs.getString("status");

                Group group1 = groups.get(g1);
                // Group 2 might not be loaded if we are sharded, but here we load all.

                if (group1 != null) {
                    group1.getDiplomaticRelations().put(g2, DiplomacyStatus.valueOf(statusStr));
                }
            }
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to load diplomacy");
        }
    }

    private void loadMembers(Map<UUID, Group> groups) {
        String sql = "SELECT * FROM members";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                try {
                    UUID groupId = UUID.fromString(rs.getString("group_id"));
                    Group group = groups.get(groupId);
                    if (group == null)
                        continue;

                    UUID playerId = UUID.fromString(rs.getString("player_id"));
                    String playerName = rs.getString("player_name");
                    String roleIdStr = rs.getString("role_id");
                    UUID roleId = roleIdStr != null ? UUID.fromString(roleIdStr) : null;

                    GroupMember member = new GroupMember(playerId, playerName, roleId);

                    long lastVal = rs.getLong("last_online");
                    if (lastVal > 0)
                        member.setLastActive(new Timestamp(lastVal).toLocalDateTime());

                    long joinVal = rs.getLong("joined_at");
                    if (joinVal > 0)
                        member.setJoinDate(new Timestamp(joinVal).toLocalDateTime());

                    String homeIdStr = rs.getString("default_home_id");
                    if (homeIdStr != null)
                        member.setDefaultHome(UUID.fromString(homeIdStr));

                    double power = rs.getDouble("player_power");
                    double contribution = rs.getDouble("contribution");
                    member.setPower(power);
                    member.setContribution(contribution);

                    // Add to group's member set directly
                    group.getMembers().add(member);

                } catch (Exception e) {
                    LOGGER.atSevere().withCause(e).log("Failed to load member row");
                }
            }
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to load members");
        }
    }

    public Map<String, UUID> loadAllClaims() {
        Map<String, UUID> claims = new HashMap<>();
        String sql = "SELECT world_name, chunk_x, chunk_z, group_id FROM claims";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String world = rs.getString("world_name");
                int x = rs.getInt("chunk_x");
                int z = rs.getInt("chunk_z");
                String groupId = rs.getString("group_id");

                String key = world + ":" + x + ":" + z;
                claims.put(key, UUID.fromString(groupId));
            }
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to load claims");
        }
        return claims;
    }

    // --- Groups ---

    public void createGroup(Group group) {
        String sql = "INSERT INTO groups (id, name, tag, description, color, created_at, leader_id, type, level, money_to_next_level, bank_balance, kills, deaths, homes_json) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, group.getId().toString());
            stmt.setString(2, group.getName());
            stmt.setString(3, group.getTag());
            stmt.setString(4, group.getDescription());
            stmt.setString(5, group.getColor());
            stmt.setString(5, group.getColor());
            stmt.setLong(6, java.sql.Timestamp.valueOf(group.getCreatedAt()).getTime());
            stmt.setString(7, group.getLeaderId().toString());
            stmt.setString(8, group.getType().name());

            if (group instanceof Guild guild) {
                stmt.setInt(9, guild.getLevel());
                stmt.setDouble(10, guild.getMoneyToNextLevel());
            } else {
                stmt.setObject(9, null);
                stmt.setObject(10, null);
            }

            stmt.setDouble(11, group.getBankBalance());

            if (group instanceof Faction faction) {
                stmt.setInt(12, faction.getKills());
                stmt.setInt(13, faction.getDeaths());
            } else {
                stmt.setInt(12, 0);
                stmt.setInt(13, 0);
            }
            stmt.setString(14, "[]"); // Empty homes initially

            stmt.executeUpdate();

            // Add leader as member
            addMember(group.getId(), group.getMember(group.getLeaderId()));

            // Persist default roles
            for (GroupRole role : group.getRoles()) {
                createRole(group.getId(), role);
            }

        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to create group " + group.getName());
        }
    }

    public void updateGroup(Group group) {
        String sql = "UPDATE groups SET name=?, tag=?, description=?, color=?, leader_id=?, level=?, money_to_next_level=?, bank_balance=?, kills=?, deaths=? WHERE id=?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, group.getName());
            stmt.setString(2, group.getTag());
            stmt.setString(3, group.getDescription());
            stmt.setString(4, group.getColor());
            stmt.setString(5, group.getLeaderId().toString());

            if (group instanceof Guild guild) {
                stmt.setInt(6, guild.getLevel());
                stmt.setDouble(7, guild.getMoneyToNextLevel());
            } else {
                stmt.setObject(6, null);
                stmt.setObject(7, null);
            }

            stmt.setDouble(8, group.getBankBalance());
            if (group instanceof Faction faction) {
                stmt.setInt(9, faction.getKills());
                stmt.setInt(10, faction.getDeaths());
            } else {
                stmt.setInt(9, 0);
                stmt.setInt(10, 0);
            }

            stmt.setString(11, group.getId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to update group " + group.getName());
        }
    }

    public void deleteGroup(UUID groupId) {
        String sql = "DELETE FROM groups WHERE id=?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, groupId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to delete group " + groupId);
        }
    }

    // --- Members ---

    public void addMember(UUID groupId, GroupMember member) {
        String sql = "INSERT INTO members (player_id, group_id, player_name, role_id, joined_at, last_online, default_home_id, player_power, contribution) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, member.getPlayerId().toString());
            stmt.setString(2, groupId.toString());
            stmt.setString(3, member.getPlayerName());
            stmt.setString(4, member.getRoleId() != null ? member.getRoleId().toString() : null);
            stmt.setLong(5, java.sql.Timestamp.valueOf(member.getJoinDate()).getTime());
            stmt.setLong(6, java.sql.Timestamp.valueOf(member.getLastActive()).getTime());
            stmt.setString(7, member.getDefaultHome() != null ? member.getDefaultHome().toString() : null);
            stmt.setDouble(8, member.getPower());
            stmt.setDouble(9, member.getContribution());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to add member " + member.getPlayerName());
        }
    }

    public void removeMember(UUID groupId, UUID playerId) {
        String sql = "DELETE FROM members WHERE group_id=? AND player_id=?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, groupId.toString());
            stmt.setString(2, playerId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to remove member " + playerId);
        }
    }

    public void updateMember(UUID groupId, GroupMember member) {
        String sql = "UPDATE members SET role_id=?, last_online=?, default_home_id=?, player_power=?, contribution=? WHERE group_id=? AND player_id=?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, member.getRoleId() != null ? member.getRoleId().toString() : null);
            stmt.setLong(2, java.sql.Timestamp.valueOf(member.getLastActive()).getTime());
            stmt.setString(3, member.getDefaultHome() != null ? member.getDefaultHome().toString() : null);
            stmt.setDouble(4, member.getPower());
            stmt.setDouble(5, member.getContribution());
            stmt.setString(6, groupId.toString());
            stmt.setString(7, member.getPlayerId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to update member " + member.getPlayerName());
        }
    }

    // --- Claims ---

    public void addClaim(UUID groupId, String world, int x, int z) {
        String sql = "INSERT INTO claims (world_name, chunk_x, chunk_z, group_id, claimed_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, world);
            stmt.setInt(2, x);
            stmt.setInt(3, z);
            stmt.setString(4, groupId.toString());
            stmt.setLong(5, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to add claim at " + x + "," + z);
        }
    }

    public void removeClaim(String world, int x, int z) {
        String sql = "DELETE FROM claims WHERE world_name=? AND chunk_x=? AND chunk_z=?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, world);
            stmt.setInt(2, x);
            stmt.setInt(3, z);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to remove claim at " + x + "," + z);
        }
    }

    // --- Roles ---

    public void createRole(UUID groupId, GroupRole role) {
        String sql = "INSERT INTO roles (role_id, group_id, name, priority, permissions_json) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role.getId().toString());
            stmt.setString(2, groupId.toString());
            stmt.setString(3, role.getName());
            stmt.setInt(4, role.getPriority());
            stmt.setString(5, role.getPermissions().toString()); // Set.toString() gives [A, B]
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to create role " + role.getName());
        }
    }

    public void updateRole(UUID groupId, GroupRole role) {
        String sql = "UPDATE roles SET name=?, priority=?, permissions_json=? WHERE role_id=?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role.getName());
            stmt.setInt(2, role.getPriority());
            stmt.setString(3, role.getPermissions().toString());
            stmt.setString(4, role.getId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to update role " + role.getName());
        }
    }

    public void deleteRole(UUID roleId) {
        String sql = "DELETE FROM roles WHERE role_id=?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roleId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to delete role " + roleId);
        }
    }

    // --- Homes ---

    public void saveHome(UUID groupId, GroupHome home) {
        // Upsert style or just delete and insert (simplest for composite keys
        // sometimes, but REPLACE INTO is SQLite specific)
        // Let's use INSERT OR REPLACE
        String sql = "INSERT OR REPLACE INTO homes (home_id, group_id, name, world_name, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, home.getId().toString());
            stmt.setString(2, groupId.toString());
            stmt.setString(3, home.getName());
            stmt.setString(4, home.getWorld());
            stmt.setDouble(5, home.getX());
            stmt.setDouble(6, home.getY());
            stmt.setDouble(7, home.getZ());
            stmt.setDouble(8, home.getYaw());
            stmt.setDouble(9, home.getPitch());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to save home " + home.getName());
        }
    }

    public void deleteHome(UUID groupId, String homeName) {
        String sql = "DELETE FROM homes WHERE group_id=? AND name=?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, groupId.toString());
            stmt.setString(2, homeName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to delete home " + homeName);
        }
    }

    // --- Diplomacy ---

    public void setDiplomacy(UUID g1, UUID g2, DiplomacyStatus status) {
        String sql = "INSERT OR REPLACE INTO diplomacy (group_id_1, group_id_2, status) VALUES (?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, g1.toString());
            stmt.setString(2, g2.toString());
            stmt.setString(3, status.name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Failed to set diplomacy");
        }
    }
}
