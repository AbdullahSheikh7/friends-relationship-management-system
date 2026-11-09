package frmSystem.pages.views;

import frmSystem.db.daos.ContactEntryDAO;
import frmSystem.db.daos.FriendDAO;
import frmSystem.db.daos.NoteDAO;
import frmSystem.db.models.Cadence;
import frmSystem.db.models.ContactEntry;
import frmSystem.db.models.Friend;
import frmSystem.db.models.Note;
import frmSystem.pages.RootLayout;
import frmSystem.ui.UtilsUI;

import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** One friend's full profile: rhythm, flagged topics, notes, history, and details. */
public class FriendProfileView {
    private static final List<String> NOTE_TAGS = List.of("Job / Career", "Family & Life", "Hobbies", "Health & Well-being", "Books & Media");
    private static final int NOTES_SHOWN = 3;

    private final RootLayout app;
    private final long friendId;
    private final HBox tabBar = UtilsUI.styled(UtilsUI.row(4), "seg");
    private final VBox tabContent = new VBox(14);
    private final List<String> chosenTags = new ArrayList<>();
    private Friend friend;
    private int tab;
    private boolean allNotes;

    public FriendProfileView(RootLayout app, long friendId) {
        this.app = app;
        this.friendId = friendId;
    }

    public Node build() {
        friend = FriendDAO.findById(friendId).orElse(null);
        if (friend == null) {
            return UtilsUI.styled(new VBox(UtilsUI.label("This friend is no longer here.", "h3")), "page");
        }
        Button directory = UtilsUI.button("Directory", "btn-plain");
        directory.setOnAction(_ -> app.show(RootLayout.Page.FRIENDS));
        HBox crumbs = UtilsUI.row(4, directory, UtilsUI.icon(MaterialDesignC.CHEVRON_RIGHT, 10, "ic-muted"), UtilsUI.label("Friends", "muted", "small"), UtilsUI.icon(MaterialDesignC.CHEVRON_RIGHT, 10, "ic-muted"),
                UtilsUI.label(friend.getName(), "small", "semi"), UtilsUI.spacer(),
                UtilsUI.bullet("Record ID #FRM-" + String.format("%04d", friend.getId()), "pill", "pill-strong"));

        renderTabs();
        VBox left = new VBox(14, tabBar, tabContent);
        HBox.setHgrow(left, Priority.ALWAYS);
        left.setMinWidth(0);
        VBox right = new VBox(14, rhythmCard(), infoCard(), hobbiesCard(), encountersCard());
        right.setMinWidth(300);
        right.setMaxWidth(300);
        renderTab();
        return RootLayout.scrollPage(UtilsUI.styled(new VBox(14, crumbs, headerCard(), new HBox(16, left, right)), "page"));
    }

    private Node headerCard() {
        StackPane photo = new StackPane(UtilsUI.label(friend.initials(), "h1", "avatar-label"));
        photo.setMinSize(84, 84);
        photo.setMaxSize(84, 84);
        photo.setStyle("-fx-background-color: linear-gradient(to bottom right, #9ff2e0, #cfeafc); -fx-background-radius: 14;");
        HBox nameRow = UtilsUI.row(8, UtilsUI.label(friend.getName(), "h1"));
        Button star = UtilsUI.button("", "icon-btn");
        star.setGraphic(UtilsUI.icon(friend.isFavorite() ? MaterialDesignS.STAR : MaterialDesignS.STAR_OUTLINE, 20, "ic-star"));
        star.setOnAction(_ -> {
            friend.setFavorite(!friend.isFavorite());
            FriendDAO.update(friend);
            app.refresh();
        });
        nameRow.getChildren().add(star);
        HBox pills = UtilsUI.row(6, UtilsUI.pill(friend.getSpace(), "pill-strong"),
                UtilsUI.pill(friend.getCloseness().label(), UtilsUI.closenessKind(friend)));
        HBox details = UtilsUI.row(8);
        if (!friend.getPronouns().isEmpty()) {
            details.getChildren().addAll(UtilsUI.label(friend.getPronouns(), "muted"), UtilsUI.icon(MaterialDesignC.CIRCLE_SMALL, 8, "ic-muted"));
        }
        details.getChildren().add(UtilsUI.label(friend.getOccupation(), "semi"));
        if (!friend.getCity().isEmpty()) {
            details.getChildren().addAll(UtilsUI.icon(MaterialDesignC.CIRCLE_SMALL, 8, "ic-muted"), UtilsUI.label(friend.getCity(), "muted"));
        }
        HBox status = UtilsUI.row(8, UtilsUI.statusPill(friend), UtilsUI.label("Unhurried reminder based on your " + friend.getCadence().label().toLowerCase() + " rhythm", "small", "muted"));
        VBox text = UtilsUI.col(6, nameRow, pills, details, status);

        Button log = UtilsUI.button("Log Contact", "btn-primary");
        log.setOnAction(_ -> app.openLogContact(friend));
        Button snooze = UtilsUI.button("Snooze", "btn-soft");
        snooze.setOnAction(_ -> Actions.showBelow(Actions.snoozeMenu(app, friend), snooze));
        Button edit = UtilsUI.button("Edit Profile", "btn-soft");
        edit.setOnAction(_ -> new AddFriendDialog(app, friend).open());
        Button more = UtilsUI.button("More", "btn-plain");
        more.setOnAction(_ -> Actions.showBelow(Actions.friendMenu(app, friend, () -> app.show(RootLayout.Page.FRIENDS)), more));
        HBox card = UtilsUI.row(18, photo, text, UtilsUI.spacer(), UtilsUI.row(8, log, snooze, edit, more));
        card.getStyleClass().add("profile-header");
        return card;
    }

