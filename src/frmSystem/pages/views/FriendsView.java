package frmSystem.pages.views;

import frmSystem.db.daos.FriendDAO;
import frmSystem.db.daos.NoteDAO;
import frmSystem.db.models.Friend;
import frmSystem.db.models.FriendStatus;
import frmSystem.db.models.Note;
import frmSystem.pages.RootLayout;
import frmSystem.ui.UtilsUI;

import org.kordamp.ikonli.materialdesign2.MaterialDesignS;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Friends Directory: table and card views with search, filters, bulk actions, and paging. */
public class FriendsView {
    private static final int PAGE_SIZE = 8;
    private static final List<String> SPACES = List.of("Work", "School / Uni", "Neighborhood", "Family Circle", "Online & Gaming");

    private final RootLayout app;
    private final VBox results = new VBox(12);
    private final HBox spaceChips = UtilsUI.row(6);
    private final HBox viewTabs = UtilsUI.styled(UtilsUI.row(2), "seg");
    private final HBox groupTabs = UtilsUI.styled(UtilsUI.row(2), "seg");
    private final Set<Long> selected = new HashSet<>();
    private final TextField search = new TextField();
    private final ComboBox<String> closenessBox = new ComboBox<>();
    private final ComboBox<String> cadenceBox = new ComboBox<>();
    private final ComboBox<String> statusBox = new ComboBox<>();
    private final ComboBox<String> sortBox = new ComboBox<>();

    private String space;
    private String group = "Active";
    private boolean cards;
    private int pageIndex;

    public FriendsView(RootLayout app, String initialSearch) {
        this.app = app;
        search.setText(initialSearch);
    }

    public Node build() {
        long activeCount = friends().stream().filter(f -> !f.isArchived()).count();
        long archivedCount = friends().size() - activeCount;
        HBox crumbs = UtilsUI.row(8, UtilsUI.label("DIRECTORY", "eyebrow", "c-primary"),
                UtilsUI.pill(activeCount + " active", "pill-strong"), UtilsUI.pill(archivedCount + " archived", "pill-strong"));
        VBox title = UtilsUI.col(4, crumbs, UtilsUI.label("Friends Directory", "h1"),
                UtilsUI.label("Browse, organize, and gently stay connected with the people in your life.", "muted"));
        renderViewTabs();
        Button export = UtilsUI.button("Export", "btn-surface");
        export.setOnAction(_ -> Exports.friendsCsv(app));
        Button add = UtilsUI.button("Add Friend", "btn-primary");
        add.setOnAction(_ -> app.openAddFriend());
        HBox head = UtilsUI.row(10, title, UtilsUI.spacer(), viewTabs, export, add);
        head.setFillHeight(false);

        search.setPromptText("Filter by name, notes, hobbies, job");
        search.setPrefColumnCount(10);
        search.textProperty().addListener((_, _, _) -> resetAndRender());
        HBox.setHgrow(search, Priority.ALWAYS);
        HBox searchBox = UtilsUI.styled(UtilsUI.row(6, UtilsUI.label("Filter:", "ic-muted"), search), "field-box");
        HBox.setHgrow(searchBox, Priority.ALWAYS);
        combo(closenessBox, List.of("Closeness: All", "Very close", "Close friend", "Casual"));
        combo(cadenceBox, List.of("Cadence: Any", "Weekly", "Bi-weekly", "Monthly", "Every 3 months", "Every 6 months"));
        combo(statusBox, List.of("Status: All", "Needs hello", "Due soon", "On track"));
        combo(sortBox, List.of("Sort: Due date (soonest)", "Sort: Name", "Sort: Last contacted"));
        HBox filters = UtilsUI.row(8, searchBox, closenessBox, cadenceBox, statusBox, sortBox);

        renderGroupTabs();
        renderSpaceChips();
        FlowPane chips = new FlowPane(12, 8, spaceChips, groupTabs);
        VBox controls = UtilsUI.card(12, filters, chips);

        render();
        return RootLayout.scrollPage(UtilsUI.styled(new VBox(14, head, controls, results), "page"));
    }

