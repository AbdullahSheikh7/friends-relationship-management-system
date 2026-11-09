package frmSystem.db.daos;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

import frmSystem.db.models.ContactEntry;
import frmSystem.db.models.ContactType;

import frmSystem.db.Database;

public final class ContactEntryDAO {

    private ContactEntryDAO() {}

    private static final String TAG_SEPARATOR = "|";
    private static final String ORDER = " ORDER BY date DESC, id DESC";

    private static final String INSERT = """
            INSERT INTO contact_entries(friend_id, date, type, duration, title, location, summary, tags)
            VALUES(?, ?, ?, ?, ?, ?, ?, ?)""";

    private static final String UPDATE = """
            UPDATE contact_entries SET friend_id = ?, date = ?, type = ?, duration = ?,
                                       title = ?, location = ?, summary = ?, tags = ?
            WHERE id = ?""";

    // ---------- CREATE ----------

    public static void insert(ContactEntry e) {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, e);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) e.setId(keys.getLong(1));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Insert failed", ex);
        }
    }

    // ---------- READ ----------

    public static Optional<ContactEntry> findById(long id) {
        return queryOne("SELECT * FROM contact_entries WHERE id = ?", id);
    }

    public static List<ContactEntry> findByFriend(long friendId) {
        return query("SELECT * FROM contact_entries WHERE friend_id = ?" + ORDER, friendId);
    }

    public static List<ContactEntry> findRecent(int limit) {
        return query("SELECT * FROM contact_entries" + ORDER + " LIMIT ?", limit);
    }

    public static List<ContactEntry> search(String text) {
        String like = "%" + text + "%";
        return query(
                "SELECT * FROM contact_entries WHERE title LIKE ? OR summary LIKE ? "
              + "OR location LIKE ?" + ORDER, like, like, like);
    }
    
    public static List<ContactEntry> findAll() {
        return query("SELECT * FROM contact_entries" + ORDER);
    }

    // ---------- UPDATE ----------

    public static void update(ContactEntry e) {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            bind(ps, e);
            ps.setLong(9, e.getId());       // bind wrote 1..8
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Update failed", ex);
        }
    }

    // ---------- DELETE ----------

    public static void delete(long id) {
        execute("DELETE FROM contact_entries WHERE id = ?", id);
    }

    public static void deleteByFriend(long friendId) {
        execute("DELETE FROM contact_entries WHERE friend_id = ?", friendId);
    }

    // ---------- Helpers ----------

    private static List<ContactEntry> query(String sql, Object... params) {
        List<ContactEntry> list = new ArrayList<>();
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bindAll(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query failed", e);
        }
        return list;
    }

    private static Optional<ContactEntry> queryOne(String sql, Object... params) {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bindAll(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query failed", e);
        }
    }

    private static void execute(String sql, Object... params) {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bindAll(ps, params);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed", e);
        }
    }

    private static void bindAll(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
    }

    /** Writes exactly 8 columns (id only used by UPDATE's WHERE). */
    private static void bind(PreparedStatement ps, ContactEntry e) throws SQLException {
        int i = 1;
        ps.setLong  (i++, e.getFriendId());
        ps.setString(i++, e.getDate().toString());
        ps.setString(i++, e.getType().name());
        ps.setString(i++, e.getDuration());
        ps.setString(i++, e.getTitle());
        ps.setString(i++, e.getLocation());
        ps.setString(i++, e.getSummary());
        ps.setString(i,   String.join(TAG_SEPARATOR, e.getTags()));
    }

    private static ContactEntry map(ResultSet rs) throws SQLException {
        ContactEntry e = new ContactEntry();
        e.setId(rs.getLong("id"));
        e.setFriendId(rs.getLong("friend_id"));
        e.setDate(LocalDate.parse(rs.getString("date")));
        e.setType(ContactType.valueOf(rs.getString("type")));
        e.setDuration(rs.getString("duration"));
        e.setTitle(rs.getString("title"));
        e.setLocation(rs.getString("location"));
        e.setSummary(rs.getString("summary"));
        String t = rs.getString("tags");
        e.setTags(t == null || t.isBlank() ? List.of() : List.of(t.split("\\" + TAG_SEPARATOR)));
        return e;
    }
}