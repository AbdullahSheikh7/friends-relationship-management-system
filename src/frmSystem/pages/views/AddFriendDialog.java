package frmSystem.pages.views;

import frmSystem.db.daos.FriendDAO;
import frmSystem.db.daos.NoteDAO;
import frmSystem.db.models.Cadence;
import frmSystem.db.models.Closeness;
import frmSystem.db.models.Friend;
import frmSystem.db.models.Note;
import frmSystem.pages.RootLayout;
import frmSystem.ui.UtilsUI;

import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The six-step Add a Friend flow, also used for editing. Moving between steps keeps what was typed. */
public class AddFriendDialog {
    private static final String[] STEPS = {"Closeness", "Space", "Basics", "Interests", "Rhythm", "First Note"};
    private static final String[] NEXT = {"Space & Context", "Basics", "Interests", "Rhythm", "First Note", ""};
    private static final List<String> SPACES = List.of("School / Uni", "Work", "Neighborhood", "Family Circle", "Online & Gaming", "Travel & Adventure");
    private static final int MAX_NAME = 80;

    private final RootLayout app;
    private final Friend editing;
    private final HBox stepBar = UtilsUI.row(8);
    private final VBox body = new VBox(16);
    private final HBox footer = UtilsUI.styled(UtilsUI.row(10), "modal-footer");
    private final Label error = UtilsUI.label("", "field-error");

    private final TextField name = new TextField();
    private final TextField since = new TextField();
    private final TextField occupation = new TextField();
    private final TextField city = new TextField();
    private final TextField phone = new TextField();
    private final TextField email = new TextField();
    private final DatePicker lastContacted = new DatePicker(LocalDate.now());
    private final DatePicker birthday = new DatePicker();
    private final TextField hobbies = new TextField();
    private final TextArea firstNote = new TextArea();

    private Closeness closeness = Closeness.VERY_CLOSE;
    private String space = "School / Uni";
    private Cadence cadence = Closeness.VERY_CLOSE.suggestedCadence();
    private int step;

    public AddFriendDialog(RootLayout app, Friend editing) {
        this.app = app;
        this.editing = editing;
        if (editing != null) {
            closeness = editing.getCloseness();
            space = editing.getSpace();
            cadence = editing.getCadence();
            name.setText(editing.getName());
            since.setText(editing.getFriendsSince());
            occupation.setText(editing.getOccupation());
            city.setText(editing.getCity());
            phone.setText(editing.getPhone());
            email.setText(editing.getEmail());
            lastContacted.setValue(editing.getLastContacted());
            birthday.setValue(editing.getBirthday());
            hobbies.setText(String.join(", ", editing.getHobbies()));
        }
    }

    public void open() {
        Button close = UtilsUI.iconButton(MaterialDesignC.CLOSE, "icon-btn");
        close.setOnAction(_ -> app.closeModal());
        HBox titleRow = UtilsUI.row(8, UtilsUI.col(2,
                UtilsUI.bullet("FRM GUIDED RELATIONSHIP SETUP", "eyebrow", "c-primary"),
                UtilsUI.label(editing == null ? "Add a Friend" : "Edit " + editing.getName(), "h2"),
                UtilsUI.label("No pressure, just gentle ways to remember the people who matter.", "muted")),
                UtilsUI.spacer(), close);
        VBox top = new VBox(14, titleRow, stepBar);
        top.setPadding(new Insets(20, 22, 10, 22));

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        body.setPadding(new Insets(8, 22, 16, 22));
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox modal = new VBox(top, scroll, footer);
        modal.getStyleClass().add("modal");
        modal.setMaxWidth(720);
        modal.setMaxHeight(570);
        render();
        app.openModal(modal);
    }

    private void render() {
        stepBar.getChildren().clear();
        for (int i = 0; i < STEPS.length; i++) {
            Region bar = UtilsUI.styled(new Region(), "step-bar");
            if (i <= step) {
                bar.getStyleClass().add("on");
            }
            Label text = UtilsUI.label(String.format("%02d %s", i + 1, STEPS[i]), "small", i == step ? "semi" : "muted");
            if (i == step) {
                text.getStyleClass().add("c-primary");
            }
            VBox item = UtilsUI.col(6, bar, text);
            HBox.setHgrow(item, Priority.ALWAYS);
            item.setPrefWidth(100);
            final int target = i;
            item.setOnMouseClicked(_ -> {
                if (target < step || validate()) {
                    step = target;
                    render();
                }
            });
            item.setStyle("-fx-cursor: hand;");
            stepBar.getChildren().add(item);
        }

        body.getChildren().setAll(switch (step) {
            case 0 -> closenessStep();
            case 1 -> spaceStep();
            case 2 -> basicsStep();
            case 3 -> interestsStep();
            case 4 -> rhythmStep();
            default -> noteStep();
        });
        error.setText("");
        body.getChildren().add(error);
        renderFooter();
    }

