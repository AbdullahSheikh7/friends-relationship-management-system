package frmSystem.pages.views;

import frmSystem.db.daos.FriendDAO;
import frmSystem.db.daos.GiftIdeaDAO;
import frmSystem.db.daos.NoteDAO;
import frmSystem.db.models.Friend;
import frmSystem.db.models.FriendStatus;
import frmSystem.db.models.Note;
import frmSystem.pages.RootLayout;
import frmSystem.ui.UtilsUI;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class DashboardView {
    private static final int PAGE_SIZE = 8;

    private final RootLayout app;
    private final VBox list = new VBox(12);
    private final HBox tabs = UtilsUI.styled(UtilsUI.row(2), "seg");
    private FriendStatus tab;
    private String space = "All";
    private String closeness = "All";
    private String cadence = "Any";
    private int shown = PAGE_SIZE;

    public DashboardView(RootLayout app) {
        this.app = app;
    }

    public Node build() {
        List<Friend> active = active();
        Node header = header();
        Node stats = stats(active);

        VBox left = new VBox(12, listControls(), list);
        HBox.setHgrow(left, Priority.ALWAYS);
        left.setMinWidth(0);
        VBox right = new VBox(14, birthdays(active), privacyCard());
        right.setMinWidth(330);
        right.setMaxWidth(330);
        right.setPrefWidth(330);

        renderList();
        VBox page = UtilsUI.styled(new VBox(16, header, stats, new HBox(16, left, right)), "page");
        return RootLayout.scrollPage(page);
    }

    private List<Friend> active() {
        return FriendDAO.findAll().stream().filter(f -> !f.isArchived()).toList();
    }

    private Node header() {
        String part = LocalTime.now().getHour() < 12 ? "morning" : LocalTime.now().getHour() < 17 ? "afternoon" : "evening";
        Node helloIcon = UtilsUI.icon(MaterialDesignW.WEATHER_SUNNY, 16, "ic-warn");
        HBox title = UtilsUI.row(10, helloIcon,
                UtilsUI.label("Good " + part + "! Here's where your friendships stand today.", "h2"));
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH));
        HBox sub = UtilsUI.row(8, UtilsUI.label(date, "muted"), UtilsUI.icon(MaterialDesignC.CIRCLE_SMALL, 8, "ic-muted"), UtilsUI.label("Take your time, no rush.", "italic", "c-primary"));
        Button log = UtilsUI.button("Quick Log Contact", "btn-surface");
        log.setOnAction(_ -> app.openLogContact(null));
        Button add = UtilsUI.button("Add Friend", "btn-primary");
        add.setOnAction(_ -> app.openAddFriend());
        return UtilsUI.row(10, UtilsUI.col(4, title, sub), UtilsUI.spacer(), log, add);
    }

    private long count(List<Friend> friends, FriendStatus status) {
        return friends.stream().filter(f -> f.status() == status).count();
    }

    private Node stats(List<Friend> active) {
        long spaces = active.stream().map(Friend::getSpace).distinct().count();
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.add(stat("GENTLE NUDGE", "c-danger", "Needs a hello", "tile-danger", MaterialDesignA.ALERT,
                count(active, FriendStatus.NEEDS_HELLO), "friends waiting", "Whenever you're free", "c-danger"), 0, 0);
        grid.add(stat("APPROACHING", "c-warn", "Due soon", "tile-warn", MaterialDesignT.TIMER,
                count(active, FriendStatus.DUE_SOON), "scheduled soon", "Coming up this week", "c-warn"), 1, 0);
        grid.add(stat("CONNECTED", "c-primary", "On track", "tile-mint", MaterialDesignC.CHECK,
                count(active, FriendStatus.ON_TRACK), "feeling close", "Recently caught up", "c-primary"), 2, 0);
        grid.add(stat("SANCTUARY", "muted", "Total friends", "tile-info", MaterialDesignA.ACCOUNT_GROUP,
                active.size(), "people cherished", "Spread across " + spaces + " active spaces", null), 3, 0);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(25);
            grid.getColumnConstraints().add(column);
        }
        return grid;
    }

    private Node stat(String eyebrow, String eyebrowColor, String title, String tileKind, Ikon symbol,
                      long number, String unit, String footnote, String numberColor) {
        VBox text = UtilsUI.col(2, UtilsUI.label(eyebrow, "eyebrow", eyebrowColor), UtilsUI.label(title, "h3"));
        StackPane tile = UtilsUI.tile(tileKind);
        tile.getChildren().add(UtilsUI.icon(symbol, 16));
        HBox top = UtilsUI.row(8, text, UtilsUI.spacer(), tile);
        Label big = UtilsUI.label(String.valueOf(number), "stat-number");
        if (numberColor != null) {
            big.getStyleClass().add(numberColor);
        }
        HBox value = UtilsUI.row(8, big, UtilsUI.label(unit, "muted"));
        HBox foot = UtilsUI.row(6, UtilsUI.icon(MaterialDesignC.CIRCLE_SMALL, 8, numberColor == null ? "ic-primary" : numberColor.replace("c-", "ic-")), UtilsUI.label(footnote, "small", "med"));
        VBox card = UtilsUI.card(10, top, value, foot);
        card.setMaxHeight(Double.MAX_VALUE);
        return card;
    }

    private Node listControls() {
        HBox title = UtilsUI.row(10, UtilsUI.label("Who to reach out to next", "h3"));
        renderTabs();
        HBox top = UtilsUI.row(10, title, UtilsUI.spacer(), tabs);

        ComboBox<String> spaceBox = filter(List.of("All", "Work", "School / Uni", "Neighborhood", "Family Circle", "Online & Gaming"), "Space: ", v -> space = v);
        ComboBox<String> closeBox = filter(List.of("All", "Very close", "Close friend", "Casual"), "Closeness: ", v -> closeness = v);
        ComboBox<String> cadenceBox = filter(List.of("Any", "Weekly", "Bi-weekly", "Monthly", "Every 3 months", "Every 6 months"), "Cadence: ", v -> cadence = v);
        Label filterLabel = UtilsUI.label("Filter:", "small", "muted");
        filterLabel.setMinWidth(Region.USE_PREF_SIZE);
        Label sortLabel = UtilsUI.label("Due date (soonest first)", "small", "semi");
        sortLabel.setMinWidth(Region.USE_PREF_SIZE);
        spaceBox.setPrefWidth(150);
        closeBox.setPrefWidth(150);
        cadenceBox.setPrefWidth(150);
        HBox filters = UtilsUI.row(8, filterLabel, spaceBox, closeBox, cadenceBox, UtilsUI.spacer(), sortLabel);
        return UtilsUI.card(14, top, filters);
    }

    private ComboBox<String> filter(List<String> options, String prefix, Consumer<String> setter) {
        ComboBox<String> box = new ComboBox<>();
        box.getItems().setAll(options);
        box.getSelectionModel().selectFirst();
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(String value) {
                return value == null ? "" : prefix + value;
            }

            @Override
            public String fromString(String value) {
                return value;
            }
        });
        box.setOnAction(_ -> {
            setter.accept(box.getValue());
            shown = PAGE_SIZE;
            renderList();
        });
        return box;
    }

    private void renderTabs() {
        List<Friend> active = active();
        tabs.getChildren().setAll(
                tabButton("All (" + active.size() + ")", null),
                tabButton("Needs hello (" + count(active, FriendStatus.NEEDS_HELLO) + ")", FriendStatus.NEEDS_HELLO),
                tabButton("Due soon (" + count(active, FriendStatus.DUE_SOON) + ")", FriendStatus.DUE_SOON),
                tabButton("On track (" + count(active, FriendStatus.ON_TRACK) + ")", FriendStatus.ON_TRACK));
    }

    private Button tabButton(String text, FriendStatus status) {
        Button button = UtilsUI.button(text, "seg-btn");
        if (status == tab) {
            button.getStyleClass().add("on");
        }
        button.setOnAction(_ -> {
            tab = status;
            shown = PAGE_SIZE;
            renderTabs();
            renderList();
        });
        return button;
    }

    private List<Friend> filtered() {
        return active().stream()
                .filter(f -> tab == null || f.status() == tab)
                .filter(f -> space.equals("All") || f.getSpace().equals(space))
                .filter(f -> closeness.equals("All") || f.getCloseness().label().equals(closeness))
                .filter(f -> cadence.equals("Any") || f.getCadence().label().equals(cadence))
                .sorted(Comparator.comparing(Friend::daysUntilDue))
                .toList();
    }

    private void renderList() {
        List<Friend> friends = filtered();
        List<Node> nodes = new ArrayList<>();
        friends.stream().limit(shown).forEach(f -> nodes.add(friendCard(f)));
        if (friends.isEmpty()) {
            nodes.add(UtilsUI.card(6, UtilsUI.label("Nobody here right now.", "h4"), UtilsUI.label("Try a different filter, or enjoy the quiet.", "muted")));
        } else if (friends.size() > shown) {
            Button more = UtilsUI.button("Show more (" + (friends.size() - shown) + " remaining)", "btn-surface");
            more.setOnAction(_ -> {
                shown += PAGE_SIZE;
                renderList();
            });
            StackPane holder = new StackPane(more);
            nodes.add(holder);
        }
        list.getChildren().setAll(nodes);
    }

    private Node friendCard(Friend friend) {
        Label name = UtilsUI.label(friend.getName(), "h4");
        name.setStyle("-fx-font-size: 15px;");
        HBox nameRow = UtilsUI.row(8, name, UtilsUI.pill(friend.getSpace(), "pill-strong"), UtilsUI.pill(friend.getCloseness().label(), UtilsUI.closenessKind(friend)));
        HBox meta = UtilsUI.row(6, UtilsUI.label(friend.getCadence().label(), "small", "muted"),
                UtilsUI.icon(MaterialDesignC.CIRCLE_SMALL, 8, "ic-muted"), UtilsUI.label("Last reached out:", "small", "muted"),
                UtilsUI.label(UtilsUI.longDate(friend.getLastContacted()), "small", "semi"),
                UtilsUI.label("(" + friend.daysSinceContact() + " days ago)", "small", "muted"));
        HBox head = UtilsUI.row(12, UtilsUI.avatar(friend, 46), UtilsUI.col(2, nameRow, meta), UtilsUI.spacer(), statusPill(friend));

        List<Note> notes = NoteDAO.findByFriend(friend.getId());
        Node body;
        if (notes.isEmpty()) {
            body = UtilsUI.styled(UtilsUI.row(10,
                    UtilsUI.wrapped(friend.getHeadline(), "muted")), "tint");
        } else {
            Note note = notes.get(0);
            VBox text = UtilsUI.col(4, UtilsUI.wrapped("\"" + note.getBody() + "\""),
                    UtilsUI.bullet("Updated via Notes " + UtilsUI.ago(note.getDate()), "tiny", "med", "muted"));
            HBox.setHgrow(text, Priority.ALWAYS);
            HBox box = UtilsUI.row(10, UtilsUI.icon(MaterialDesignF.FORMAT_QUOTE_OPEN, 18, "ic-danger"), text);
            box.setAlignment(Pos.TOP_LEFT);
            body = UtilsUI.styled(box, "tint");
        }

        Button view = UtilsUI.button("View Profile", "btn-plain");
        view.setContentDisplay(ContentDisplay.RIGHT);
        view.setOnAction(_ -> app.showFriendProfile(friend.getId()));
        HBox actions = UtilsUI.row(8, Actions.logContactButton(app, friend), Actions.snoozeButton(app, friend), UtilsUI.spacer(), view);
        VBox card = UtilsUI.card(12, head, body, actions);
        card.setMinWidth(Region.USE_COMPUTED_SIZE);
        return card;
    }

    private Label statusPill(Friend friend) {
        String text = switch (friend.status()) {
            case NEEDS_HELLO -> "Time to say hi to " + friend.firstName();
            case DUE_SOON -> UtilsUI.statusText(friend);
            case ON_TRACK -> "On track • Due in " + friend.daysUntilDue() + " days";
        };
        return UtilsUI.bullet(text, "pill", UtilsUI.statusKind(friend.status()), "pill-big");
    }

    private Node birthdays(List<Friend> active) {
        StackPane cakeTile = UtilsUI.tile("tile-danger");
        cakeTile.getChildren().add(UtilsUI.icon(MaterialDesignC.CAKE_VARIANT, 18, "ic-danger"));
        HBox title = UtilsUI.row(10, cakeTile,
                UtilsUI.label("Upcoming Birthdays", "h4"), UtilsUI.spacer(), UtilsUI.pill("Next 30d", "pill-strong"));
        VBox cards = new VBox(10);
        active.stream().filter(f -> f.getBirthday() != null && f.daysUntilBirthday() <= 30)
                .sorted(Comparator.comparing(Friend::daysUntilBirthday)).limit(2).forEach(f -> {
                    Label when = UtilsUI.pill(UtilsUI.inTime(f.daysUntilBirthday()), f.daysUntilBirthday() <= 7 ? "pill-danger" : "pill-strong");
                    HBox top = UtilsUI.row(6, UtilsUI.label(f.getName(), "h4"), UtilsUI.spacer(), when);
                    VBox card = UtilsUI.styled(UtilsUI.col(5, top, UtilsUI.label(UtilsUI.shortDate(f.nextBirthday()) + "  •  Turning " + f.turningAge(), "small", "muted")), "tint");
                    GiftIdeaDAO.findByFriend(f.getId()).stream().findFirst().ifPresent(g ->
                            card.getChildren().add(UtilsUI.row(6, UtilsUI.wrapped("Gift idea: " + g.getItem(), "small", "c-danger"))));
                    cards.getChildren().add(card);
                });
        Button all = UtilsUI.button("View all birthdays", "link");
        all.setOnAction(_ -> app.show(RootLayout.Page.BIRTHDAYS));
        return UtilsUI.card(12, title, cards, all);
    }

    private Node privacyCard() {
        StackPane shieldTile = UtilsUI.tile("tile-primary");
        shieldTile.getChildren().add(UtilsUI.icon(MaterialDesignL.LOCK, 18, "ic-white"));
        HBox head = UtilsUI.row(10, shieldTile, UtilsUI.label("100% Local & Private", "h4"));
        Button backup = UtilsUI.button("Backup", "btn-surface");
        backup.setOnAction(_ -> app.show(RootLayout.Page.BACKUP));
        HBox foot = UtilsUI.row(6, UtilsUI.label("frm_vault.db", "small", "semi"), UtilsUI.spacer(), backup);
        return UtilsUI.styled(UtilsUI.col(10, head,
                UtilsUI.wrapped("Your relationship database is strictly stored in an offline SQLite file on this machine. No cloud telemetry, no algorithms, no guilt.", "small", "muted"),
                foot), "tint-card");
    }
}