    private void combo(ComboBox<String> box, List<String> options) {
        box.getItems().setAll(options);
        box.getSelectionModel().selectFirst();
        box.setOnAction(_ -> resetAndRender());
    }

    private void resetAndRender() {
        pageIndex = 0;
        render();
    }

    private List<Friend> friends() {
        return FriendDAO.findAll();
    }

    private void renderViewTabs() {
        Button table = UtilsUI.button("Table", "seg-btn");
        Button card = UtilsUI.button("Cards", "seg-btn");
        (cards ? card : table).getStyleClass().add("on");
        table.setOnAction(_ -> {
            cards = false;
            renderViewTabs();
            render();
        });
        card.setOnAction(_ -> {
            cards = true;
            renderViewTabs();
            render();
        });
        viewTabs.getChildren().setAll(table, card);
    }

    private void renderGroupTabs() {
        long active = friends().stream().filter(f -> !f.isArchived()).count();
        long favorites = friends().stream().filter(f -> !f.isArchived() && f.isFavorite()).count();
        long archived = friends().stream().filter(Friend::isArchived).count();
        groupTabs.getChildren().setAll(groupTab("Active", active), groupTab("Favorites", favorites), groupTab("Archived", archived));
    }

    private Button groupTab(String name, long count) {
        Button button = UtilsUI.button(name + " (" + count + ")", "seg-btn");
        if (name.equals(group)) {
            button.getStyleClass().add("on");
        }
        button.setOnAction(_ -> {
            group = name;
            renderGroupTabs();
            renderSpaceChips();
            resetAndRender();
        });
        return button;
    }

    private void renderSpaceChips() {
        spaceChips.getChildren().clear();
        spaceChips.getChildren().add(spaceChip("All Spaces", null));
        SPACES.forEach(s -> spaceChips.getChildren().add(spaceChip(s, s)));
    }

    private Button spaceChip(String text, String value) {
        long count = friends().stream().filter(f -> !f.isArchived() && (value == null || value.equals(f.getSpace()))).count();
        Button chip = UtilsUI.button(text + "  " + count, "chip");
        if (java.util.Objects.equals(value, space)) {
            chip.getStyleClass().add("on");
        }
        chip.setOnAction(_ -> {
            space = value;
            renderSpaceChips();
            resetAndRender();
        });
        return chip;
    }

    private boolean matchesSearch(Friend f, String query) {
        if (query.isEmpty()) {
            return true;
        }
        String q = query.toLowerCase(Locale.ENGLISH);
        if ((f.getName() + " " + f.getOccupation() + " " + f.getHeadline() + " " + String.join(" ", f.getHobbies()))
                .toLowerCase(Locale.ENGLISH).contains(q)) {
            return true;
        }
        for (Note note : NoteDAO.findByFriend(f.getId())) {
            if ((note.getTitle() + " " + note.getBody()).toLowerCase(Locale.ENGLISH).contains(q)) {
                return true;
            }
        }
        return false;
    }

    private String statusName(FriendStatus status) {
        return switch (status) {
            case NEEDS_HELLO -> "Needs hello";
            case DUE_SOON -> "Due soon";
            case ON_TRACK -> "On track";
        };
    }

    private List<Friend> filtered() {
        String query = search.getText().trim();
        Comparator<Friend> order = switch (sortBox.getSelectionModel().getSelectedIndex()) {
            case 1 -> Comparator.comparing(Friend::getName);
            case 2 -> Comparator.comparing(Friend::getLastContacted).reversed();
            default -> Comparator.comparing(Friend::daysUntilDue);
        };
        return friends().stream()
                .filter(f -> switch (group) {
                    case "Favorites" -> f.isFavorite() && !f.isArchived();
                    case "Archived" -> f.isArchived();
                    default -> !f.isArchived();
                })
                .filter(f -> space == null || f.getSpace().equals(space))
                .filter(f -> closenessBox.getSelectionModel().getSelectedIndex() <= 0 || f.getCloseness().label().equals(closenessBox.getValue()))
                .filter(f -> cadenceBox.getSelectionModel().getSelectedIndex() <= 0 || f.getCadence().label().equals(cadenceBox.getValue()))
                .filter(f -> statusBox.getSelectionModel().getSelectedIndex() <= 0 || statusName(f.status()).equals(statusBox.getValue()))
                .filter(f -> matchesSearch(f, query))
                .sorted(order)
                .toList();
    }

