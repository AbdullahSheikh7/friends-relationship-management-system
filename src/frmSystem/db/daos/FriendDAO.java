package frmSystem.db.daos;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

import frmSystem.db.Database;
import frmSystem.db.models.Cadence;
import frmSystem.db.models.Closeness;
import frmSystem.db.models.Friend;

public final class FriendDAO {

    private FriendDAO() {}

    private static final String HOBBY_SEPARATOR = "|";
    private static final String ORDER = " ORDER BY name COLLATE NOCASE";

    private static final String INSERT = """
            INSERT INTO friends(name, occupation, headline, city, pronouns, friends_since,
                                email, phone, handle, space, closeness, cadence,
                                last_contacted, birthday, snoozed_until, favorite, archived, hobbies)
            VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";

    private static final String UPDATE = """
            UPDATE friends SET name = ?, occupation = ?, headline = ?, city = ?, pronouns = ?,
                               friends_since = ?, email = ?, phone = ?, handle = ?, space = ?,
                               closeness = ?, cadence = ?, last_contacted = ?, birthday = ?,
                               snoozed_until = ?, favorite = ?, archived = ?, hobbies = ?
            WHERE id = ?""";
    
    public static void snooze(long id, LocalDate until) {
        execute("UPDATE friends SET snoozed_until = ? WHERE id = ?", until.toString(), id);
    }

    // ---------- CREATE ----------

    public static void insert(Friend f) {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, f);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) f.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Insert failed", e);
        }
    }

    // ---------- READ ----------

    public static List<Friend> findAll() {
        return query("SELECT * FROM friends" + ORDER);
    }

    public static List<Friend> findActive() {
        return query("SELECT * FROM friends WHERE archived = 0" + ORDER);
    }

    public static Optional<Friend> findById(long id) {
        return queryOne("SELECT * FROM friends WHERE id = ?", id);
    }

    public static List<Friend> search(String text) {
        String like = "%" + text + "%";
        return query(
                "SELECT * FROM friends WHERE name LIKE ? OR city LIKE ? "
              + "OR occupation LIKE ? OR handle LIKE ?" + ORDER,
                like, like, like, like);
    }

    // ---------- UPDATE ----------

    public static void update(Friend f) {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            bind(ps, f);
            ps.setLong(19, f.getId()); // bind writes params 1..18
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Update failed", e);
        }
    }

    public static void markContacted(long id, LocalDate date) {
        execute("UPDATE friends SET last_contacted = ?, snoozed_until = NULL WHERE id = ?",
                date.toString(), id);
    }

    // ---------- DELETE ----------

    public static void delete(long id) {
        execute("DELETE FROM friends WHERE id = ?", id);
    }

    // ---------- Helpers ----------

    private static List<Friend> query(String sql, Object... params) {
        List<Friend> list = new ArrayList<>();
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

    private static Optional<Friend> queryOne(String sql, Object... params) {
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

    /** Writes exactly 18 columns (ids are only used by UPDATE's WHERE clause). */
    private static void bind(PreparedStatement ps, Friend f) throws SQLException {
        int i = 1;
        ps.setString(i++, f.getName());
        ps.setString(i++, f.getOccupation());
        ps.setString(i++, f.getHeadline());
        ps.setString(i++, f.getCity());
        ps.setString(i++, f.getPronouns());
        ps.setString(i++, f.getFriendsSince());
        ps.setString(i++, f.getEmail());
        ps.setString(i++, f.getPhone());
        ps.setString(i++, f.getHandle());
        ps.setString(i++, f.getSpace());
        ps.setString(i++, f.getCloseness().name());
        ps.setString(i++, f.getCadence().name());
        ps.setString(i++, f.getLastContacted().toString());
        ps.setString(i++, dateOrNull(f.getBirthday()));
        ps.setString(i++, dateOrNull(f.getSnoozedUntil()));
        ps.setInt   (i++, f.isFavorite() ? 1 : 0);
        ps.setInt   (i++, f.isArchived() ? 1 : 0);
        ps.setString(i,   String.join(HOBBY_SEPARATOR, f.getHobbies()));
    }

    private static Friend map(ResultSet rs) throws SQLException {
        Friend f = new Friend();
        f.setId(rs.getLong("id"));
        f.setName(rs.getString("name"));
        f.setOccupation(rs.getString("occupation"));
        f.setHeadline(rs.getString("headline"));
        f.setCity(rs.getString("city"));
        f.setPronouns(rs.getString("pronouns"));
        f.setFriendsSince(rs.getString("friends_since"));
        f.setEmail(rs.getString("email"));
        f.setPhone(rs.getString("phone"));
        f.setHandle(rs.getString("handle"));
        f.setSpace(rs.getString("space"));
        f.setCloseness(Closeness.valueOf(rs.getString("closeness")));
        f.setCadence(Cadence.valueOf(rs.getString("cadence")));
        f.setLastContacted(LocalDate.parse(rs.getString("last_contacted")));
        f.setBirthday(parseDate(rs.getString("birthday")));
        f.setSnoozedUntil(parseDate(rs.getString("snoozed_until")));
        f.setFavorite(rs.getInt("favorite") == 1);
        f.setArchived(rs.getInt("archived") == 1);
        String h = rs.getString("hobbies");
        f.setHobbies(h == null || h.isBlank()
                ? List.of()
                : List.of(h.split("\\" + HOBBY_SEPARATOR)));
        return f;
    }

    private static String dateOrNull(LocalDate d) {
        return d == null ? null : d.toString();
    }

    private static LocalDate parseDate(String s) {
        return (s == null || s.isBlank()) ? null : LocalDate.parse(s);
    }
}