    private void renderTabs() {
        int notes = NoteDAO.findByFriend(friendId).size();
        int contacts = ContactEntryDAO.findByFriend(friendId).size();
        tabBar.getChildren().setAll(
                tabButton("Timeline & Notes (" + notes + ")", 0),
                tabButton("Contact History (" + contacts + ")", 1),
                tabButton("Shared Memories & Milestones", 2));
    }

    private Button tabButton(String text, int index) {
        Button button = UtilsUI.button(text, "seg-btn");
        if (tab == index) {
            button.getStyleClass().add("on");
        }
        button.setOnAction(_ -> {
            tab = index;
            renderTabs();
            renderTab();
        });
        return button;
    }

    private void renderTab() {
        tabContent.getChildren().clear();
        switch (tab) {
            case 0 -> {
                flaggedCard().ifPresent(tabContent.getChildren()::add);
                tabContent.getChildren().addAll(addNoteCard(), notesSection());
            }
            case 1 -> {
                List<ContactEntry> entries = ContactEntryDAO.findByFriend(friendId);
                if (entries.isEmpty()) {
                    tabContent.getChildren().add(UtilsUI.card(6, UtilsUI.label("No catch-ups logged yet.", "h4"), UtilsUI.label("Log your first one whenever it happens.", "muted")));
                }
                entries.forEach(entry -> tabContent.getChildren().add(ContactHistoryView.entryCard(app, entry)));
            }
            default -> tabContent.getChildren().add(memoriesCard());
        }
    }

    private Optional<Node> flaggedCard() {
        List<Note> flagged = NoteDAO.findByFriend(friendId).stream().filter(Note::isFlagged).toList();
        if (flagged.isEmpty()) {
            return Optional.empty();
        }
        StackPane flagTile = UtilsUI.tile("tile-warn");
        flagTile.getChildren().add(UtilsUI.icon(MaterialDesignF.FLAG, 18, "ic-warn"));
        HBox title = UtilsUI.row(10, flagTile,
                UtilsUI.col(1, UtilsUI.label("Flagged for next catch-up", "h4"), UtilsUI.label("Topics you wanted to remember to bring up", "small", "muted")),
                UtilsUI.spacer(), UtilsUI.pill(flagged.size() + " PENDING", "pill-warn"));
        VBox box = new VBox(10, title);
        flagged.forEach(note -> {
            Button asked = UtilsUI.button("Mark asked", "btn-soft");
            asked.setOnAction(_ -> {
                note.setFlagged(false);
                NoteDAO.update(note);
                app.toast("Marked as asked.");
                app.refresh();
            });
            VBox text = UtilsUI.col(3, UtilsUI.wrapped(note.getTitle() + ": " + note.getBody(), "med"),
                    UtilsUI.label("Added via Notes  •  " + UtilsUI.ago(note.getDate()), "tiny", "muted"));
            HBox.setHgrow(text, Priority.ALWAYS);
            HBox row = UtilsUI.row(10, UtilsUI.icon(MaterialDesignS.STAR, 16, "ic-star"), text, asked);
            row.setAlignment(Pos.TOP_LEFT);
            box.getChildren().add(UtilsUI.styled(row, "inner-card"));
        });
        box.getStyleClass().add("flag-card");
        return Optional.of(box);
    }

