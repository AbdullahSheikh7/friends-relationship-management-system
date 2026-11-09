package frmSystem.db.daos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import frmSystem.db.Database;
import frmSystem.db.models.ContactEntry;
import frmSystem.db.models.Friend;
import frmSystem.db.models.GiftIdea;
import frmSystem.db.models.Note;

/** Stores backup versions inside the vault database itself (DSA concept #2:
 *  the vault keeps a LIFO stack of snapshots — restore always pops the newest). */
public final class BackupDAO {

    private BackupDAO() {}

    public record Snapshot(long id, String label, String createdAt, int friends, int notes, int contacts, int gifts) {}

    // ---------- Public API ----------

    public static long insert(String label) {
        List<Friend> friends = FriendDAO.findAll();
        List<Note> notes = NoteDAO.findAll();
        List<ContactEntry> contacts = ContactEntryDAO.findAll();
        List<GiftIdea> gifts = GiftIdeaDAO.findAll();
        String payload = encode(friends, notes, contacts, gifts);
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO snapshots(label, friends, notes, contacts, gifts, payload) VALUES(?, ?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, label);
            ps.setInt(2, friends.size());
            ps.setInt(3, notes.size());
            ps.setInt(4, contacts.size());
            ps.setInt(5, gifts.size());
            ps.setString(6, payload);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Snapshot insert failed", e);
        }
    }

    /** Newest first — the head of this list is the top of the restore stack. */
    public static List<Snapshot> findAll() {
        List<Snapshot> list = new ArrayList<>();
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, label, created_at, friends, notes, contacts, gifts FROM snapshots ORDER BY id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Snapshot(rs.getLong("id"), rs.getString("label"), rs.getString("created_at"),
                        rs.getInt("friends"), rs.getInt("notes"), rs.getInt("contacts"), rs.getInt("gifts")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Snapshot query failed", e);
        }
        return list;
    }

    public static void delete(long id) {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("DELETE FROM snapshots WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Snapshot delete failed", e);
        }
    }

    /** Wipes live tables and rebuilds them from the snapshot inside one transaction. */
    public static void restore(long id) {
        List<String> payload;
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("SELECT payload FROM snapshots WHERE id = ?");
        ) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new RuntimeException("Snapshot not found: " + id);
                }
                payload = List.of(rs.getString("payload").split("\n", -1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Snapshot read failed", e);
        }
        decodeIntoLiveTables(payload);
    }

    // ---------- Transactional rebuild ----------

    private static void decodeIntoLiveTables(List<String> records) {
        try (Connection c = Database.connect()) {
            try (Statement s = c.createStatement()) {
                s.execute("PRAGMA foreign_keys = OFF");
            }
            c.setAutoCommit(false);
            try (Statement s = c.createStatement()) {
                s.executeUpdate("DELETE FROM gift_ideas");
                s.executeUpdate("DELETE FROM contact_entries");
                s.executeUpdate("DELETE FROM notes");
                s.executeUpdate("DELETE FROM friends");
            }
            insertFriends(c, records);
            insertNotes(c, records);
            insertContacts(c, records);
            insertGifts(c, records);
            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Snapshot restore failed", e);
        }
    }

    private static void insertFriends(Connection c, List<String> records) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO friends(id, name, occupation, headline, city, pronouns, friends_since,
                                    email, phone, handle, space, closeness, cadence, last_contacted,
                                    birthday, snoozed_until, favorite, archived, hobbies)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""")) {
            for (String r : records) {
                if (!r.startsWith("F|")) continue;
                String[] p = splitRecord(r);
                ps.setLong(1, Long.parseLong(p[1]));
                ps.setString(2, p[2]);
                ps.setString(3, p[3]);
                ps.setString(4, p[4]);
                ps.setString(5, p[5]);
                ps.setString(6, p[6]);
                ps.setString(7, p[7]);
                ps.setString(8, p[8]);
                ps.setString(9, p[9]);
                ps.setString(10, p[10]);
                ps.setString(11, p[11]);
                ps.setString(12, p[12]);
                ps.setString(13, p[13]);
                ps.setString(14, date(p[14]));
                ps.setString(15, date(p[15]));
                ps.setString(16, date(p[16]));
                ps.setInt(17, optInt(p[17]));
                ps.setInt(18, optInt(p[18]));
                ps.setString(19, p[19]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void insertNotes(Connection c, List<String> records) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO notes(id, friend_id, title, body, date, flagged, tags)
                VALUES(?, ?, ?, ?, ?, ?, ?)""")) {
            for (String r : records) {
                if (!r.startsWith("N|")) continue;
                String[] p = splitRecord(r);
                ps.setLong(1, Long.parseLong(p[1]));
                ps.setLong(2, Long.parseLong(p[2]));
                ps.setString(3, p[3]);
                ps.setString(4, p[4]);
                ps.setString(5, date(p[5]));
                ps.setInt(6, optInt(p[6]));
                ps.setString(7, p[7]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void insertContacts(Connection c, List<String> records) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO contact_entries(id, friend_id, date, type, duration, title, location, summary, tags)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)""")) {
            for (String r : records) {
                if (!r.startsWith("C|")) continue;
                String[] p = splitRecord(r);
                ps.setLong(1, Long.parseLong(p[1]));
                ps.setLong(2, Long.parseLong(p[2]));
                ps.setString(3, date(p[3]));
                ps.setString(4, p[4]);
                ps.setString(5, p[5]);
                ps.setString(6, p[6]);
                ps.setString(7, p[7]);
                ps.setString(8, p[8]);
                ps.setString(9, p[9]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void insertGifts(Connection c, List<String> records) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO gift_ideas(id, friend_id, item, price_usd, purchased)
                VALUES(?, ?, ?, ?, ?)""")) {
            for (String r : records) {
                if (!r.startsWith("G|")) continue;
                String[] p = splitRecord(r);
                ps.setLong(1, Long.parseLong(p[1]));
                ps.setLong(2, Long.parseLong(p[2]));
                ps.setString(3, p[3]);
                ps.setInt(4, p[4].isBlank() ? 0 : Integer.parseInt(p[4]));
                ps.setInt(5, optInt(p[5]));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ---------- Snapshot payload format ----------
    // Each row is one line:  F|..|N|..|C|..|G|.. fields joined by '|'.
    // Escapes inside a field:  \  ->  \\      |  ->  \p
    //                          \n ->  \n      \r ->  \r

    private static String encode(List<Friend> friends, List<Note> notes,
                                 List<ContactEntry> contacts, List<GiftIdea> gifts) {
        List<String> lines = new ArrayList<>();
        for (Friend f : friends) {
            lines.add(String.join("|", "F",
                    String.valueOf(f.getId()), e(f.getName()), e(f.getOccupation()), e(f.getHeadline()),
                    e(f.getCity()), e(f.getPronouns()), e(f.getFriendsSince()), e(f.getEmail()), e(f.getPhone()),
                    e(f.getHandle()), e(f.getSpace()), e(f.getCloseness().name()), e(f.getCadence().name()),
                    e(f.getLastContacted().toString()), e(dateOrNull(f.getBirthday())), e(dateOrNull(f.getSnoozedUntil())),
                    e(String.valueOf(f.isFavorite() ? 1 : 0)), e(String.valueOf(f.isArchived() ? 1 : 0)),
                    e(String.join("|", f.getHobbies()))));
        }
        for (Note n : notes) {
            lines.add(String.join("|", "N",
                    String.valueOf(n.getId()), String.valueOf(n.getFriendId()), e(n.getTitle()), e(n.getBody()),
                    e(n.getDate().toString()), e(String.valueOf(n.isFlagged() ? 1 : 0)),
                    e(String.join("|", n.getTags()))));
        }
        for (ContactEntry ce : contacts) {
            lines.add(String.join("|", "C",
                    String.valueOf(ce.getId()), String.valueOf(ce.getFriendId()), e(ce.getDate().toString()),
                    e(ce.getType().name()), e(ce.getDuration()), e(ce.getTitle()), e(ce.getLocation()),
                    e(ce.getSummary()), e(String.join("|", ce.getTags()))));
        }
        for (GiftIdea g : gifts) {
            lines.add(String.join("|", "G",
                    String.valueOf(g.getId()), String.valueOf(g.getFriendId()), e(g.getItem()),
                    String.valueOf(g.getPriceUsd()), e(String.valueOf(g.isPurchased() ? 1 : 0))));
        }
        return String.join("\n", lines);
    }

    private static String[] splitRecord(String record) {
        String[] raw = record.split("\\|", -1);
        for (int i = 0; i < raw.length; i++) {
            raw[i] = unescape(raw[i]);
        }
        return raw;
    }

    private static String e(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (char ch : value.toCharArray()) {
            switch (ch) {
                case '\\' -> b.append("\\\\");
                case '|' -> b.append("\\p");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                default -> b.append(ch);
            }
        }
        return b.toString();
    }

    private static String unescape(String token) {
        if (!token.contains("\\")) {
            return token;
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < token.length(); i++) {
            char ch = token.charAt(i);
            if (ch == '\\' && i < token.length() - 1) {
                char next = token.charAt(++i);
                switch (next) {
                    case 'p' -> b.append("|");
                    case 'n' -> b.append("\n");
                    case 'r' -> b.append("\r");
                    case '\\' -> b.append("\\");
                    default -> {
                        b.append(ch).append(next);
                    }
                }
            } else {
                b.append(ch);
            }
        }
        return b.toString();
    }

    private static String date(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static int optInt(String value) {
        return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
    }

    private static String dateOrNull(LocalDate date) {
        return date == null ? "" : date.toString();
    }
}
