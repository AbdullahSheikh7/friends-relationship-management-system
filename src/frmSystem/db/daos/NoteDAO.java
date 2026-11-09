package frmSystem.db.daos;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

import frmSystem.db.models.Note;

import frmSystem.db.Database;

public final class NoteDAO {

    private NoteDAO() {}

    private static final String TAG_SEPARATOR = "|";
    private static final String ORDER = " ORDER BY date DESC, id DESC";

    private static final String INSERT = """
            INSERT INTO notes(friend_id, title, body, date, flagged, tags)
            VALUES(?, ?, ?, ?, ?, ?)""";

    private static final String UPDATE = """
            UPDATE notes SET friend_id = ?, title = ?, body = ?, date = ?,
                             flagged = ?, tags = ?
            WHERE id = ?""";

    // ---------- CREATE ----------

    public static void insert(Note n) {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, n);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) n.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Insert failed", e);
        }
    }

    // ---------- READ ----------

    public static Optional<Note> findById(long id) {
        return queryOne("SELECT * FROM notes WHERE id = ?", id);
    }

    public static List<Note> findByFriend(long friendId) {
        return query("SELECT * FROM notes WHERE friend_id = ?" + ORDER, friendId);
    }

    public static List<Note> findFlaggedByFriend(long friendId) {
        return query("SELECT * FROM notes WHERE friend_id = ? AND flagged = 1" + ORDER, friendId);
    }

    public static List<Note> search(String text) {
        String like = "%" + text + "%";
        return query("SELECT * FROM notes WHERE title LIKE ? OR body LIKE ?" + ORDER, like, like);
    }
    
    public static List<Note> findAll() {
        return query("SELECT * FROM notes" + ORDER);
    }

    // ---------- UPDATE ----------

    public static void update(Note n) {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            bind(ps, n);
            ps.setLong(7, n.getId());        // bind wrote 1..6
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Update failed", e);
        }
    }

    // ---------- DELETE ----------

    public static void delete(long id) {
        execute("DELETE FROM notes WHERE id = ?", id);
    }

    public static void deleteByFriend(long friendId) {
        execute("DELETE FROM notes WHERE friend_id = ?", friendId);
    }

    // ---------- Helpers ----------

    private static List<Note> query(String sql, Object... params) {
        List<Note> list = new ArrayList<>();
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

    private static Optional<Note> queryOne(String sql, Object... params) {
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

    /** Writes exactly 6 columns (id only used by UPDATE's WHERE). */
    private static void bind(PreparedStatement ps, Note n) throws SQLException {
        int i = 1;
        ps.setLong  (i++, n.getFriendId());
        ps.setString(i++, n.getTitle());
        ps.setString(i++, n.getBody());
        ps.setString(i++, n.getDate().toString());
        ps.setInt   (i++, n.isFlagged() ? 1 : 0);
        ps.setString(i,   String.join(TAG_SEPARATOR, n.getTags()));
    }

    private static Note map(ResultSet rs) throws SQLException {
        Note n = new Note();
        n.setId(rs.getLong("id"));
        n.setFriendId(rs.getLong("friend_id"));
        n.setTitle(rs.getString("title"));
        n.setBody(rs.getString("body"));
        n.setDate(LocalDate.parse(rs.getString("date")));
        n.setFlagged(rs.getInt("flagged") == 1);
        String t = rs.getString("tags");
        n.setTags(t == null || t.isBlank() ? List.of() : List.of(t.split("\\" + TAG_SEPARATOR)));
        return n;
    }
}