package frmSystem.db.daos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import frmSystem.db.Database;
import frmSystem.db.models.Friend;
import frmSystem.db.models.Note;
import frmSystem.pages.views.SearchEngine;

/** Persistent inverted index over the search_index table (DSA concept #4).
 *  Every friend record is tokenized into keywords, each keyword is stored with
 *  its numeric signature, the owning friend, and its frequency (quantity).
 *  Relevance is frequency-based: results sort by summed quantities. */
public final class SearchIndexDAO {

    private SearchIndexDAO() {}

    // ---------- Index maintenance ----------

    public static void reindex(long friendId) {
        Friend friend = FriendDAO.findById(friendId).orElse(null);
        if (friend == null) {
            deleteForFriend(friendId);
            return;
        }
        List<Note> notes = NoteDAO.findByFriend(friendId);
        Map<String, Integer> keywords = SearchEngine.extractKeywords(friend, notes);

        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM search_index WHERE friend_id = ?")) {
                ps.setLong(1, friendId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO search_index(signature, keyword, friend_id, quantity) VALUES(?, ?, ?, ?)")) {
                for (Map.Entry<String, Integer> keyword : keywords.entrySet()) {
                    ps.setLong(1, SearchEngine.signature(keyword.getKey()));
                    ps.setString(2, keyword.getKey());
                    ps.setLong(3, friendId);
                    ps.setInt(4, keyword.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Search index rebuild failed for friend " + friendId, e);
        }
    }

    public static void deleteForFriend(long friendId) {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("DELETE FROM search_index WHERE friend_id = ?")) {
            ps.setLong(1, friendId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Search index delete failed for friend " + friendId, e);
        }
    }

    public static void rebuildAll() {
        for (Friend friend : FriendDAO.findAll()) {
            reindex(friend.getId());
        }
    }

    /** Rebuilds the index once when it is empty while friend records exist. */
    public static void bootstrap() {
        if (FriendDAO.findAll().isEmpty()) {
            return;
        }
        try (Connection c = Database.connect();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM search_index")) {
            if (rs.next() && rs.getLong(1) == 0) {
                rebuildAll();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Search index bootstrap failed", e);
        }
    }

    // ---------- Ranked search ----------

    /** friend_id -> total frequency score, highest first when consumed by the view. */
    public static Map<Long, Integer> rank(String query) {
        List<String> words = SearchEngine.queryWords(query);
        Map<Long, Integer> scores = new HashMap<>();
        if (words.isEmpty()) {
            return scores;
        }

        for (String word : words) {
            addMatches(scores, SearchEngine.signature(word), 10);
            for (String synonym : SearchEngine.synonyms(word)) {
                addMatches(scores, SearchEngine.signature(synonym), 10);
            }
            addPrefix(scores, word, 6);
            addFuzzy(scores, word);
        }
        return scores;
    }

    /** Exact keyword hits: signature equality — the same formula the rows were built with. */
    private static void addMatches(Map<Long, Integer> scores, long signature, int multiplier) {
        String sql = "SELECT friend_id, SUM(quantity) FROM search_index WHERE signature = ? GROUP BY friend_id";
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, signature);
            merge(scores, ps, multiplier);
        } catch (SQLException e) {
            throw new RuntimeException("Search rank failed", e);
        }
    }

    /** Partial typing: every indexed keyword starting with the typed prefix. */
    private static void addPrefix(Map<Long, Integer> scores, String prefix, int multiplier) {
        String sql = "SELECT friend_id, SUM(quantity) FROM search_index WHERE keyword LIKE ? GROUP BY friend_id";
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            merge(scores, ps, multiplier);
        } catch (SQLException e) {
            throw new RuntimeException("Search prefix failed", e);
        }
    }

    private static void addFuzzy(Map<Long, Integer> scores, String word) {
        if (word.length() < 4) {
            return;
        }
        int limit = word.length() <= 5 ? 1 : 2;
        List<String> candidates = distinctKeywords();
        for (String candidate : candidates) {
            if (candidate.equals(word) || candidate.startsWith(word)) continue;
            if (candidate.charAt(0) != word.charAt(0)) continue;
            if (Math.abs(candidate.length() - word.length()) > limit) continue;
            if (SearchEngine.levenshtein(word, candidate) <= limit) {
                addKeywordOccurrences(scores, candidate, 4);
            }
        }
    }

    private static List<String> distinctKeywords() {
        List<String> keywords = new java.util.ArrayList<>();
        try (Connection c = Database.connect();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT DISTINCT keyword FROM search_index")) {
            while (rs.next()) {
                keywords.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Search keywords query failed", e);
        }
        return keywords;
    }

    private static void addKeywordOccurrences(Map<Long, Integer> scores, String keyword, int multiplier) {
        String sql = "SELECT friend_id, quantity FROM search_index WHERE keyword = ?";
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, keyword);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    scores.merge(rs.getLong(1), rs.getInt(2) * multiplier, Integer::sum);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Search keyword query failed", e);
        }
    }

    private static void merge(Map<Long, Integer> scores, PreparedStatement ps, int multiplier) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                scores.merge(rs.getLong(1), rs.getInt(2) * multiplier, Integer::sum);
            }
        }
    }
}
