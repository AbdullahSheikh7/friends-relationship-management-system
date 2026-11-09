package frmSystem.pages.views;

import frmSystem.db.daos.ContactEntryDAO;
import frmSystem.db.daos.FriendDAO;
import frmSystem.db.models.ContactEntry;
import frmSystem.db.models.ContactType;
import frmSystem.db.models.Friend;
import frmSystem.pages.RootLayout;
import frmSystem.ui.UtilsUI;

import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Contact History: every logged catch-up on a timeline, with stats and a quick log form. */
public class ContactHistoryView {
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.ENGLISH);

    private final RootLayout app;
    private final VBox timeline = new VBox(12);
    private final HBox typeChips = UtilsUI.row(6);
    private final TextField search = new TextField();
    private final ComboBox<Friend> friendBox = new ComboBox<>();
    private final ComboBox<String> periodBox = new ComboBox<>();
    private ContactType type;

    public ContactHistoryView(RootLayout app) {
        this.app = app;
    }

    private static String typeKind(ContactType type) {
        return type == ContactType.VOICE_VIDEO ? "pill-warn" : "pill-mint";
    }

    public Node build() {
        List<ContactEntry> all = ContactEntryDAO.findAll();
        HBox crumbs = UtilsUI.row(6, UtilsUI.label("FRM", "eyebrow", "c-primary"), UtilsUI.icon(MaterialDesignC.CHEVRON_RIGHT, 10, "ic-muted"), UtilsUI.label("CONTACT HISTORY", "eyebrow", "muted"), UtilsUI.bullet("Timeline", "pill", "pill-strong"));
        VBox title = UtilsUI.col(4, crumbs, UtilsUI.label("Contact History & Catch-up Log", "h1"),
                UtilsUI.wrapped("A chronological timeline of encounters, phone calls, coffee meetups, and thoughtful exchanges across all your friendships.", "muted"));
        title.setMaxWidth(560);
        Button log = UtilsUI.button("Log Quick Contact", "btn-primary");
        log.setOnAction(_ -> app.openLogContact(null));
        HBox head = UtilsUI.row(10, title, UtilsUI.spacer(), log);
        head.setAlignment(Pos.TOP_LEFT);

        search.setPromptText("Search conversations, venues, notes");
        search.textProperty().addListener((_, _, _) -> renderTimeline());
        HBox.setHgrow(search, Priority.ALWAYS);
        HBox searchBox = UtilsUI.styled(UtilsUI.row(0, search), "field-box");
        HBox.setHgrow(searchBox, Priority.ALWAYS);
        renderTypeChips(all);
        friendBox.getItems().setAll(FriendDAO.findAll().stream().filter(f -> !f.isArchived()).sorted(Comparator.comparing(Friend::getName)).toList());
        friendBox.getItems().addFirst(null);
        friendBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Friend friend) {
                return friend == null ? "All Friends" : friend.getName();
            }

            @Override
            public Friend fromString(String text) {
                return null;
            }
        });
        friendBox.getSelectionModel().selectFirst();
        friendBox.setOnAction(_ -> renderTimeline());
        periodBox.getItems().setAll("Past 3 Months", "Past Year", "All Time");
        periodBox.getSelectionModel().select(0);
        periodBox.setOnAction(_ -> renderTimeline());
        VBox filters = UtilsUI.card(0, UtilsUI.row(8, searchBox, typeChips, friendBox, periodBox));

        // WIP: enable once LogContactDialog.form(RootLayout, …) exists.
        VBox quick = UtilsUI.card(10, UtilsUI.row(8, UtilsUI.label("Quick Catch-up Log", "h4"), UtilsUI.spacer(), UtilsUI.label("2-Min Entry", "tiny", "semi")),
                UtilsUI.wrapped("Quickly record an unexpected encounter or warm phone call before details slip away.", "small", "muted")
                /* , LogContactDialog.form(app, null, () -> { }) */);
        VBox right = new VBox(14, quick, cadenceCard(all), spotsCard(all), exportCard());
        right.setMinWidth(300);
        right.setMaxWidth(300);
        timeline.setMinWidth(0);
        HBox.setHgrow(timeline, Priority.ALWAYS);
        renderTimeline();
        return RootLayout.scrollPage(UtilsUI.styled(new VBox(14, head, stats(all), filters, new HBox(16, timeline, right)), "page"));
    }

    private Node stats(List<ContactEntry> all) {
        LocalDate monthAgo = LocalDate.now().minusDays(30);
        long recent = all.stream().filter(c -> c.getDate().isAfter(monthAgo)).count();
        long inPerson = all.stream().filter(c -> c.getType() == ContactType.IN_PERSON).count();
        String topSpot = favouriteSpots(all).keySet().stream().findFirst().orElse("Not enough data yet");
        List<Friend> active = FriendDAO.findAll().stream().filter(f -> !f.isArchived()).toList();
        long calm = active.stream().filter(f -> f.daysUntilDue() >= 0).count();
        long pace = active.isEmpty() ? 0 : Math.round(calm * 100.0 / active.size());

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.add(stat("TOTAL ENCOUNTERS", all.size() + "", "+" + recent + " in 30 days", "Recorded touchpoints across your friends", null), 0, 0);
        double share = all.isEmpty() ? 0 : inPerson / (double) all.size();
        grid.add(stat("IN-PERSON MEETUPS", inPerson + "", "", Math.round(share * 100) + "% of all encounters", share), 1, 0);
        VBox spot = UtilsUI.col(6, UtilsUI.wrapped(topSpot, "h3"), UtilsUI.label("Your most visited place", "small", "muted"));
        grid.add(statShell("MOST CONNECTED SPACE", spot), 2, 0);
        grid.add(stat("UNHURRIED PACE", pace + "%", "Healthy Rhythm", "Zero pressure • Consistent mutual care", null), 3, 0);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(25);
            grid.getColumnConstraints().add(column);
        }
        return grid;
    }

    private Node statShell(String title, Node body) {
        HBox top = UtilsUI.row(6, UtilsUI.label(title, "eyebrow", "muted"), UtilsUI.spacer());
        VBox card = UtilsUI.card(10, top, body);
        card.setMaxHeight(Double.MAX_VALUE);
        return card;
    }

    private Node stat(String title, String number, String badge, String caption, Double progress) {
        VBox body = UtilsUI.col(6, UtilsUI.row(8, UtilsUI.label(number, "stat-number"), UtilsUI.label(badge, "small", "semi", "c-primary")), UtilsUI.wrapped(caption, "small", "muted"));
        if (progress != null) {
            Region track = UtilsUI.styled(new Region(), "step-bar");
            Region fill = UtilsUI.styled(new Region(), "step-bar", "on");
            fill.prefWidthProperty().bind(track.widthProperty().multiply(progress));
            fill.setMaxWidth(Region.USE_PREF_SIZE);
            StackPane bar = new StackPane(track, fill);
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            body.getChildren().add(bar);
        }
        return statShell(title, body);
    }

    private void renderTypeChips(List<ContactEntry> all) {
        typeChips.getChildren().setAll(typeChip("All Types", null, all.size()));
        for (ContactType option : List.of(ContactType.IN_PERSON, ContactType.VOICE_VIDEO)) {
            long count = all.stream().filter(c -> c.getType() == option).count();
            typeChips.getChildren().add(typeChip(option == ContactType.IN_PERSON ? "In-person" : "Voice / Video", option, count));
        }
    }

    private Button typeChip(String text, ContactType value, long count) {
        Button chip = UtilsUI.button(text + "  " + count, "chip");
        if (value == type) {
            chip.getStyleClass().add("on");
        }
        chip.setOnAction(_ -> {
            type = value;
            renderTypeChips(ContactEntryDAO.findAll());
            renderTimeline();
        });
        return chip;
    }

    private void renderTimeline() {
        String query = search.getText().trim().toLowerCase(Locale.ENGLISH);
        Friend friend = friendBox.getValue();
        int period = periodBox.getSelectionModel().getSelectedIndex();
        LocalDate since = switch (period) {
            case 0 -> LocalDate.now().minusMonths(3);
            case 1 -> LocalDate.now().minusYears(1);
            default -> LocalDate.MIN;
        };
        List<ContactEntry> entries = ContactEntryDAO.findAll().stream()
                .filter(c -> type == null || c.getType() == type)
                .filter(c -> friend == null || c.getFriendId() == friend.getId())
                .filter(c -> !c.getDate().isBefore(since))
                .filter(c -> query.isEmpty() || (c.getTitle() + " " + c.getSummary() + " " + c.getLocation()).toLowerCase(Locale.ENGLISH).contains(query))
                .toList();
        timeline.getChildren().clear();
        if (entries.isEmpty()) {
            timeline.getChildren().add(UtilsUI.card(6, UtilsUI.label("No catch-ups found.", "h4"), UtilsUI.label("Try a longer time period, or log a new one.", "muted")));
            return;
        }
        Map<YearMonth, List<ContactEntry>> byMonth = new LinkedHashMap<>();
        entries.forEach(entry -> byMonth.computeIfAbsent(YearMonth.from(entry.getDate()), _ -> new ArrayList<>()).add(entry));
        byMonth.forEach((month, list) -> {
            Label monthPill = UtilsUI.pill(month.format(MONTH), "pill-strong", "pill-big");
            monthPill.setStyle("-fx-font-family: 'Plus Jakarta Sans SemiBold'; -fx-font-size: 16px;");
            timeline.getChildren().add(UtilsUI.row(8, monthPill, UtilsUI.spacer(), UtilsUI.label(list.size() + " LOGGED ENCOUNTERS", "eyebrow", "muted")));
            list.forEach(entry -> timeline.getChildren().add(entryCard(app, entry)));
        });
    }

    /** One entry on the timeline. Also shown on a friend's profile. */
    static Node entryCard(RootLayout app, ContactEntry entry) {
        Friend friend = FriendDAO.findById(entry.getFriendId()).orElse(null);
        HBox top = UtilsUI.row(10);
        if (friend != null) {
            top.getChildren().add(UtilsUI.avatar(friend, 42));
        }
        HBox nameRow = UtilsUI.row(8, UtilsUI.label(friend == null ? "Unknown" : friend.getName(), "h4"), UtilsUI.pill(entry.getType().label(), typeKind(entry.getType())));
        HBox dateRow = UtilsUI.row(6, UtilsUI.label(DAY.format(entry.getDate()), "small", "muted"));
        if (!entry.getDuration().isEmpty()) {
            dateRow.getChildren().addAll(UtilsUI.icon(MaterialDesignC.CIRCLE_SMALL, 8, "ic-muted"), UtilsUI.label(entry.getDuration(), "small", "muted"));
        }
        top.getChildren().addAll(UtilsUI.col(2, nameRow, dateRow));

        VBox card = UtilsUI.card(8, top, UtilsUI.wrapped(entry.getTitle(), "h4"));
        if (!entry.getLocation().isEmpty()) {
            card.getChildren().add(UtilsUI.row(6, UtilsUI.label(entry.getLocation(), "small", "muted")));
        }
        if (!entry.getSummary().isEmpty()) {
            card.getChildren().add(UtilsUI.styled(new VBox(UtilsUI.wrapped(entry.getSummary())), "tint"));
        }
        HBox foot = UtilsUI.row(6);
        entry.getTags().forEach(tag -> foot.getChildren().add(UtilsUI.pill("#" + tag, "pill-strong")));
        if (friend != null) {
            Button view = UtilsUI.button("View Full Profile", "link");
            view.setOnAction(_ -> app.showFriendProfile(friend.getId()));
            foot.getChildren().addAll(UtilsUI.spacer(), view);
        }
        card.getChildren().add(foot);
        return card;
    }

    private Node cadenceCard(List<ContactEntry> all) {
        Map<ContactType, Long> counts = new EnumMap<>(ContactType.class);
        for (ContactType option : ContactType.values()) {
            counts.put(option, all.stream().filter(c -> c.getType() == option).count());
        }
        String[] colors = {"-frm-primary", "-frm-chart-2", "-frm-chart-3", "-frm-danger"};
        HBox bar = UtilsUI.row(2);
        VBox legend = new VBox(6);
        int i = 0;
        for (ContactType option : ContactType.values()) {
            long count = counts.get(option);
            double share = all.isEmpty() ? 0 : count / (double) all.size();
            Region part = new Region();
            part.setMinHeight(10);
            part.setStyle("-fx-background-color: " + colors[i] + "; -fx-background-radius: 3;");
            HBox.setHgrow(part, Priority.ALWAYS);
            part.prefWidthProperty().bind(bar.widthProperty().multiply(share));
            part.setMinWidth(0);
            bar.getChildren().add(part);
            Region dot = new Region();
            dot.setMinSize(9, 9);
            dot.setMaxSize(9, 9);
            dot.setStyle("-fx-background-color: " + colors[i] + "; -fx-background-radius: 5;");
            legend.getChildren().add(UtilsUI.row(8, dot, UtilsUI.label(option.groupLabel(), "small"), UtilsUI.spacer(),
                    UtilsUI.label(Math.round(share * 100) + "% (" + count + ")", "small", "semi")));
            i++;
        }
        return UtilsUI.card(10, UtilsUI.row(8, UtilsUI.label("Interaction Cadence", "h4"), UtilsUI.spacer(), UtilsUI.label("Healthy Balance", "tiny", "semi", "c-primary")),
                UtilsUI.wrapped("Distribution across communication channels.", "small", "muted"), bar, legend);
    }

    private Map<String, Long> favouriteSpots(List<ContactEntry> all) {
        Map<String, Long> counts = all.stream().filter(c -> !c.getLocation().isEmpty())
                .collect(Collectors.groupingBy(ContactEntry::getLocation, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(4)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, _) -> a, LinkedHashMap::new));
    }

    private Node spotsCard(List<ContactEntry> all) {
        VBox card = UtilsUI.card(10, UtilsUI.row(8, UtilsUI.label("Favorite Meetup Spots", "h4"), UtilsUI.spacer()));
        Function<Long, String> visits = count -> count + (count == 1 ? " visit" : " visits");
        favouriteSpots(all).forEach((spot, count) -> card.getChildren().add(UtilsUI.row(8,
                UtilsUI.wrapped(spot, "small", "semi"), UtilsUI.spacer(), UtilsUI.pill(visits.apply(count), "pill-strong"))));
        return card;
    }

    private Node exportCard() {
        Button csv = UtilsUI.button("CSV Log", "btn-surface");
        csv.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(csv, Priority.ALWAYS);
        csv.setOnAction(_ -> Exports.contactsCsv(app));
        Button json = UtilsUI.button("JSON", "btn-surface");
        json.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(json, Priority.ALWAYS);
        json.setOnAction(_ -> Exports.contactsJson(app));
        return UtilsUI.styled(UtilsUI.col(10, UtilsUI.row(8, UtilsUI.label("Export Catch-up Log", "h4")),
                UtilsUI.wrapped("Export your complete relationship journal to plain JSON or CSV format for personal archiving.", "small", "muted"),
                UtilsUI.row(8, csv, json)), "tint-card");
    }
}