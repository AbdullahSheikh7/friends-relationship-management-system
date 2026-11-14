package frmSystem.db;

import java.time.LocalDate;
import java.util.List;

import frmSystem.db.models.Cadence;
import frmSystem.db.models.Closeness;
import frmSystem.db.models.ContactEntry;
import frmSystem.db.models.ContactType;
import frmSystem.db.models.Friend;
import frmSystem.db.models.GiftIdea;
import frmSystem.db.models.Note;

/** One-shot demo seeder for testing (Ctrl+Shift+D). Inserts a controlled set of
 *  friends / notes / check-ins / gifts designed to exercise every search path:
 *  frequency ranking ("guitar" -> Sara > Nadia), prefix ("sar", "pho"),
 *  fuzzy ("fotography" -> Daniel), synonyms ("job" -> Marcus via "work"),
 *  digits ("555" -> everyone), plus every status / filter combination. */
public final class TestData {

    private TestData() {}

    private static int friends;
    private static int notes;
    private static int entries;
    private static int gifts;

    public static String seed() {
        friends = notes = entries = gifts = 0;

        LocalDate today = LocalDate.now();

        // 1 - frequency king: "guitar" appears in headline fields, hobbies, notes, tags
        Friend sara = friend(
                "Sara Lindqvist", "Indie guitarist and stream artist",
                "Paints album covers between streams", "Uppsala", "she/her",
                "April 2023", "sara.lindqvist@example.com", "555-0101", "@saraplays",
                "Online & Gaming", Closeness.VERY_CLOSE, Cadence.WEEKLY,
                today.minusDays(2), LocalDate.of(2007, 11, 24), true, false,
                List.of("Guitar", "Painting", "Gaming", "Karaoke"));
        note(sara, "Guitar progress", "Guitar practice four times this week. The new guitar strings sound warm. Next goal: one full song on guitar.", false, List.of("music", "guitar"));
        note(sara, "Concert wishlist", "Mentioned a limited vinyl concert recording she has wanted for years.", false, List.of("gift"));
        entry(sara, today.minusDays(2), ContactType.VOICE_VIDEO, "40 min", "Weekly catch-up", "Her place", "New guitar pedal unboxing and stream plans.", List.of("weekly"));
        entry(sara, today.minusDays(9), ContactType.CHAT, "", "Stream raid", "", "Raided her painting stream with the group.", List.of());
        gift(sara, "Vinyl record", 25, false);

        // 2 - synonym test: headline contains "work", so searching "job" matches via synonyms
        Friend marcus = friend(
                "Marcus Chen", "Head chef, proud of his work at Ember Kitchen",
                "Feeds half the neighborhood", "Seattle", "he/him",
                "September 2022", "marcus.chen@example.com", "555-0102", "@chefmarcus",
                "Work", Closeness.CLOSE, Cadence.BI_WEEKLY,
                today.minusDays(10), LocalDate.of(1996, 2, 19), true, false,
                List.of("Cooking", "Hiking", "Bouldering"));
        note(marcus, "Dinner party plan", "Cooking a tasting menu for the team. Maybe a cooking class together next month.", false, List.of("food", "work"));
        entry(marcus, today.minusDays(10), ContactType.IN_PERSON, "2 h", "Ember Kitchen visit", "Seattle", "Ate the seasonal menu, talked about a cooking collab.", List.of("food"));
        entry(marcus, today.minusDays(45), ContactType.CHAT, "15 min", "Quick hello", "", "Traded hiking trail recommendations.", List.of());
        gift(marcus, "Chef knife", 85, true);

        // 3 - due-soon status; "chess" frequency leader over Tomas
        Friend priya = friend(
                "Priya Sharma", "Med student, chess club captain",
                "Runs the anatomy study group", "Boston", "she/her",
                "January 2024", "priya.sharma@example.com", "555-0103", "@priyameds",
                "School / Uni", Closeness.CLOSE, Cadence.MONTHLY,
                today.minusDays(27), LocalDate.of(2005, 12, 26), false, false,
                List.of("Chess", "Reading", "Badminton"));
        note(priya, "Exam week survival", "Exam on anatomy Monday, second exam on Friday. Bring her coffee before the exam.", true, List.of("study", "exam", "chess"));
        note(priya, "Chess club practice", "Chess club practice after the exam week wraps up.", false, List.of("chess"));
        entry(priya, today.minusDays(27), ContactType.IN_PERSON, "1 h", "Study group", "Campus library", "Anatomy flashcards and badminton after.", List.of("study"));
        entry(priya, today.minusDays(70), ContactType.LETTER, "", "Postcard", "", "Sent a postcard from the trip.", List.of());
        gift(priya, "Chess clock", 40, false);

        // 4 - fuzzy test: searching "fotography" should still reach photography
        Friend daniel = friend(
                "Daniel Okafor", "Street photographer",
                "Shoots film on weekends", "Austin", "he/him",
                "June 2023", "daniel.okafor@example.com", "555-0104", "@danoka",
                "Neighborhood", Closeness.CASUAL, Cadence.EVERY_3_MONTHS,
                today.minusDays(20), LocalDate.of(1999, 1, 23), false, false,
                List.of("Photography", "Cycling", "Film"));
        note(daniel, "Photo walk", "Photo walk downtown planned. He edits photography prints in the garage studio.", false, List.of("photography"));
        entry(daniel, today.minusDays(20), ContactType.IN_PERSON, "3 h", "Garage studio visit", "Austin", "Printed two photography series for the spring fair.", List.of("art"));
        gift(daniel, "Film rolls", 18, false);

        // 5 - needs-hello status
        Friend elena = friend(
                "Elena Petrova", "Architect, minimalism enthusiast",
                "Renovating an old loft downtown", "Chicago", "she/her",
                "November 2021", "elena.petrova@example.com", "555-0105", "@elena.builds",
                "Work", Closeness.CLOSE, Cadence.MONTHLY,
                today.minusDays(40), null, false, false,
                List.of("Climbing", "Coffee", "Sketching"));
        note(elena, "Renovation ideas", "Talked about the loft renovation and the coffee roaster visit idea.", false, List.of("design"));
        entry(elena, today.minusDays(40), ContactType.VOICE_VIDEO, "25 min", "Loft walkthrough", "", "Video walkthrough of the renovation progress.", List.of());
        gift(elena, "Climbing chalk", 14, false);

        // 6 - archived friend; second "chess" player
        Friend tomas = friend(
                "Tomas Rivera", "Cousin and Sunday football rival",
                "Grills for the whole family", "El Paso", "he/him",
                "Forever", "tomas.rivera@example.com", "555-0106", "@tomasriv",
                "Family Circle", Closeness.VERY_CLOSE, Cadence.MONTHLY,
                today.minusDays(15), LocalDate.of(1994, 1, 4), false, true,
                List.of("Football", "Grilling", "Chess"));
        note(tomas, "Thanksgiving menu", "Grilling plan for Thanksgiving. Bring the chess board for after dinner.", false, List.of("family", "food"));
        entry(tomas, today.minusDays(15), ContactType.IN_PERSON, "4 h", "Family dinner", "El Paso", "Sunday football and grilling.", List.of("family"));
        gift(tomas, "Grill set", 60, false);

        // 7 - second "guitar" hit with lower frequency than Sara
        Friend nadia = friend(
                "Nadia Rahman", "Co-op gaming buddy",
                "Never misses a karaoke night", "Manchester", "she/her",
                "August 2024", "nadia.rahman@example.com", "555-0107", "@nadiaplays",
                "Online & Gaming", Closeness.CASUAL, Cadence.WEEKLY,
                today.minusDays(1), LocalDate.of(2001, 3, 17), false, false,
                List.of("Gaming", "Karaoke", "Guitar"));
        note(nadia, "Co-op night", "Co-op campaign finished. She wants to try the new guitar rhythm pack next.", false, List.of("gaming"));
        entry(nadia, today.minusDays(1), ContactType.CHAT, "50 min", "Co-op session", "", "Finished the campaign, planned the next one.", List.of("gaming"));
        gift(nadia, "Gaming mousepad", 30, false);

        return "Seeded " + friends + " friends, " + notes + " notes, "
                + entries + " check-ins, " + gifts + " gift ideas.";
    }

