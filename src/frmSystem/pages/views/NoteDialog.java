package frmSystem.pages.views;

import frmSystem.db.daos.FriendDAO;
import frmSystem.db.daos.NoteDAO;
import frmSystem.db.models.Friend;
import frmSystem.db.models.Note;
import frmSystem.pages.RootLayout;
import frmSystem.ui.UtilsUI;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Creates or edits a note. */
public class NoteDialog {
    private final RootLayout app;
    private final Note editing;
    private final Friend preselected;

    public NoteDialog(RootLayout app, Note editing, Friend preselected) {
        this.app = app;
        this.editing = editing;
        this.preselected = preselected;
    }

    public void open() {
        ComboBox<Friend> friendBox = new ComboBox<>();
        friendBox.getItems().setAll(FriendDAO.findAll().stream().filter(f -> !f.isArchived())
                .sorted(Comparator.comparing(Friend::getName)).toList());
        friendBox.setPromptText("Select friend...");
        friendBox.setMaxWidth(Double.MAX_VALUE);
        friendBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Friend friend) {
                return friend == null ? "" : friend.getName();
            }

            @Override
            public Friend fromString(String text) {
                return null;
            }
        });
        TextField title = new TextField();
        title.setPromptText("Title");
        title.getStyleClass().add("field");
        TextArea body = new TextArea();
        body.setPromptText("Write a note, memory, or topic for next time...");
        body.setPrefRowCount(6);
        body.setWrapText(true);
        body.getStyleClass().add("field");
        TextField tags = new TextField();
        tags.setPromptText("Tags, separated by commas (Job, Family, Plans)");
        tags.getStyleClass().add("field");
        CheckBox flag = new CheckBox("Flag as reminder / Ask next time");
        Label error = UtilsUI.label("", "field-error");

        if (editing != null) {
            FriendDAO.findById(editing.getFriendId()).ifPresent(friendBox::setValue);
            title.setText(editing.getTitle());
            body.setText(editing.getBody());
            tags.setText(String.join(", ", editing.getTags()));
            flag.setSelected(editing.isFlagged());
        } else if (preselected != null) {
            friendBox.setValue(preselected);
        }

        Button cancel = UtilsUI.button("Cancel", "btn-soft");
        cancel.setOnAction(_ -> app.closeModal());
        Button save = UtilsUI.button("Save Note", "btn-primary");
        save.setOnAction(_ -> {
            if (friendBox.getValue() == null) {
                error.setText("Please select a friend.");
                return;
            }
            if (body.getText().isBlank()) {
                error.setText("Please write something first.");
                return;
            }
            List<String> tagList = new ArrayList<>();
            Arrays.stream(tags.getText().split(",")).map(s -> s.trim().replace("#", "")).filter(s -> !s.isEmpty()).forEach(tagList::add);
            String titleText = title.getText().isBlank() ? "Note" : title.getText().trim();
            if (editing == null) {
                Note note = new Note();
                note.setFriendId(friendBox.getValue().getId());
                note.setTitle(titleText);
                note.setBody(body.getText().trim());
                note.setDate(LocalDate.now());
                note.setFlagged(flag.isSelected());
                note.setTags(tagList);
                NoteDAO.insert(note);
            } else {
                editing.setFriendId(friendBox.getValue().getId());
                editing.setTitle(titleText);
                editing.setBody(body.getText().trim());
                editing.setTags(tagList);
                editing.setFlagged(flag.isSelected());
                NoteDAO.update(editing);
            }
            app.closeModal();
            app.toast("Note saved.");
            app.refresh();
        });

        Node actions = UtilsUI.row(8, UtilsUI.spacer(), cancel, save);
        VBox modal = new VBox(10, UtilsUI.label(editing == null ? "New Note" : "Edit Note", "h2"), friendBox, title, body, tags, flag, error, actions);
        modal.getStyleClass().add("modal");
        modal.setPadding(new Insets(22));
        modal.setMaxSize(500, Region.USE_PREF_SIZE);
        app.openModal(modal);
    }
}