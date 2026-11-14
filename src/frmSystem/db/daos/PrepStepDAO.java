package frmSystem.db.daos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import frmSystem.db.Database;
import frmSystem.db.models.PrepStep;

/** Milestone preparation steps for the Birthdays guide (global checklist).
 *  Persists the guide card that previously kept its rows only in memory. */
public final class PrepStepDAO {

    private PrepStepDAO() {}

    private static final List<String> DEFAULT_STEPS = List.of(
            "Ask discreetly about a quiet dinner or a gathering",
            "Create a shared group chat for the college circle",
            "Order a keepsake (allow 10 days for engraving)",
            "Compile a photo memory book from the archive");

    public static List<PrepStep> findAll() {
        List<PrepStep> steps = new ArrayList<>();
        try (Connection c = Database.connect();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM prep_steps ORDER BY id")) {
            while (rs.next()) {
                PrepStep step = new PrepStep();
                step.setId(rs.getLong("id"));
                step.setText(rs.getString("text"));
                step.setDone(rs.getInt("done") == 1);
                steps.add(step);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Prep steps query failed", e);
        }
        return steps;
    }

    public static void insert(String text) {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO prep_steps(text, done) VALUES(?, 0)")) {
            ps.setString(1, text);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Prep step insert failed", e);
        }
    }

    public static void setDone(long id, boolean done) {
        execute("UPDATE prep_steps SET done = ? WHERE id = ?", done ? 1 : 0, id);
    }

    public static void delete(long id) {
        execute("DELETE FROM prep_steps WHERE id = ?", id);
    }

    /** Creates the table when an older database file is missing it, so schema
     *  drift can never crash the app at startup. */
    private static void ensureTable() {
        try (Connection c = Database.connect();
             Statement s = c.createStatement()) {
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS prep_steps (
                    id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    text TEXT    NOT NULL,
                    done INTEGER NOT NULL DEFAULT 0
                )""");
        } catch (SQLException e) {
            throw new RuntimeException("Prep steps table setup failed", e);
        }
    }

    /** Seeds the four default steps once, when the table is still empty.
     *  The read connection is closed before any insert happens. */
    public static void bootstrap() {
        ensureTable();
        boolean empty;
        try (Connection c = Database.connect();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM prep_steps")) {
            empty = rs.next() && rs.getLong(1) == 0;
        } catch (SQLException e) {
            throw new RuntimeException("Prep steps bootstrap failed", e);
        }
        if (empty) {
            for (String step : DEFAULT_STEPS) {
                insert(step);
            }
        }
    }

    private static void execute(String sql, Object... params) {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Prep step update failed", e);
        }
    }
}