    // ---------- Builders ----------

    private static Friend friend(String name, String occupation, String headline, String city,
            String pronouns, String friendsSince, String email, String phone, String handle,
            String space, Closeness closeness, Cadence cadence, LocalDate lastContacted,
            LocalDate birthday, boolean favorite, boolean archived, List<String> hobbies) {
        Friend f = new Friend();
        f.setName(name);
        f.setOccupation(occupation);
        f.setHeadline(headline);
        f.setCity(city);
        f.setPronouns(pronouns);
        f.setFriendsSince(friendsSince);
        f.setEmail(email);
        f.setPhone(phone);
        f.setHandle(handle);
        f.setSpace(space);
        f.setCloseness(closeness);
        f.setCadence(cadence);
        f.setLastContacted(lastContacted);
        f.setBirthday(birthday);
        f.setFavorite(favorite);
        f.setArchived(archived);
        f.setHobbies(hobbies);
        f.insert();
        friends++;
        return f;
    }

    private static void note(Friend f, String title, String body, boolean flagged, List<String> tags) {
        Note n = new Note();
        n.setFriendId(f.getId());
        n.setTitle(title);
        n.setBody(body);
        n.setFlagged(flagged);
        n.setTags(tags);
        n.insert();
        notes++;
    }

    private static void entry(Friend f, LocalDate date, ContactType type, String duration,
            String title, String location, String summary, List<String> tags) {
        ContactEntry e = new ContactEntry();
        e.setFriendId(f.getId());
        e.setDate(date);
        e.setType(type);
        e.setDuration(duration);
        e.setTitle(title);
        e.setLocation(location);
        e.setSummary(summary);
        e.setTags(tags);
        e.insert();
        entries++;
    }

    private static void gift(Friend f, String item, int priceUsd, boolean purchased) {
        GiftIdea g = new GiftIdea();
        g.setFriendId(f.getId());
        g.setItem(item);
        g.setPriceUsd(priceUsd);
        g.setPurchased(purchased);
        g.insert();
        gifts++;
    }
}
