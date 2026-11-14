package frmSystem.pages.views;

import frmSystem.db.models.Friend;
import frmSystem.db.models.Note;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Text-logic layer of the friends search engine (DSA concept #3: hash signatures).
 *  Pure functions only — the persistent inverted index lives in the
 *  search_index table and is driven by SearchIndexDAO. */
public final class SearchEngine {

    private static final Pattern WORDS = Pattern.compile("[^a-z0-9]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "at", "do", "does", "for", "from",
            "has", "have", "how", "i", "in", "is", "me", "my", "of",
            "on", "the", "to", "was", "what", "where", "which", "who",
            "with", "show", "find", "please"
    );

    private static final Map<String, List<String>> SYNONYMS = Map.ofEntries(
            Map.entry("job", List.of("work", "occupation", "profession")),
            Map.entry("work", List.of("job", "occupation", "profession")),
            Map.entry("hobby", List.of("hobbies", "interest", "interests")),
            Map.entry("hobbies", List.of("hobby", "interest", "interests")),
            Map.entry("interest", List.of("hobby", "hobbies")),
            Map.entry("location", List.of("city", "address")),
            Map.entry("city", List.of("location")),
            Map.entry("birthday", List.of("birth", "born")),
            Map.entry("phone", List.of("mobile", "contact")),
            Map.entry("mobile", List.of("phone", "contact")),
            Map.entry("notes", List.of("note", "memo")),
            Map.entry("note", List.of("notes", "memo"))
    );

    // ---------- Word signature (collision-resistant hash) ----------
    // Letter weights: a..z = 1..26, digits 0..9 = 27..36.
    // Recursive multiply -> add -> square, one letter at a time, mod a big prime:
    //   h0 = 7
    //   hi = ((h(i-1) * wi) + wi)^2 mod 1_000_000_007

    public static final long SIGNATURE_MODULUS = 1_000_000_007L;
    private static final int SEED = 7;

    public static long signature(String word) {
        long hash = SEED;
        for (int i = 0; i < word.length(); i++) {
            long weight = weight(word.charAt(i));
            if (weight == 0) {
                continue;
            }
            long step = (hash * weight + weight) % SIGNATURE_MODULUS;
            hash = step * step % SIGNATURE_MODULUS;
        }
        return hash;
    }

    private static int weight(char c) {
        if (c >= 'a' && c <= 'z') {
            return c - 'a' + 1;
        }
        if (c >= '0' && c <= '9') {
            return c - '0' + 27;
        }
        return 0;
    }

    // ---------- Tokenization & query shaping ----------

    public static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(WORDS.split(text.toLowerCase(Locale.ROOT)))
                .filter(word -> !word.isBlank())
                .toList();
    }

    public static boolean isStopWord(String word) {
        return STOP_WORDS.contains(word);
    }

    /** Query words: single letters survive so live-prefix typing still matches. */
    public static List<String> queryWords(String query) {
        return tokenize(query).stream()
                .filter(word -> word.length() == 1 || !STOP_WORDS.contains(word))
                .toList();
    }

    public static List<String> synonyms(String word) {
        return SYNONYMS.getOrDefault(word, List.of());
    }

    // ---------- Field weights & keyword extraction ----------

    /** Weighted keyword -> total quantity map for one friend record (friend fields + notes). */
    public static Map<String, Integer> extractKeywords(Friend friend, List<Note> notes) {
        Map<String, Integer> keywords = new LinkedHashMap<>();
        accumulate(keywords, friend.getName(), 8);
        accumulate(keywords, friend.getOccupation(), 5);
        accumulate(keywords, friend.getHeadline(), 4);
        accumulate(keywords, String.join(" ", friend.getHobbies()), 4);
        accumulate(keywords, friend.getCity(), 3);
        accumulate(keywords, friend.getSpace(), 2);
        accumulate(keywords, friend.getCloseness().label(), 2);
        accumulate(keywords, friend.getCadence().label(), 2);
        accumulate(keywords, friend.getEmail(), 2);
        accumulate(keywords, friend.getPhone(), 2);
        accumulate(keywords, friend.getHandle(), 2);
        accumulate(keywords, friend.getPronouns(), 1);
        accumulate(keywords, friend.getFriendsSince(), 1);
        for (Note note : notes) {
            accumulate(keywords, note.getTitle(), 3);
            accumulate(keywords, note.getBody(), 2);
            accumulate(keywords, String.join(" ", note.getTags()), 3);
        }
        return keywords;
    }

    private static void accumulate(Map<String, Integer> keywords, String value, int weight) {
        for (String token : tokenize(value)) {
            keywords.merge(token, weight, Integer::sum);
        }
    }

    // ---------- Fuzzy matching ----------

    public static int levenshtein(String first, String second) {
        int[] previous = new int[second.length() + 1];
        int[] current = new int[second.length() + 1];

        for (int j = 0; j <= second.length(); j++) previous[j] = j;

        for (int i = 1; i <= first.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= second.length(); j++) {
                int cost = first.charAt(i - 1) == second.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost
                );
            }
            int[] temp = previous;
            previous = current;
            current = temp;
        }
        return previous[second.length()];
    }
}