    private void renderFooter() {
        Button cancel = UtilsUI.button("Cancel", "btn-plain");
        cancel.setOnAction(_ -> app.closeModal());
        Button back = UtilsUI.button("Back", "btn-soft");
        back.setDisable(step == 0);
        back.setOnAction(_ -> {
            step--;
            render();
        });
        boolean last = step == STEPS.length - 1;
        Button next = UtilsUI.button(last ? (editing == null ? "Save Friend" : "Save Changes") : "Next: " + NEXT[step], "btn-primary");
        next.setOnAction(_ -> {
            if (!validate()) {
                return;
            }
            if (last) {
                save();
            } else {
                step++;
                render();
            }
        });
        footer.getChildren().setAll(cancel, UtilsUI.spacer(), UtilsUI.label("Step " + (step + 1) + " of " + STEPS.length, "small", "muted"), back, next);
    }

    private boolean validate() {
        String typed = name.getText().trim();
        if (step >= 2) {
            if (typed.isEmpty()) {
                return fail("Please add a name or nickname.");
            }
            if (typed.length() > MAX_NAME) {
                return fail("Names can be up to " + MAX_NAME + " characters.");
            }
            if (lastContacted.getValue() != null && lastContacted.getValue().isAfter(LocalDate.now())) {
                return fail("Last contacted can't be in the future.");
            }
            if (birthday.getValue() != null && birthday.getValue().isAfter(LocalDate.now())) {
                return fail("Birthday can't be in the future.");
            }
            if (!email.getText().isBlank() && !email.getText().trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
                return fail("That email address doesn't look right.");
            }
        }
        error.setText("");
        return true;
    }

    private boolean fail(String message) {
        error.setText(message);
        return false;
    }

