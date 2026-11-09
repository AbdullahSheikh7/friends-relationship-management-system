package frmSystem.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public final class Database {

    private static final String URL = "jdbc:sqlite:frm_vault.db";
    private static volatile boolean initialized = false;

    public static Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(URL);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
        }
        return c;
    }

    /** Idempotent: safe to call on every startup. */
    public static synchronized void init() {
        if (initialized) return;
        try (Connection c = connect()) {
            System.out.println("Database Connection Successful");
            createTables(c);
            migrateTables(c);
            initialized = true;
            System.out.println("Database schema ready");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ---------- Schema creation ----------

    private static void createTables(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {

            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS friends (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    name            TEXT    NOT NULL DEFAULT '',
                    occupation      TEXT             DEFAULT '',
                    headline        TEXT             DEFAULT '',
                    city            TEXT             DEFAULT '',
                    pronouns        TEXT             DEFAULT '',
                    friends_since   TEXT             DEFAULT '',
                    email           TEXT             DEFAULT '',
                    phone           TEXT             DEFAULT '',
                    handle          TEXT             DEFAULT '',
                    space           TEXT             DEFAULT 'Work',
                    closeness       TEXT             DEFAULT 'CLOSE',
                    cadence         TEXT             DEFAULT 'MONTHLY',
                    last_contacted  TEXT    NOT NULL DEFAULT (date('now')),
                    birthday        TEXT,
                    snoozed_until   TEXT,
                    favorite        INTEGER NOT NULL DEFAULT 0,
                    archived        INTEGER NOT NULL DEFAULT 0,
                    hobbies         TEXT             DEFAULT ''
                )""");

            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS notes (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    friend_id  INTEGER NOT NULL,
                    title      TEXT             DEFAULT '',
                    body       TEXT             DEFAULT '',
                    date       TEXT    NOT NULL DEFAULT (date('now')),
                    flagged    INTEGER NOT NULL DEFAULT 0,
                    tags       TEXT             DEFAULT '',
                    FOREIGN KEY (friend_id) REFERENCES friends(id) ON DELETE CASCADE
                )""");

            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS contact_entries (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    friend_id INTEGER NOT NULL,
                    date      TEXT    NOT NULL DEFAULT (date('now')),
                    type      TEXT    NOT NULL DEFAULT 'CHAT',
                    duration  TEXT             DEFAULT '',
                    title     TEXT             DEFAULT '',
                    location  TEXT             DEFAULT '',
                    summary   TEXT             DEFAULT '',
                    tags      TEXT             DEFAULT '',
                    FOREIGN KEY (friend_id) REFERENCES friends(id) ON DELETE CASCADE
                )""");

            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS gift_ideas (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    friend_id INTEGER NOT NULL,
                    item      TEXT             DEFAULT '',
                    price_usd INTEGER NOT NULL DEFAULT 0,
                    purchased INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (friend_id) REFERENCES friends(id) ON DELETE CASCADE
                )""");

            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS snapshots (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    label      TEXT    NOT NULL,
                    created_at TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
                    friends    INTEGER NOT NULL DEFAULT 0,
                    notes      INTEGER NOT NULL DEFAULT 0,
                    contacts   INTEGER NOT NULL DEFAULT 0,
                    gifts      INTEGER NOT NULL DEFAULT 0,
                    payload    TEXT    NOT NULL
                )""");

            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_friends_name  ON friends(name COLLATE NOCASE)");
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_notes_friend  ON notes(friend_id)");
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_notes_flag    ON notes(friend_id, flagged)");
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ce_friend     ON contact_entries(friend_id)");
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ce_date       ON contact_entries(date)");
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_gi_friend     ON gift_ideas(friend_id)");
        }
    }

    // ---------- Migrations (add missing columns) ----------

    private static void migrateTables(Connection c) throws SQLException {
        migrate(c, "friends", new String[][] {
            // { column, ALTER-definition, optional backfill expression for existing rows }
            { "name",           "TEXT NOT NULL DEFAULT ''",    null },
            { "occupation",     "TEXT DEFAULT ''",              null },
            { "headline",       "TEXT DEFAULT ''",              null },
            { "city",           "TEXT DEFAULT ''",              null },
            { "pronouns",       "TEXT DEFAULT ''",              null },
            { "friends_since",  "TEXT DEFAULT ''",              null },
            { "email",          "TEXT DEFAULT ''",              null },
            { "phone",          "TEXT DEFAULT ''",              null },
            { "handle",         "TEXT DEFAULT ''",              null },
            { "space",          "TEXT DEFAULT 'Work'",          null },
            { "closeness",      "TEXT DEFAULT 'CLOSE'",         null },
            { "cadence",        "TEXT DEFAULT 'MONTHLY'",       null },
            { "last_contacted", "TEXT",                         "date('now')" },
            { "birthday",       "TEXT",                         null },
            { "snoozed_until",  "TEXT",                         null },
            { "favorite",       "INTEGER NOT NULL DEFAULT 0",   null },
            { "archived",       "INTEGER NOT NULL DEFAULT 0",   null },
            { "hobbies",        "TEXT DEFAULT ''",              null },
        });

        migrate(c, "notes", new String[][] {
            { "friend_id", "INTEGER NOT NULL DEFAULT 0",   null },
            { "title",     "TEXT DEFAULT ''",              null },
            { "body",      "TEXT DEFAULT ''",              null },
            { "date",      "TEXT",                         "date('now')" },
            { "flagged",   "INTEGER NOT NULL DEFAULT 0",   null },
            { "tags",      "TEXT DEFAULT ''",              null },
        });

        migrate(c, "contact_entries", new String[][] {
            { "friend_id", "INTEGER NOT NULL DEFAULT 0",   null },
            { "date",      "TEXT",                         "date('now')" },
            { "type",      "TEXT NOT NULL DEFAULT 'CHAT'", null },
            { "duration",  "TEXT DEFAULT ''",              null },
            { "title",     "TEXT DEFAULT ''",              null },
            { "location",  "TEXT DEFAULT ''",              null },
            { "summary",   "TEXT DEFAULT ''",              null },
            { "tags",      "TEXT DEFAULT ''",              null },
        });

        migrate(c, "gift_ideas", new String[][] {
            { "friend_id", "INTEGER NOT NULL DEFAULT 0", null },
            { "item",      "TEXT DEFAULT ''",            null },
            { "price_usd", "INTEGER NOT NULL DEFAULT 0", null },
            { "purchased", "INTEGER NOT NULL DEFAULT 0", null },
        });
    }

    private static void migrate(Connection c, String table, String[][] columns) throws SQLException {
        Set<String> existing = existingColumns(c, table);
        try (Statement s = c.createStatement()) {
            for (String[] col : columns) {
                String name = col[0];
                if (existing.contains(name)) continue;
                s.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + name + " " + col[1]);
                if (col.length > 2 && col[2] != null) {
                    // SQLite forbids non-constant DEFAULT in ALTER TABLE,
                    // so backfill existing rows manually.
                    s.executeUpdate(
                        "UPDATE " + table + " SET " + name + " = " + col[2]
                        + " WHERE " + name + " IS NULL");
                }
                System.out.println("Migrated " + table + ": added column " + name);
            }
        }
    }

    private static Set<String> existingColumns(Connection c, String table) throws SQLException {
        Set<String> cols = new HashSet<>();
        try (PreparedStatement ps = c.prepareStatement("PRAGMA table_info(" + table + ")");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) cols.add(rs.getString("name"));
        }
        return cols;
    }
}