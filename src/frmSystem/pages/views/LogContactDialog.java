package frmSystem.pages.views;

import frmSystem.db.daos.ContactEntryDAO;
import frmSystem.db.daos.FriendDAO;
import frmSystem.db.models.ContactEntry;
import frmSystem.db.models.ContactType;
import frmSystem.db.models.Friend;
import frmSystem.pages.RootLayout;
import frmSystem.ui.UtilsUI;

import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Records a catch-up, including one that happened on a past date. */
public class LogContactDialog {
    private final RootLayout app;
    private final Friend preselected;

    public LogContactDialog(RootLayout app, Friend preselected) {
        this.app = app;
        this.preselected = preselected;
    }

    public void open() {
        VBox form = form(app, preselected, app::closeModal);
        Button close = UtilsUI.iconButton(MaterialDesignC.CLOSE, "icon-btn");
        close.setOnAction(_ -> app.closeModal());
        VBox modal = new VBox(14, UtilsUI.row(8, UtilsUI.label("Log a Catch-up", "h2"), UtilsUI.spacer(), close), form);
        modal.getStyleClass().add("modal");
        modal.setPadding(new Insets(22));
        modal.setMaxSize(440, Region.USE_PREF_SIZE);
        app.openModal(modal);
    }

    /** The form itself. Contact History shows it inline as Quick Catch-up Log. */
    static VBox form(RootLayout app, Friend preselected, Runnable afterSave) {
        List<Friend> friends = FriendDAO.findAll().stream().filter(f -> !f.isArchived())
                .sorted(Comparator.comparing(Friend::getName)).toList();
        ComboBox<Friend> friendBox = new ComboBox<>();
        friendBox.getItems().setAll(friends);
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
        if (preselected != null) {
            friendBox.setValue(preselected);
        }
        ComboBox<ContactType> typeBox = new ComboBox<>();
        typeBox.getItems().setAll(ContactType.values());
        typeBox.setValue(ContactType.IN_PERSON);
        typeBox.setMaxWidth(Double.MAX_VALUE);
        typeBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ContactType type) {
                return type == null ? "" : type.label();
            }

            @Override
            public ContactType fromString(String text) {
                return null;
            }
        });
        DatePicker when = new DatePicker(LocalDate.now());
        when.setMaxWidth(Double.MAX_VALUE);
        TextArea topics = new TextArea();
        topics.setPromptText("What did you talk about? Any books, life news, or follow-ups mentioned?");
        topics.setPrefRowCount(3);
        topics.setWrapText(true);
        topics.getStyleClass().add("field");
        Label error = UtilsUI.label("", "field-error");

        Button save = UtilsUI.button("Record Catch-up", "btn-primary");
        save.setMaxWidth(Double.MAX_VALUE);
        save.setOnAction(_ -> {
            if (friendBox.getValue() == null) {
                error.setText("Please select a friend.");
                return;
            }
            if (when.getValue() == null || when.getValue().isAfter(LocalDate.now())) {
                error.setText("Please choose today or a past date.");
                return;
            }
            String text = topics.getText().trim();
            String title = typeBox.getValue().label() + " with " + friendBox.getValue().firstName();
            ContactEntry entry = new ContactEntry();
            entry.setFriendId(friendBox.getValue().getId());
            entry.setDate(when.getValue());
            entry.setType(typeBox.getValue());
            entry.setDuration("");
            entry.setTitle(title);
            entry.setLocation("");
            entry.setSummary(text);
            entry.setTags(List.of());
            ContactEntryDAO.insert(entry);
            app.toast("Catch-up with " + friendBox.getValue().firstName() + " recorded.");
            afterSave.run();
            app.refresh();
        });

        GridPane twoColumns = new GridPane();
        twoColumns.setHgap(10);
        ColumnConstraints channelColumn = new ColumnConstraints();
        channelColumn.setPercentWidth(60);
        ColumnConstraints dateColumn = new ColumnConstraints();
        dateColumn.setPercentWidth(40);
        twoColumns.getColumnConstraints().addAll(channelColumn, dateColumn);
        twoColumns.addRow(0, labeled("CHANNEL", typeBox), labeled("WHEN", when));
        return UtilsUI.col(10, labeled("FRIEND", friendBox), twoColumns, labeled("KEY TOPICS & MEMORY SPARKS", topics), error, save);
    }

    private static Node labeled(String title, Node input) {
        return UtilsUI.col(4, UtilsUI.label(title, "eyebrow", "muted"), input);
    }
}