    private void render() {
        List<Friend> all = filtered();
        int pages = Math.max(1, (int) Math.ceil(all.size() / (double) PAGE_SIZE));
        pageIndex = Math.min(pageIndex, pages - 1);
        List<Friend> page = all.subList(pageIndex * PAGE_SIZE, Math.min(all.size(), (pageIndex + 1) * PAGE_SIZE));

        results.getChildren().clear();
        if (!selected.isEmpty()) {
            results.getChildren().add(bulkBar());
        }
        if (all.isEmpty()) {
            results.getChildren().add(UtilsUI.card(6, UtilsUI.label("No friends match.", "h4"), UtilsUI.label("Clear a filter, or add someone new.", "muted")));
            return;
        }
        results.getChildren().add(cards ? cardGrid(page) : table(page, all.size(), pages));
        if (cards) {
            results.getChildren().add(pager(all.size(), page.size(), pages));
        }
    }

    private Node bulkBar() {
        Button log = bulkButton("Log Contact");
        log.setOnAction(_ -> {
            selectedFriends().forEach(f -> app.logContactToday(f, _ -> { }));
            app.toast("Logged a check-in with " + selected.size() + " friends.");
            selected.clear();
            render();
        });
        Button snooze = bulkButton("Snooze");
        snooze.setOnAction(_ -> {
            selectedFriends().forEach(f -> FriendDAO.snooze(f.getId(), LocalDate.now().plusDays(7)));
            app.toast("Snoozed " + selected.size() + " friends for 1 week.");
            selected.clear();
            render();
        });
        Button close = UtilsUI.button("Close", "btn-plain");
        close.setOnAction(_ -> {
            selected.clear();
            render();
        });
        Button export = bulkButton("Export");
        export.setOnAction(_ -> Exports.friendsCsv(app));
        HBox bar = UtilsUI.row(10, UtilsUI.bullet(selected.size() + " friends selected", "semi"), UtilsUI.spacer(), log, snooze, export, close);
        bar.getStyleClass().add("dark-bar");
        return bar;
    }

    private Button bulkButton(String text) {
        return new Button(text);
    }

    private List<Friend> selectedFriends() {
        return friends().stream().filter(f -> selected.contains(f.getId())).toList();
    }

