package dzve.database;

import dzve.utils.LogService;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private final File dataFolder;

    public DatabaseManager(String dataFolderPath) {
        this.dataFolder = new File(dataFolderPath);
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public void connect() throws SQLException, ClassNotFoundException {
        // Ensure driver is loaded
        Class.forName("org.sqlite.JDBC");

        File dbFile = new File(dataFolder, "groups.db");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        java.util.Properties props = new java.util.Properties();
        props.setProperty("foreign_keys", "true");
        // Set busy timeout to 30 seconds to handle locking situations better
        props.setProperty("busy_timeout", "30000");

        try (Connection conn = DriverManager.getConnection(url, props)) {
            LogService.info("DATABASE", "Connected to SQLite database at " + dbFile.getAbsolutePath());

            // Enable WAL mode for better concurrency
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL;");
                stmt.execute("PRAGMA synchronous=NORMAL;");
            }

            initializeTables(conn);
        }
    }

    private void initializeTables(Connection conn) {
        LogService.debug("DATABASE", "Initializing database tables...");
        try (Statement stmt = conn.createStatement()) {
            // Groups Table
            stmt.execute("CREATE TABLE IF NOT EXISTS groups (" +
                    "id TEXT PRIMARY KEY, " +
                    "name TEXT NOT NULL UNIQUE, " +
                    "tag TEXT NOT NULL UNIQUE, " +
                    "description TEXT, " +
                    "color TEXT, " +
                    "created_at INTEGER, " +
                    "leader_id TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "level INTEGER, " + // For Guild
                    "money_to_next_level REAL, " + // For Guild
                    "bank_balance REAL DEFAULT 0, " +
                    "kills INTEGER DEFAULT 0, " +
                    "deaths INTEGER DEFAULT 0, " +
                    "homes_json TEXT" + // Deprecated, using homes table
                    ");");

            // Members Table
            stmt.execute("CREATE TABLE IF NOT EXISTS members (" +
                    "player_id TEXT NOT NULL, " + // UUID
                    "group_id TEXT NOT NULL, " +
                    "player_name TEXT, " +
                    "role_id TEXT NOT NULL, " +
                    "joined_at INTEGER, " +
                    "last_online INTEGER, " +
                    "default_home_id TEXT, " +
                    "player_power REAL DEFAULT 0, " +
                    "contribution REAL DEFAULT 0, " +
                    "bank_balance REAL DEFAULT 0, " +
                    "PRIMARY KEY (player_id, group_id), " +
                    "FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE" +
                    ");");

            // Claims Table
            stmt.execute("CREATE TABLE IF NOT EXISTS claims (" +
                    "world_name TEXT NOT NULL, " +
                    "chunk_x INTEGER NOT NULL, " +
                    "chunk_z INTEGER NOT NULL, " +
                    "group_id TEXT NOT NULL, " +
                    "claimed_at INTEGER, " +
                    "PRIMARY KEY (world_name, chunk_x, chunk_z), " +
                    "FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE" +
                    ");");

            // Roles Table
            stmt.execute("CREATE TABLE IF NOT EXISTS roles (" +
                    "role_id TEXT NOT NULL, " +
                    "group_id TEXT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "priority INTEGER, " +
                    "permissions_json TEXT, " +
                    "PRIMARY KEY (role_id, group_id), " +
                    "FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE" +
                    ");");

            // Relations Table (Diplomacy)
            stmt.execute("CREATE TABLE IF NOT EXISTS diplomacy (" +
                    "group_id_1 TEXT NOT NULL, " +
                    "group_id_2 TEXT NOT NULL, " +
                    "status TEXT NOT NULL, " +
                    "PRIMARY KEY (group_id_1, group_id_2), " +
                    "FOREIGN KEY (group_id_1) REFERENCES groups(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (group_id_2) REFERENCES groups(id) ON DELETE CASCADE" +
                    ");");

            // Group Invitations
            stmt.execute("CREATE TABLE IF NOT EXISTS invitations (" +
                    "group_id TEXT NOT NULL, " +
                    "player_id TEXT NOT NULL, " +
                    "inviter_id TEXT, " +
                    "timestamp INTEGER, " +
                    "PRIMARY KEY (group_id, player_id), " +
                    "FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE" +
                    ");");

            // Homes Table
            stmt.execute("CREATE TABLE IF NOT EXISTS homes (" +
                    "home_id TEXT PRIMARY KEY, " +
                    "group_id TEXT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "world_name TEXT NOT NULL, " +
                    "x REAL NOT NULL, " +
                    "y REAL NOT NULL, " +
                    "z REAL NOT NULL, " +
                    "yaw REAL NOT NULL, " +
                    "pitch REAL NOT NULL, " +
                    "FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE" +
                    ");");

            // Check for schema updates (migrations)
            checkSchemaUpdates(conn);

        } catch (SQLException e) {
            LogService.error("DATABASE", "Failed to initialize database tables", e);
        }
    }

    private void checkSchemaUpdates(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Check members table for new columns
            boolean hasPower = false;
            boolean hasContribution = false;
            boolean hasBankBalance = false;

            try (java.sql.ResultSet rs = stmt.executeQuery("PRAGMA table_info(members)")) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    if ("player_power".equalsIgnoreCase(name))
                        hasPower = true;
                    if ("contribution".equalsIgnoreCase(name))
                        hasContribution = true;
                    if ("bank_balance".equalsIgnoreCase(name))
                        hasBankBalance = true;
                }
            }

            if (!hasPower) {
                LogService.info("DATABASE", "Migrating database: Adding player_power column to members table...");
                stmt.execute("ALTER TABLE members ADD COLUMN player_power REAL DEFAULT 0;");
            }

            if (!hasContribution) {
                LogService.info("DATABASE", "Migrating database: Adding contribution column to members table...");
                stmt.execute("ALTER TABLE members ADD COLUMN contribution REAL DEFAULT 0;");
            }

            if (!hasBankBalance) {
                LogService.info("DATABASE", "Migrating database: Adding bank_balance column to members table...");
                stmt.execute("ALTER TABLE members ADD COLUMN bank_balance REAL DEFAULT 0;");
            }
        } catch (SQLException e) {
            LogService.error("DATABASE", "Failed to check/apply schema updates", e);
        }
    }

    public Connection getConnection() throws SQLException {
        File dbFile = new File(dataFolder, "groups.db");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        java.util.Properties props = new java.util.Properties();
        props.setProperty("foreign_keys", "true");
        // Set busy timeout to 30 seconds to handle locking situations better
        props.setProperty("busy_timeout", "30000");
        return DriverManager.getConnection(url, props);
    }

    public void close() {
        // No persistent connection to close
    }
}
