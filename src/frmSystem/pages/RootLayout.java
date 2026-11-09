package frmSystem.pages;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import frmSystem.db.daos.ContactEntryDAO;
import frmSystem.db.daos.FriendDAO;
import frmSystem.db.models.ContactEntry;
import frmSystem.db.models.Friend;
import frmSystem.db.models.FriendStatus;
import frmSystem.ui.UtilsUI;

import frmSystem.pages.views.AddFriendDialog;
import frmSystem.pages.views.BackupView;
import frmSystem.pages.views.BirthdaysView;
import frmSystem.pages.views.ContactHistoryView;
import frmSystem.pages.views.DashboardView;
import frmSystem.pages.views.FriendProfileView;
import frmSystem.pages.views.FriendsView;
import frmSystem.pages.views.HelpView;
import frmSystem.pages.views.LogContactDialog;
import frmSystem.pages.views.NoteDialog;
import frmSystem.pages.views.NotesHubView;
import frmSystem.pages.views.SettingsView;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class RootLayout {
    public enum Page { DASHBOARD, FRIENDS, NOTES, HISTORY, BIRTHDAYS, BACKUP, SETTINGS, HELP }

    private final Stage stage;
    private final StackPane root = new StackPane();
    private final StackPane content = new StackPane();
    private final StackPane modalLayer = new StackPane();
    private final VBox toastLayer = new VBox(8);
    private final Map<Page, Button> navButtons = new EnumMap<>(Page.class);
    private final Label friendCount = UtilsUI.pill("0");
    private final Circle birthdayDot = new Circle(4);
    private Runnable refreshCurrent = () -> { };
    private boolean busy = false;

    public RootLayout(Stage primaryStage) {
        this.stage = primaryStage;
        BorderPane shell = new BorderPane();
        shell.setTop(buildTopBar());
        shell.setLeft(buildSidebar());
        shell.setBottom(buildStatusBar());
        content.getStyleClass().add("content");
        shell.setCenter(content);

        modalLayer.setVisible(false);
        modalLayer.getStyleClass().add("overlay");
        toastLayer.setAlignment(Pos.BOTTOM_CENTER);
        toastLayer.setPadding(new Insets(0, 0, 44, 0));
        toastLayer.setPickOnBounds(false);
        toastLayer.setMouseTransparent(false);

        root.getChildren().addAll(shell, modalLayer, toastLayer);
        show(Page.DASHBOARD);
    }

    // ---------- Accessors ----------

    public Parent root() {
        return root;
    }

    public Stage stage() {
        return stage;
    }

    // ---------- Shortcuts ----------

    public void installShortcuts(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), this::openAddFriend);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN), () -> openLogContact(null));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.J, KeyCombination.SHORTCUT_DOWN), () -> new NoteDialog(this, null, null).open());
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), this::closeModal);
    }

    // ---------- Page navigation ----------

    public void show(Page page) {
        if (busy) {
            toast("A backup operation is running. The screen stays locked until it finishes.");
            return;
        }
        refreshCurrent = () -> setPage(page, buildPage(page));
        refreshCurrent.run();
    }

    public boolean isBusy() {
        return busy;
    }

    private Node buildPage(Page page) {
        return switch (page) {
            case DASHBOARD -> new DashboardView(this).build();
            case FRIENDS -> new FriendsView(this, "").build();
            case NOTES -> new NotesHubView(this).build();
            case HISTORY -> new ContactHistoryView(this).build();
            case BIRTHDAYS -> new BirthdaysView(this).build();
            case BACKUP -> new BackupView(this).build();
            case SETTINGS -> new SettingsView(this).build();
            case HELP -> new HelpView().build();
        };
    }

    public void showFriendProfile(long friendId) {
        if (busy) {
            toast("A backup operation is running. The screen stays locked until it finishes.");
            return;
        }
        refreshCurrent = () -> setPage(Page.FRIENDS, new FriendProfileView(this, friendId).build());
        refreshCurrent.run();
    }

    public void refresh() {
        refreshCurrent.run();
    }

    private void setPage(Page active, Node node) {
        navButtons.forEach((page, button) -> {
            button.getStyleClass().remove("active");
            if (page == active) {
                button.getStyleClass().add("active");
            }
        });
        var all = FriendDAO.findAll();
        long activeFriends = all.stream().filter(f -> !f.isArchived()).count();
        friendCount.setText(String.valueOf(activeFriends));
        birthdayDot.setVisible(all.stream().anyMatch(f -> f.daysUntilBirthday() <= 7));
        content.getChildren().setAll(node);
    }

    public static ScrollPane scrollPage(Node page) {
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    // ---------- Dialogs & modals ----------

    public void openAddFriend() {
        new AddFriendDialog(this, null).open();
    }

    public void openLogContact(Friend friend) {
        new LogContactDialog(this, friend).open();
    }

    public void openModal(Node modal) {
        modalLayer.getChildren().setAll(modal);
        StackPane.setMargin(modal, new Insets(24));
        modalLayer.setVisible(true);
    }

    public void closeModal() {
        if (busy) {
            return;
        }
        modalLayer.getChildren().clear();
        modalLayer.setVisible(false);
    }

    public void confirm(String title, String message, String actionText, Runnable action) {
        VBox box = new VBox(14);
        box.getStyleClass().add("modal");
        box.setPadding(new Insets(22));
        box.setMaxSize(420, Region.USE_PREF_SIZE);
        Button cancel = UtilsUI.button("Cancel", "btn-soft");
        cancel.setOnAction(_ -> closeModal());
        Button ok = UtilsUI.button(actionText, "btn-primary");
        ok.setOnAction(_ -> {
            closeModal();
            action.run();
        });
        HBox actions = UtilsUI.row(8, UtilsUI.spacer(), cancel, ok);
        box.getChildren().addAll(UtilsUI.label(title, "h3"), UtilsUI.wrapped(message, "muted"), actions);
        openModal(box);
    }

    // ---------- Busy loader (backup / restore) ----------

    /** Locks the whole UI behind a non-removable loader modal while a long database task runs.
     *  View switching is blocked by the busy checks in show()/showFriendProfile(). */
    public void runBusy(String message, Runnable task, Runnable onComplete) {
        busy = true;
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setStyle("-fx-progress-color: -frm-primary;");
        VBox box = new VBox(14,
                spinner,
                UtilsUI.label(message, "h3"),
                UtilsUI.wrapped("Please wait — navigation is locked to keep your data safe.", "small", "muted"));
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("modal");
        box.setPadding(new Insets(30));
        box.setMaxSize(380, Region.USE_PREF_SIZE);
        openModal(box);

        long started = System.nanoTime();
        Thread worker = new Thread(() -> {
            boolean failed = guarded(task);
            long remaining = 800 - (System.nanoTime() - started) / 1_000_000;
            try {
                if (remaining > 0) {
                    Thread.sleep(remaining);
                }
            } catch (InterruptedException ignored) {
            }
            Platform.runLater(() -> {
                busy = false;
                closeModal();
                if (failed) {
                    toast("The backup operation failed. Try again.");
                }
                onComplete.run();
            });
        });
        worker.setDaemon(true);
        worker.start();
    }

    private static boolean guarded(Runnable task) {
        try {
            task.run();
            return false;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return true;
        }
    }

    // ---------- Toasts ----------

    public void toast(String message) {
        toast(message, null, null);
    }

    public void toast(String message, String actionText, Runnable action) {
        Label messageLabel = new Label(message);

        HBox toast = new HBox(12, messageLabel);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.getStyleClass().add("toast");
        toast.setMaxWidth(Region.USE_PREF_SIZE);

        if (action != null) {
            Button button = new Button(actionText);
            button.setOnAction(_ -> {
                toastLayer.getChildren().remove(toast);
                action.run();
            });
            toast.getChildren().add(button);
        }

        toastLayer.getChildren().setAll(toast);
        PauseTransition pause = new PauseTransition(Duration.seconds(action == null ? 3 : 6));
        pause.setOnFinished(_ -> toastLayer.getChildren().remove(toast));
        pause.play();
    }

    // ---------- Top bar ----------

    private Node buildTopBar() {
        ImageLogo logo = new ImageLogo();

        Label title = UtilsUI.label("FRM — Friends Management System", "h4");

        Button bell = UtilsUI.button("Alerts", "btn-soft");
        bell.setOnAction(_ -> {
            long waiting = FriendDAO.findAll().stream()
                    .filter(f -> !f.isArchived() && f.status() == FriendStatus.NEEDS_HELLO).count();
            toast(waiting + " friends are waiting for a hello, whenever you're free.");
        });

        Button profile = UtilsUI.button("Profile", "btn-soft");
        profile.setOnAction(_ -> show(Page.SETTINGS));

        Region leftSpacer  = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer,  Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        HBox bar = new HBox(12, logo, title, leftSpacer, rightSpacer, bell, profile);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("topbar");
        return bar;
    }

    // ---------- Sidebar ----------

    private Button nav(Page page, String text) {
        Button button = new Button(text);
        button.getStyleClass().setAll("nav-item");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setOnAction(_ -> show(page));
        navButtons.put(page, button);
        return button;
    }

    private Node withTrailing(Button button, Node trailing) {
        StackPane.setAlignment(trailing, Pos.CENTER_RIGHT);
        StackPane.setMargin(trailing, new Insets(0, 10, 0, 0));
        trailing.setMouseTransparent(true);
        return new StackPane(button, trailing);
    }

    private Node buildSidebar() {
        Button add = new Button("Add Friend");
        add.getStyleClass().setAll("nav-add");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setOnAction(_ -> openAddFriend());

        birthdayDot.setStyle("-fx-fill: -frm-danger;");

        Label mainHeader = new Label("MAIN WORKSPACE");
        mainHeader.getStyleClass().addAll("eyebrow", "muted");

        HBox mainTop = new HBox(8, mainHeader, add);
        mainTop.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(add, Priority.ALWAYS);

        VBox main = new VBox(4,
                mainTop,
                nav(Page.DASHBOARD, "Dashboard"),
                withTrailing(nav(Page.FRIENDS, "Friends"), friendCount),
                nav(Page.NOTES, "Notes Hub"),
                nav(Page.HISTORY, "Contact History"),
                withTrailing(nav(Page.BIRTHDAYS, "Birthdays"), birthdayDot));
        VBox.setMargin(main.getChildren().get(0), new Insets(0, 0, 6, 8));

        Label systemHeader = new Label("DATA & SYSTEM");
        systemHeader.getStyleClass().addAll("eyebrow", "muted");

        VBox system = new VBox(4,
                systemHeader,
                nav(Page.BACKUP, "Backup & Export"),
                nav(Page.SETTINGS, "Settings"),
                nav(Page.HELP, "Help & About"));
        VBox.setMargin(system.getChildren().get(0), new Insets(0, 0, 6, 8));

        VBox sidebar = new VBox(main, UtilsUI.spacer(), system);
        sidebar.getStyleClass().add("sidebar");
        return sidebar;
    }

    // ---------- Status bar ----------

    private Node buildStatusBar() {
        Label privacyLabel = new Label("All data stored locally on this device");
        privacyLabel.getStyleClass().addAll("tiny", "med", "muted");
        HBox left = new HBox(6, privacyLabel);
        left.setAlignment(Pos.CENTER_LEFT);

        Label savedLabel = new Label("Last saved just now (Automatic)");
        savedLabel.getStyleClass().addAll("tiny", "med", "muted");
        HBox middle = new HBox(6, savedLabel);
        middle.setAlignment(Pos.CENTER_LEFT);

        Label right = new Label("FRM v1.2.0  •  Salim Habib University Project");
        right.getStyleClass().addAll("tiny", "med", "muted");

        Region leftSpacer  = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer,  Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        HBox bar = new HBox(10, left, leftSpacer, middle, rightSpacer, right);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("statusbar");
        return bar;
    }

    // ---------- Logo ----------

    private static final class ImageLogo extends StackPane {
        ImageLogo() {
            Circle outer = new Circle(11);
            outer.setStyle("-fx-fill: transparent; -fx-stroke: -frm-primary; -fx-stroke-width: 3;");
            Circle inner = new Circle(4.5);
            inner.setStyle("-fx-fill: -frm-orange;");
            getChildren().addAll(outer, inner);
            setMinSize(30, 30);
            setMaxSize(30, 30);
            setStyle("-fx-background-color: -frm-tint; -fx-background-radius: 8;");
        }
    }

    // ---------- Quick action ----------

    /** One-click Log Contact: records a check-in today and restarts the friend's reminder. */
    public void logContactToday(Friend friend, Consumer<Friend> after) {
        LocalDate today = LocalDate.now();
        FriendDAO.markContacted(friend.getId(), today);

        ContactEntry entry = new ContactEntry();
        entry.setFriendId(friend.getId());
        entry.setDate(today);
        entry.setTitle("Quick check-in");
        ContactEntryDAO.insert(entry);

        toast("Logged a check-in with " + friend.firstName() + ".");
        after.accept(friend);
    }
}