    private GridPane rowGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setAlignment(Pos.CENTER_LEFT);
        grid.setMinWidth(0);
        double[] widths = {4, 22, 12, 10, 9, 11, 16, 10};
        for (double width : widths) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(width);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }
        return grid;
    }

    private Node table(List<Friend> page, int total, int pages) {
        GridPane head = rowGrid();
        head.getStyleClass().add("table-head");
        CheckBox all = new CheckBox();
        all.setSelected(page.stream().allMatch(f -> selected.contains(f.getId())));
        all.setOnAction(_ -> {
            page.forEach(f -> {
                if (all.isSelected()) {
                    selected.add(f.getId());
                } else {
                    selected.remove(f.getId());
                }
            });
            render();
        });
        String[] titles = {"Friend", "Space", "Closeness", "Rhythm", "Last Contact", "Cadence Status", "Actions"};
        head.add(all, 0, 0);
        for (int i = 0; i < titles.length; i++) {
            head.add(UtilsUI.label(titles[i], "small", "semi"), i + 1, 0);
        }
        VBox box = new VBox(head);
        page.forEach(f -> box.getChildren().add(tableRow(f)));
        box.getChildren().add(UtilsUI.styled(pager(total, page.size(), pages), "table-row"));
        box.setMinWidth(0);
        box.setStyle("-fx-background-color: -frm-surface; -fx-background-radius: 10;");
        return box;
    }

    private Node tableRow(Friend f) {
        GridPane row = rowGrid();
        row.getStyleClass().add("table-row");
        row.setOnMouseClicked(_ -> app.showFriendProfile(f.getId()));

        CheckBox check = new CheckBox();
        check.setSelected(selected.contains(f.getId()));
        check.setOnAction(_ -> {
            if (check.isSelected()) {
                selected.add(f.getId());
            } else {
                selected.remove(f.getId());
            }
            render();
        });
        HBox nameLine = UtilsUI.row(5, UtilsUI.label(f.getName(), "h4"));
        if (f.isFavorite()) {
            nameLine.getChildren().add(UtilsUI.icon(MaterialDesignS.STAR, 16, "ic-star"));
        }
        Node who = UtilsUI.row(10, UtilsUI.avatar(f, 36), UtilsUI.col(1, nameLine, UtilsUI.label(f.getHeadline(), "tiny", "med", "muted")));
        Label status = UtilsUI.pill(UtilsUI.statusText(f), UtilsUI.statusKind(f.status()));
        status.setWrapText(true);
        status.setMinWidth(0);

        Button log = UtilsUI.button("Log", "btn-plain");
        log.setOnAction(_ -> app.logContactToday(f, _ -> app.refresh()));
        Button snooze = UtilsUI.button("Snooze", "btn-plain");
        snooze.setOnAction(_ -> Actions.showBelow(Actions.snoozeMenu(app, f), snooze));
        Button more = UtilsUI.button("More", "btn-plain");
        more.setOnAction(_ -> Actions.showBelow(Actions.friendMenu(app, f, app::refresh), more));
        HBox actions = UtilsUI.row(0, log, snooze, more);

        row.add(check, 0, 0);
        row.add(who, 1, 0);
        row.add(UtilsUI.pill(f.getSpace(), "pill-strong"), 2, 0);
        row.add(UtilsUI.pill(f.getCloseness().label(), UtilsUI.closenessKind(f)), 3, 0);
        row.add(UtilsUI.wrapped(f.getCadence().label(), "small"), 4, 0);
        row.add(UtilsUI.col(1, UtilsUI.label(UtilsUI.longDate(f.getLastContacted()), "small", "med"),
                UtilsUI.label(f.daysSinceContact() + " days ago", "tiny", "muted")), 5, 0);
        row.add(status, 6, 0);
        row.add(actions, 7, 0);
        return row;
    }

    private Node cardGrid(List<Friend> page) {
        FlowPane flow = new FlowPane(12, 12);
        page.forEach(f -> {
            Button open = UtilsUI.button("View Profile", "link");
            open.setOnAction(_ -> app.showFriendProfile(f.getId()));
            VBox card = UtilsUI.card(10, UtilsUI.row(10, UtilsUI.avatar(f, 44), UtilsUI.col(1, UtilsUI.label(f.getName(), "h4"), UtilsUI.label(f.getHeadline(), "tiny", "muted"))),
                    UtilsUI.row(6, UtilsUI.pill(f.getSpace(), "pill-strong"), UtilsUI.pill(f.getCloseness().label(), UtilsUI.closenessKind(f))),
                    UtilsUI.pill(UtilsUI.statusText(f), UtilsUI.statusKind(f.status())),
                    UtilsUI.row(6, Actions.logContactButton(app, f), UtilsUI.spacer(), open));
            card.setPrefWidth(320);
            flow.getChildren().add(card);
        });
        return flow;
    }

    private HBox pager(int total, int shown, int pages) {
        int first = total == 0 ? 0 : pageIndex * PAGE_SIZE + 1;
        Button previous = UtilsUI.button("Previous", "btn-plain");
        previous.setDisable(pageIndex == 0);
        previous.setOnAction(_ -> {
            pageIndex--;
            render();
        });
        Button next = UtilsUI.button("Next", "btn-plain");
        next.setDisable(pageIndex >= pages - 1);
        next.setOnAction(_ -> {
            pageIndex++;
            render();
        });
        return UtilsUI.row(8, UtilsUI.label("Showing " + first + " to " + (first + shown - 1) + " of " + total + " friends", "small", "muted"),
                UtilsUI.spacer(), previous, UtilsUI.label("Page " + (pageIndex + 1) + " of " + pages, "small", "med"), next);
    }
}