package frmSystem.db.daos;

import java.sql.*;
import java.util.*;

import frmSystem.db.models.GiftIdea;

import frmSystem.db.Database;

public final class GiftIdeaDAO {

    private GiftIdeaDAO() {}

    private static final String ORDER = " ORDER BY item COLLATE NOCASE";

    private static final String INSERT = """
            INSERT INTO gift_ideas(friend_id, item, price_usd, purchased)
            VALUES(?, ?, ?, ?)""";

    private static final String UPDATE = """
            UPDATE gift_ideas SET friend_id = ?, item = ?, price_usd = ?, purchased = ?
            WHERE id = ?""";

    // ---------- CREATE ----------

    public static void insert(GiftIdea g) {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, g);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) g.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Insert failed", e);
        }
    }

    // ---------- READ ----------

    public static Optional<GiftIdea> findById(long id) {
        return queryOne("SELECT * FROM gift_ideas WHERE id = ?", id);
    }

    public static List<GiftIdea> findByFriend(long friendId) {
        return query("SELECT * FROM gift_ideas WHERE friend_id = ?" + ORDER, friendId);
    }

    public static List<GiftIdea> findUnpurchasedByFriend(long friendId) {
        return query("SELECT * FROM gift_ideas WHERE friend_id = ? AND purchased = 0" + ORDER, friendId);
    }

    public static List<GiftIdea> search(String text) {
        return query("SELECT * FROM gift_ideas WHERE item LIKE ?" + ORDER, "%" + text + "%");
    }
    
    public static List<GiftIdea> findAll() {
        return query("SELECT * FROM gift_ideas" + ORDER);
    }

    // ---------- UPDATE ----------

    public static void update(GiftIdea g) {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            bind(ps, g);
            ps.setLong(5, g.getId());       // bind wrote 1..4
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Update failed", e);
        }
    }

    /** Toggle purchased flag without touching other fields. */
    public static void markPurchased(long id) {
        execute("UPDATE gift_ideas SET purchased = 1 WHERE id = ?", id);
    }

    // ---------- DELETE ----------

    public static void delete(long id) {
        execute("DELETE FROM gift_ideas WHERE id = ?", id);
    }

    public static void deleteByFriend(long friendId) {
        execute("DELETE FROM gift_ideas WHERE friend_id = ?", friendId);
    }

    // ---------- Helpers ----------

    private static List<GiftIdea> query(String sql, Object... params) {
        List<GiftIdea> list = new ArrayList<>();
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

    private static Optional<GiftIdea> queryOne(String sql, Object... params) {
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

    /** Writes exactly 4 columns (id only used by UPDATE's WHERE). */
    private static void bind(PreparedStatement ps, GiftIdea g) throws SQLException {
        int i = 1;
        ps.setLong (i++, g.getFriendId());
        ps.setString(i++, g.getItem());
        ps.setInt  (i++, g.getPriceUsd());
        ps.setInt  (i,   g.isPurchased() ? 1 : 0);
    }

    private static GiftIdea map(ResultSet rs) throws SQLException {
        GiftIdea g = new GiftIdea();
        g.setId(rs.getLong("id"));
        g.setFriendId(rs.getLong("friend_id"));
        g.setItem(rs.getString("item"));
        g.setPriceUsd(rs.getInt("price_usd"));
        g.setPurchased(rs.getInt("purchased") == 1);
        return g;
    }
}