    private Node addNoteCard() {
        HBox title = UtilsUI.row(8, UtilsUI.label("Add New Note or Memory", "h4"));
        TextArea body = new TextArea();
        body.setPromptText("Write a note, memory, or topic for next time...");
        body.setPrefRowCount(3);
        body.setWrapText(true);
        body.getStyleClass().add("field");

        FlowPane tags = new FlowPane(6, 6);
        tags.getChildren().add(UtilsUI.label("Tags:", "small", "muted"));
        NOTE_TAGS.forEach(tag -> {
            Button chip = UtilsUI.button("#" + tag, "chip");
            chip.setOnAction(_ -> {
                if (chosenTags.remove(tag)) {
                    chip.getStyleClass().remove("on");
                } else {
                    chosenTags.add(tag);
                    chip.getStyleClass().add("on");
                }
            });
            tags.getChildren().add(chip);
        });
        TextField custom = new TextField();
        custom.setPromptText("+ Tag");
        custom.getStyleClass().add("field");
        custom.setPrefWidth(90);
        custom.setOnAction(_ -> {
            String typed = custom.getText().trim().replace("#", "");
            if (!typed.isEmpty()) {
                chosenTags.add(typed);
                Button chip = UtilsUI.button("#" + typed, "chip", "on");
                tags.getChildren().add(tags.getChildren().size() - 1, chip);
                custom.clear();
            }
        });
        tags.getChildren().add(custom);

        CheckBox flag = new CheckBox("Flag as reminder / Ask next time");
        Button save = UtilsUI.button("Save Note", "btn-primary");
        save.setOnAction(_ -> {
            if (body.getText().isBlank()) {
                app.toast("Write something first.");
                return;
            }
            String text = body.getText().trim();
            String noteTitle = text.length() > 40 ? text.substring(0, 40) + "..." : text;
            Note note = new Note();
            note.setFriendId(friendId);
            note.setTitle(noteTitle);
            note.setBody(text);
            note.setDate(LocalDate.now());
            note.setFlagged(flag.isSelected());
            note.setTags(new ArrayList<>(chosenTags));
            NoteDAO.insert(note);
            app.toast("Note saved.");
            app.refresh();
        });
        return UtilsUI.card(10, title, body, tags, UtilsUI.row(8, flag, UtilsUI.spacer(), save));
    }

    private Node notesSection() {
        List<Note> notes = NoteDAO.findByFriend(friendId);
        VBox box = new VBox(10, UtilsUI.row(8, UtilsUI.label("Recent Notes & Updates", "h4"),
                UtilsUI.label("Showing " + (allNotes ? notes.size() : Math.min(NOTES_SHOWN, notes.size())) + " of " + notes.size(), "small", "muted")));
        notes.stream().limit(allNotes ? notes.size() : NOTES_SHOWN).forEach(note -> box.getChildren().add(noteCard(note)));
        if (!allNotes && notes.size() > NOTES_SHOWN) {
            Button older = UtilsUI.button("Show older notes (" + (notes.size() - NOTES_SHOWN) + " remaining)", "btn-surface");
            older.setOnAction(_ -> {
                allNotes = true;
                renderTab();
            });
            box.getChildren().add(new StackPane(older));
        }
        return box;
    }

    private Node noteCard(Note note) {
        StackPane noteTile = UtilsUI.tile("tile-info");
        noteTile.getChildren().add(UtilsUI.icon(MaterialDesignP.PENCIL, 16, "ic-primary"));
        HBox top = UtilsUI.row(8, noteTile, UtilsUI.label(note.getTitle(), "h4"),
                UtilsUI.label(UtilsUI.longDate(note.getDate()), "small", "muted"), UtilsUI.spacer());
        if (note.isFlagged()) {
            top.getChildren().add(UtilsUI.pill("Flagged", "pill-warn"));
        }
        Button edit = UtilsUI.button("Edit", "btn-plain");
        edit.setOnAction(_ -> new NoteDialog(app, note, null).open());
        Button delete = UtilsUI.button("Delete", "btn-plain");
        delete.setOnAction(_ -> app.confirm("Delete this note?", "This note will be removed.", "Delete", () -> {
            NoteDAO.delete(note.getId());
            app.refresh();
        }));
        top.getChildren().addAll(edit, delete);
        HBox tags = UtilsUI.row(6);
        note.getTags().forEach(tag -> tags.getChildren().add(UtilsUI.pill("#" + tag.toLowerCase(), "pill-strong")));
        return UtilsUI.card(8, top, UtilsUI.wrapped(note.getBody()), tags);
    }

