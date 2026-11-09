package frmSystem.pages.views;

import frmSystem.db.daos.FriendDAO;
import frmSystem.db.daos.NoteDAO;
import frmSystem.db.models.Friend;
import frmSystem.db.models.Note;
import frmSystem.pages.RootLayout;
import frmSystem.ui.UtilsUI;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Notes Hub: every note in one place, with search, tags, and a detail panel. */
public class NotesHubView {
    private final RootLayout app;
    private final VBox list = new VBox(10);
    private final VBox detail = UtilsUI.card(12);
    private final FlowPane tagChips = new FlowPane(8, 8);
    private final TextField search = new TextField();
    private final ComboBox<Friend> friendBox = new ComboBox<>();
    private final ComboBox<String> sortBox = new ComboBox<>();
    private final Label countLabel = UtilsUI.label("", "eyebrow", "muted");
    private String tag;
    private boolean flaggedOnly;
    private Note current;

    public NotesHubView(RootLayout app) {
        this.app = app;
    }

    public Node build() {
        Label crumbs = UtilsUI.label("FRM VAULT / NOTES & OBSERVATIONS", "eyebrow", "c-primary");
        VBox title = UtilsUI.col(4, crumbs, UtilsUI.label("Notes Hub", "h1"),
                UtilsUI.wrapped("All your friend observations, gift ideas, life milestones, and flagged conversation starters in one place.", "muted"));
        title.setMaxWidth(520);
        Button export = UtilsUI.button("Export Markdown", "btn-soft");
        export.setOnAction(_ -> Exports.notesMarkdown(app));
        Button add = UtilsUI.button("+ New Note", "btn-primary");
        add.setOnAction(_ -> new NoteDialog(app, null, null).open());
        HBox head = UtilsUI.row(10, title, UtilsUI.spacer(), export, add);
        head.setAlignment(Pos.BOTTOM_LEFT);

        search.setPromptText("Search notes and tags...");
        search.textProperty().addListener((_, _, _) -> render());
        HBox.setHgrow(search, Priority.ALWAYS);
        HBox searchBox = UtilsUI.styled(UtilsUI.row(0, search), "field-box");
        HBox.setHgrow(searchBox, Priority.ALWAYS);
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
        friendBox.setOnAction(_ -> render());
        sortBox.getItems().setAll("Sort: Newest first", "Sort: Oldest first");
        sortBox.getSelectionModel().selectFirst();
        sortBox.setOnAction(_ -> render());
        Button flagged = UtilsUI.button("Flagged Catchups", "chip");
        flagged.setOnAction(_ -> {
            flaggedOnly = !flaggedOnly;
            flagged.getStyleClass().remove("on");
            if (flaggedOnly) {
                flagged.getStyleClass().add("on");
            }
            render();
        });
        VBox controls = UtilsUI.card(12, UtilsUI.row(8, searchBox, friendBox, sortBox, flagged), tagChips);

        VBox left = new VBox(10, UtilsUI.row(8, countLabel, UtilsUI.spacer(), UtilsUI.bullet("LOCAL DATABASE", "eyebrow", "c-primary")), list);
        left.setMinWidth(380);
        left.setMaxWidth(400);
        HBox.setHgrow(detail, Priority.ALWAYS);
        detail.setMinWidth(0);
        render();
        HBox columns = new HBox(16, left, detail);
        columns.setAlignment(Pos.TOP_LEFT);
        columns.setFillHeight(false);
        return RootLayout.scrollPage(UtilsUI.styled(new VBox(14, head, controls, columns), "page"));
    }

    private void renderTags() {
        List<Note> notes = NoteDAO.findAll();
        Map<String, Integer> counts = new TreeMap<>();
        notes.forEach(n -> n.getTags().forEach(t -> counts.merge(t, 1, Integer::sum)));
        tagChips.getChildren().clear();
        Button all = UtilsUI.button("All Notes  " + notes.size(), "chip");
        if (tag == null) {
            all.getStyleClass().add("on");
        }
        all.setOnAction(_ -> {
            tag = null;
            render();
        });
        tagChips.getChildren().add(all);
        counts.forEach((name, count) -> {
            Button chip = UtilsUI.button("# " + name + "  " + count, "chip");
            if (name.equals(tag)) {
                chip.getStyleClass().add("on");
            }
            chip.setOnAction(_ -> {
                tag = name;
                render();
            });
            tagChips.getChildren().add(chip);
        });
    }

    private List<Note> filtered() {
        String query = search.getText().trim().toLowerCase(Locale.ENGLISH);
        Friend friend = friendBox.getValue();
        Comparator<Note> order = Comparator.comparing(Note::getDate);
        if (sortBox.getSelectionModel().getSelectedIndex() != 1) {
            order = order.reversed();
        }
        return NoteDAO.findAll().stream()
                .filter(n -> friend == null || n.getFriendId() == friend.getId())
                .filter(n -> tag == null || n.getTags().contains(tag))
                .filter(n -> !flaggedOnly || n.isFlagged())
                .filter(n -> query.isEmpty() || (n.getTitle() + " " + n.getBody() + " " + String.join(" ", n.getTags())).toLowerCase(Locale.ENGLISH).contains(query))
                .sorted(order)
                .toList();
    }