    private void save() {
        Friend friend = editing == null ? new Friend() : editing;
        friend.setName(name.getText().trim());
        friend.setCloseness(closeness);
        friend.setSpace(space);
        friend.setCadence(cadence);
        friend.setFriendsSince(since.getText().trim());
        friend.setOccupation(occupation.getText().trim());
        if (friend.getHeadline().isEmpty() || editing != null) {
            friend.setHeadline(occupation.getText().trim());
        }
        friend.setCity(city.getText().trim());
        friend.setPhone(phone.getText().trim());
        friend.setEmail(email.getText().trim());
        friend.setLastContacted(lastContacted.getValue() == null ? LocalDate.now() : lastContacted.getValue());
        friend.setBirthday(birthday.getValue());
        List<String> list = new ArrayList<>();
        Arrays.stream(hobbies.getText().split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(list::add);
        friend.setHobbies(list);

        if (editing == null) {
            boolean duplicate = FriendDAO.findAll().stream().anyMatch(f -> f.getName().equalsIgnoreCase(friend.getName()));
            if (duplicate) {
                app.confirm("Add another " + friend.getName() + "?", "You already have a friend with this name.", "Add anyway", () -> finish(friend));
                return;
            }
            finish(friend);
        } else {
            FriendDAO.update(friend);
            app.closeModal();
            app.toast("Saved changes for " + friend.getName() + ".");
            app.refresh();
        }
    }

    private void finish(Friend friend) {
        FriendDAO.insert(friend);
        String noteText = firstNote.getText().trim();
        if (!noteText.isEmpty()) {
            Note note = new Note();
            note.setFriendId(friend.getId());
            note.setTitle("First note");
            note.setBody(noteText);
            note.setDate(LocalDate.now());
            note.setFlagged(false);
            NoteDAO.insert(note);
        }
        app.closeModal();
        app.toast(friend.getName() + " added.");
        app.showFriendProfile(friend.getId());
    }

    private Node sectionTitle(String text, String hint) {
        return UtilsUI.row(8, UtilsUI.label(text, "h3"), UtilsUI.spacer(), UtilsUI.pill(hint, "pill-strong"));
    }

    private Node closenessStep() {
        HBox tiers = UtilsUI.row(10,
                tier(Closeness.VERY_CLOSE, "Very Close", "Inner circle, confidants, everyday support.", "Weekly / Bi-weekly"),
                tier(Closeness.CLOSE, "Close Friend", "Meaningful bonds, shared memories, warm presence.", "Monthly rhythm"),
                tier(Closeness.CASUAL, "Casual & Familiar", "Good company, periodic catchups when paths cross.", "Every 3-6 months"));
        return UtilsUI.col(12, sectionTitle("How close is this friend to you?", "Select one tier"), tiers);
    }

    private Node tier(Closeness level, String title, String description, String rhythm) {
        boolean on = closeness == level;
        VBox card = UtilsUI.col(8, UtilsUI.label(title, "h4"), UtilsUI.wrapped(description, "small", "muted"), UtilsUI.spacer(), UtilsUI.bullet(rhythm, "pill", on ? "pill-mint" : "pill-strong"));
        card.getStyleClass().add("tier");
        if (on) {
            card.getStyleClass().add("on");
        }
        card.setPrefWidth(210);
        card.setMinHeight(190);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setOnMouseClicked(_ -> {
            closeness = level;
            cadence = level.suggestedCadence();
            render();
        });
        return card;
    }

    private Node spaceStep() {
        FlowPane chips = new FlowPane(8, 8);
        for (String value : SPACES) {
            Button chip = UtilsUI.button(value, "chip");
            if (value.equals(space)) {
                chip.getStyleClass().add("on");
            }
            chip.setOnAction(_ -> {
                space = value;
                render();
            });
            chips.getChildren().add(chip);
        }
        TextField custom = new TextField(SPACES.contains(space) ? "" : space);
        custom.setPromptText("+ Custom Space");
        custom.getStyleClass().add("field");
        custom.setMaxWidth(260);
        custom.textProperty().addListener((_, _, b) -> {
            if (!b.isBlank()) {
                space = b.trim();
            }
        });
        return UtilsUI.col(12, sectionTitle("Which space are they from?", "Tap to tag"), chips, custom);
    }

    private VBox field(String title, String hint, Node input) {
        return UtilsUI.col(5, UtilsUI.row(4, UtilsUI.label(title, "small", "semi"), UtilsUI.spacer(), UtilsUI.label(hint, "tiny", "muted")), input);
    }

    private Node input(TextField field, String prompt) {
        field.setPromptText(prompt);
        HBox.setHgrow(field, Priority.ALWAYS);
        return UtilsUI.styled(UtilsUI.row(0, field), "field-box");
    }

    private Node basicsStep() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        lastContacted.setMaxWidth(Double.MAX_VALUE);
        birthday.setMaxWidth(Double.MAX_VALUE);
        grid.addRow(0, field("Friend's Name *", "Preferred name", input(name, "e.g. Elena Rostova")),
                field("Connected Since", "Approximate", input(since, "e.g. College 2018")));
        grid.addRow(1, field("What they do / Current passion", "", input(occupation, "e.g. Ceramic Sculptor")),
                field("Last Contacted", "Auto-seeded", lastContacted));
        grid.addRow(2, field("Birthday", "Optional", birthday), field("City", "Optional", input(city, "e.g. Karachi")));
        grid.addRow(3, field("Phone", "Optional, never used for messaging", input(phone, "")),
                field("Email", "Optional, never used for messaging", input(email, "")));
        ColumnConstraints half = new ColumnConstraints();
        half.setPercentWidth(50);
        grid.getColumnConstraints().addAll(half, half);
        return UtilsUI.col(12, sectionTitle("Friend Basics", "Only the name is required"), grid);
    }

    private Node interestsStep() {
        String hint = closeness == Closeness.CASUAL ? "Optional for casual friends" : "Helps with conversations and gifts";
        hobbies.setPromptText("Ceramics, Coffee, Board Games");
        hobbies.getStyleClass().add("field");
        return UtilsUI.col(12, sectionTitle("Hobbies & Interests", hint), hobbies,
                UtilsUI.label("Separate each one with a comma.", "small", "muted"));
    }

    private Node rhythmStep() {
        HBox buttons = UtilsUI.styled(UtilsUI.row(4), "seg");
        for (Cadence option : Cadence.values()) {
            Button button = UtilsUI.button(option.label(), "seg-btn");
            if (option == cadence) {
                button.getStyleClass().add("on-primary");
            }
            HBox.setHgrow(button, Priority.ALWAYS);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(_ -> {
                cadence = option;
                render();
            });
            buttons.getChildren().add(button);
        }
        String recommended = "Recommended for '" + closeness.label() + "': " + closeness.suggestedCadence().label();
        HBox privacy = UtilsUI.styled(UtilsUI.row(8, UtilsUI.label("All details remain 100% offline on your local storage. No cloud tracking.", "small", "med")), "tint");
        return UtilsUI.col(12, sectionTitle("Check-in Rhythm", recommended), buttons, privacy);
    }

    private Node noteStep() {
        firstNote.setPromptText("Anything you'd like to remember? Optional.");
        firstNote.setPrefRowCount(6);
        firstNote.setWrapText(true);
        firstNote.getStyleClass().add("field");
        return UtilsUI.col(12, sectionTitle("First Note", "Optional"), firstNote);
    }
}