    private Node memoriesCard() {
        VBox card = UtilsUI.card(10, UtilsUI.label("Shared Memories & Milestones", "h4"));
        if (friend.getBirthday() != null) {
            card.getChildren().add(UtilsUI.styled(UtilsUI.row(10, UtilsUI.icon(MaterialDesignC.CAKE_VARIANT, 16, "ic-danger"),
                    UtilsUI.label("Next birthday: " + UtilsUI.longDate(friend.nextBirthday()) + " (turning " + friend.turningAge() + ")", "med")), "tint"));
        }
        if (!friend.getFriendsSince().isEmpty()) {
            card.getChildren().add(UtilsUI.styled(UtilsUI.row(10, UtilsUI.icon(MaterialDesignH.HEART, 16, "ic-primary"),
                    UtilsUI.label("Friends since " + friend.getFriendsSince(), "med")), "tint"));
        }
        NoteDAO.findByFriend(friendId).stream().filter(n -> n.getTags().stream().anyMatch(t -> t.toLowerCase().contains("milestone")))
                .forEach(n -> card.getChildren().add(UtilsUI.styled(UtilsUI.row(10, UtilsUI.icon(MaterialDesignF.FLAG, 16, "ic-warn"), UtilsUI.wrapped(n.getTitle(), "med")), "tint")));
        if (card.getChildren().size() == 1) {
            card.getChildren().add(UtilsUI.label("Tag a note #Milestones and it will appear here.", "muted"));
        }
        return card;
    }

    private Node rhythmCard() {
        HBox title = UtilsUI.row(8, UtilsUI.label("Contact Rhythm", "h4"), UtilsUI.spacer(),
                UtilsUI.pill("Every " + friend.getCadence().days() + " days", "pill-mint"));
        double progress = Math.min(1, friend.daysSinceContact() / (double) friend.getCadence().days());
        Circle track = new Circle(28);
        track.setStyle("-fx-fill: transparent; -fx-stroke: -frm-tint-strong; -fx-stroke-width: 6;");
        Arc arc = new Arc(0, 0, 28, 28, 90, -360 * progress);
        arc.setType(ArcType.OPEN);
        arc.setStyle("-fx-fill: transparent; -fx-stroke: " + (friend.daysUntilDue() < 0 ? "-frm-danger" : "-frm-primary") + "; -fx-stroke-width: 6;");
        arc.setManaged(false);
        StackPane ring = new StackPane(track, new javafx.scene.Group(arc), UtilsUI.col(0, UtilsUI.label(friend.daysSinceContact() + "d", "h4"), UtilsUI.label("SINCE", "tiny", "muted")));
        ((VBox) ring.getChildren().get(2)).setAlignment(Pos.CENTER);
        ring.setMinSize(70, 70);
        String due = (friend.daysUntilDue() < 0 ? "Was due " : "Due ") + UtilsUI.shortDate(friend.nextDue());
        if (friend.daysUntilDue() < 0) {
            due += " • " + (-friend.daysUntilDue()) + " days overdue";
        }
        VBox info = UtilsUI.col(3, UtilsUI.label("Last reached out", "semi"),
                UtilsUI.label(UtilsUI.longDate(friend.getLastContacted()) + " (" + friend.daysSinceContact() + " days ago)", "small", "muted"),
                UtilsUI.wrapped(due, "small", "semi", friend.daysUntilDue() < 0 ? "c-danger" : "c-primary"));
        HBox.setHgrow(info, Priority.ALWAYS);
        Node box = UtilsUI.styled(UtilsUI.row(12, ring, info), "tint");
        Label quote = UtilsUI.wrapped("\"No guilt! A quick 2-minute text or meme is all it takes to warm things back up.\"", "italic", "small", "muted");

        Button adjust = UtilsUI.button("Adjust Cadence", "btn-soft");
        adjust.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(adjust, Priority.ALWAYS);
        adjust.setOnAction(_ -> {
            ContextMenu menu = new ContextMenu();
            for (Cadence option : Cadence.values()) {
                MenuItem item = new MenuItem(option.label());
                item.setOnAction(_ -> {
                    friend.setCadence(option);
                    FriendDAO.update(friend);
                    app.toast("Rhythm set to " + option.label().toLowerCase() + ".");
                    app.refresh();
                });
                menu.getItems().add(item);
            }
            Actions.showBelow(menu, adjust);
        });
        Button pause = UtilsUI.button("Pause", "btn-plain");
        pause.setOnAction(_ -> {
            FriendDAO.snooze(friendId, LocalDate.now().plusDays(30));
            app.toast("Reminders for " + friend.firstName() + " paused for 30 days.");
            app.refresh();
        });
        return UtilsUI.card(10, title, box, quote, UtilsUI.row(8, adjust, pause));
    }