    private void render() {
        renderTags();
        List<Note> notes = filtered();
        countLabel.setText("SHOWING " + notes.size() + " ENTRIES");
        if (current == null || !notes.contains(current)) {
            current = notes.isEmpty() ? null : notes.get(0);
        }
        list.getChildren().clear();
        notes.forEach(n -> list.getChildren().add(noteCard(n)));
        if (notes.isEmpty()) {
            list.getChildren().add(UtilsUI.card(6, UtilsUI.label("No notes found.", "h4"), UtilsUI.label("Try another tag or start a new note.", "muted")));
        }
        renderDetail();
    }

    private String friendName(Note note) {
        return FriendDAO.findById(note.getFriendId()).map(Friend::getName).orElse("Unknown");
    }

    private Node noteCard(Note note) {
        HBox top = UtilsUI.row(8);
        FriendDAO.findById(note.getFriendId()).ifPresent(f -> top.getChildren().addAll(UtilsUI.avatar(f, 24), UtilsUI.label(f.getName(), "med")));
        top.getChildren().add(UtilsUI.spacer());
        top.getChildren().add(note.isFlagged() ? UtilsUI.pill("Catchup", "pill-danger") : UtilsUI.label("General note", "tiny", "semi", "muted"));
        HBox tags = UtilsUI.row(6);
        note.getTags().stream().limit(3).forEach(t -> tags.getChildren().add(UtilsUI.pill("#" + t, "pill-strong")));
        tags.getChildren().addAll(UtilsUI.spacer(), UtilsUI.label(UtilsUI.longDate(note.getDate()), "tiny", "semi"));
        Label preview = UtilsUI.wrapped(note.getBody().length() > 110 ? note.getBody().substring(0, 110) + "..." : note.getBody(), "small", "muted");
        VBox card = UtilsUI.card(6, top, UtilsUI.wrapped(note.getTitle(), "h4"), preview, tags);
        card.setStyle("-fx-cursor: hand;");
        if (note == current) {
            card.getStyleClass().add("accent-primary");
        }
        card.setOnMouseClicked(_ -> {
            current = note;
            render();
        });
        return card;
    }

    private void renderDetail() {
        detail.getChildren().clear();
        if (current == null) {
            detail.getChildren().add(UtilsUI.label("Select a note to read it.", "muted"));
            return;
        }
        Note note = current;
        Button copy = UtilsUI.button("Copy", "btn-plain");
        copy.setStyle("-fx-font-size: 11px; -fx-padding: 2 8;");
        copy.setOnAction(_ -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(note.getTitle() + "\n\n" + note.getBody());
            Clipboard.getSystemClipboard().setContent(content);
            app.toast("Copied.");
        });
        Button delete = UtilsUI.button("Delete", "btn-plain");
        delete.setStyle("-fx-font-size: 11px; -fx-padding: 2 8;");
        delete.setOnAction(_ -> app.confirm("Delete this note?", "This note will be removed.", "Delete", () -> {
            NoteDAO.delete(note.getId());
            current = null;
            app.refresh();
        }));
        HBox crumbs = UtilsUI.row(6, UtilsUI.label("Notes Hub", "small", "muted"), UtilsUI.label("/", "faint"), UtilsUI.label(friendName(note), "small", "semi", "c-primary"),
                UtilsUI.label("/", "faint"), UtilsUI.label(UtilsUI.longDate(note.getDate()), "small", "muted"), UtilsUI.spacer(), copy, delete);
        detail.getChildren().add(crumbs);

        FriendDAO.findById(note.getFriendId()).ifPresent(f -> {
            CheckBox flag = new CheckBox("Flag for next catch-up");
            flag.setSelected(note.isFlagged());
            flag.setOnAction(_ -> {
                note.setFlagged(flag.isSelected());
                NoteDAO.update(note);
                render();
            });
            String birthday = f.getBirthday() == null ? "" : "  •  Next birthday: " + UtilsUI.shortDate(f.nextBirthday()) + " (" + f.turningAge() + ")";
            VBox who = UtilsUI.col(2, UtilsUI.row(8, UtilsUI.label(f.getName(), "h4"), UtilsUI.pill(f.getCloseness().label(), UtilsUI.closenessKind(f))),
                    UtilsUI.wrapped("Last connected: " + UtilsUI.ago(f.getLastContacted()) + birthday, "small", "muted"));
            HBox.setHgrow(who, Priority.ALWAYS);
            detail.getChildren().add(UtilsUI.styled(UtilsUI.row(10, UtilsUI.avatar(f, 42), who, flag), "tint"));
        });

        HBox tags = UtilsUI.row(6);
        note.getTags().forEach(t -> tags.getChildren().add(UtilsUI.pill("#" + t, "pill-strong")));
        tags.getChildren().add(UtilsUI.bullet("Edited " + UtilsUI.longDate(note.getDate()), "tiny", "muted"));
        Node actions = detailActions(note);
        detail.getChildren().addAll(UtilsUI.wrapped(note.getTitle(), "h2"), tags, UtilsUI.wrapped(note.getBody()), actions);
    }

    private Node detailActions(Note note) {
        Button profile = UtilsUI.button("View Profile", "btn-soft");
        profile.setOnAction(_ -> app.showFriendProfile(note.getFriendId()));
        Button log = UtilsUI.button("Log Encounter", "btn-soft");
        log.setOnAction(_ -> FriendDAO.findById(note.getFriendId()).ifPresent(app::openLogContact));
        Button edit = UtilsUI.button("Edit Note", "btn-soft");
        edit.setOnAction(_ -> new NoteDialog(app, note, null).open());
        return UtilsUI.row(8, profile, log, UtilsUI.spacer(), edit);
    }
}