    private Node infoCard() {
        Button edit = UtilsUI.button("Edit", "btn-plain");
        edit.setOnAction(_ -> new AddFriendDialog(app, friend).open());
        VBox card = UtilsUI.card(10, UtilsUI.row(8, UtilsUI.label("Friend Info & Basics", "h4"), UtilsUI.spacer(), edit));
        if (friend.getBirthday() != null) {
            VBox birthdayText = UtilsUI.col(1, UtilsUI.label(UtilsUI.shortDate(friend.nextBirthday()), "semi", "c-warn"),
                    UtilsUI.wrapped("Turning " + friend.turningAge() + " " + UtilsUI.inTime(friend.daysUntilBirthday()).toLowerCase() + "!", "small", "c-warn"));
            HBox.setHgrow(birthdayText, Priority.ALWAYS);
            birthdayText.setMinWidth(0);
            Button alert = UtilsUI.button("Set Gift Alert", "btn-surface");
            alert.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
            alert.setOnAction(_ -> app.toast("Gift alert set for " + friend.firstName() + "."));
            card.getChildren().add(UtilsUI.styled(UtilsUI.row(8, UtilsUI.icon(MaterialDesignC.CAKE_VARIANT, 16, "ic-warn"), birthdayText, alert), "tint"));
        }
        addInfo(card, "FRIENDS SINCE", friend.getFriendsSince(), false);
        addInfo(card, "EMAIL", friend.getEmail(), true);
        addInfo(card, "PHONE", friend.getPhone(), true);
        addInfo(card, "HANDLES", friend.getHandle(), true);
        return card;
    }

    private void addInfo(VBox card, String title, String value, boolean copyable) {
        if (value == null || value.isEmpty()) {
            return;
        }
        VBox text = UtilsUI.col(1, UtilsUI.label(title, "eyebrow", "muted"), UtilsUI.label(value, "med"));
        HBox row = UtilsUI.row(10, text, UtilsUI.spacer());
        if (copyable) {
            Button copy = UtilsUI.button("Copy", "btn-plain");
            copy.setStyle("-fx-font-size: 11px; -fx-padding: 2 6;");
            copy.setOnAction(_ -> {
                ClipboardContent content = new ClipboardContent();
                content.putString(value);
                Clipboard.getSystemClipboard().setContent(content);
                app.toast("Copied.");
            });
            row.getChildren().add(copy);
        }
        card.getChildren().add(row);
    }

    private Node hobbiesCard() {
        TextField add = new TextField();
        add.setPromptText("Add a hobby and press Enter");
        add.getStyleClass().add("field");
        add.setOnAction(_ -> {
            String typed = add.getText().trim();
            if (!typed.isEmpty()) {
                friend.getHobbies().add(typed);
                FriendDAO.update(friend);
                app.refresh();
            }
        });
        FlowPane chips = new FlowPane(6, 6);
        friend.getHobbies().forEach(h -> chips.getChildren().add(UtilsUI.pill(h, "pill-strong", "pill-big")));
        return UtilsUI.card(10, UtilsUI.row(8, UtilsUI.label("Hobbies & Passions", "h4")),
                UtilsUI.wrapped("Personal interests to keep in mind for conversations and gifts:", "small", "muted"), chips, add);
    }

    private Node encountersCard() {
        VBox card = UtilsUI.card(8, UtilsUI.row(8, UtilsUI.label("Recent Encounters", "h4"), UtilsUI.spacer(), UtilsUI.label("Past 3 months", "tiny", "muted")));
        List<ContactEntry> entries = ContactEntryDAO.findByFriend(friendId);
        entries.stream().limit(3).forEach(entry -> {
            VBox text = UtilsUI.col(1, UtilsUI.label(entry.getTitle(), "small", "semi"), UtilsUI.label(entry.getType().label() + (entry.getDuration().isEmpty() ? "" : " • " + entry.getDuration()), "tiny", "muted"));
            HBox.setHgrow(text, Priority.ALWAYS);
            text.setMinWidth(0);
            card.getChildren().add(UtilsUI.styled(UtilsUI.row(8, text, UtilsUI.label(UtilsUI.shortDate(entry.getDate()), "tiny", "semi")), "tint"));
        });
        if (entries.isEmpty()) {
            card.getChildren().add(UtilsUI.label("Nothing logged yet.", "small", "muted"));
        }
        Button all = UtilsUI.button("View all " + entries.size() + " entries in Contact History", "link");
        all.setOnAction(_ -> {
            tab = 1;
            renderTabs();
            renderTab();
        });
        card.getChildren().add(all);
        return card;
    }
}