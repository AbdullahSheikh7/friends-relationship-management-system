package frmSystem.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import frmSystem.db.models.Friend;
import frmSystem.db.models.FriendStatus;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class UtilsUI {
    private static final List<String> AVATAR_COLORS = List.of("#ffd9d3", "#ffdcc2", "#9ff2e0", "#bde6fb", "#ffc2b8", "#d5e8f5");
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

    public static Label label(String text, String... classes) {
        Label label = new Label(text);
        label.getStyleClass().addAll(classes);
        return label;
    }

    public static FontIcon icon(Ikon ikon, int size, String... classes) {
        FontIcon fontIcon = new FontIcon(ikon);
        fontIcon.setIconSize(size);
        fontIcon.getStyleClass().addAll(classes);
        return fontIcon;
    }

    public static Button iconButton(Ikon ikon, String... classes) {
        Button button = new Button();
        button.setGraphic(icon(ikon, 14));
        button.getStyleClass().setAll(classes);
        button.setMinWidth(Region.USE_PREF_SIZE);
        return button;
    }

    public static Label bullet(String text, String... classes) {
        Label label = label(text, classes);
        label.setGraphic(icon(MaterialDesignC.CIRCLE_SMALL, 8, "ic-muted"));
        label.setGraphicTextGap(5);
        return label;
    }

    public static Label wrapped(String text, String... classes) {
        Label label = label(text, classes);
        label.setWrapText(true);
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    public static HBox row(double gap, Node... nodes) {
        HBox box = new HBox(gap, nodes);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    public static VBox col(double gap, Node... nodes) {
        return new VBox(gap, nodes);
    }

    public static Region spacer() {
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        VBox.setVgrow(region, Priority.ALWAYS);
        return region;
    }

    public static <T extends Region> T styled(T node, String... classes) {
        node.getStyleClass().addAll(classes);
        return node;
    }

    public static VBox card(double gap, Node... nodes) {
        return styled(col(gap, nodes), "card");
    }

    public static Label pill(String text, String... kinds) {
        Label label = label(text, "pill");
        label.getStyleClass().addAll(kinds);
        label.setMinWidth(Region.USE_PREF_SIZE);
        return label;
    }

    public static Button button(String text, String... classes) {
        Button button = new Button(text);
        button.getStyleClass().setAll(classes);
        button.setMinWidth(Region.USE_PREF_SIZE);
        return button;
    }

    public static StackPane tile(String kind) {
        StackPane tile = new StackPane();
        tile.getStyleClass().addAll("tile", kind);
        return tile;
    }

    public static StackPane avatar(Friend friend, int size) {
        Circle circle = new Circle(size / 2.0);
        circle.setFill(Color.web(AVATAR_COLORS.get(Math.floorMod(friend.getName().hashCode(), AVATAR_COLORS.size()))));
        Label initials = label(friend.initials(), "avatar-label");
        initials.setStyle("-fx-font-size: " + Math.max(10, size * 0.32) + "px;");
        StackPane avatar = new StackPane(circle, initials);
        avatar.getStyleClass().add("avatar");
        avatar.setMinSize(size, size);
        avatar.setMaxSize(size, size);
        return avatar;
    }

    public static String statusText(Friend friend) {
        long left = friend.daysUntilDue();
        return switch (friend.status()) {
            case NEEDS_HELLO -> "Needs hello, " + (-left) + "d overdue";
            case DUE_SOON -> left == 0 ? "Due today" : "Due in " + left + " " + (left == 1 ? "day" : "days");
            case ON_TRACK -> "On track, contact in " + left + " days";
        };
    }

    public static String statusKind(FriendStatus status) {
        return switch (status) {
            case NEEDS_HELLO -> "pill-danger";
            case DUE_SOON -> "pill-warn";
            case ON_TRACK -> "pill-mint";
        };
    }

    public static Label statusPill(Friend friend) {
        return pill(statusText(friend), statusKind(friend.status()), "pill-big");
    }

    public static String closenessKind(Friend friend) {
        return switch (friend.getCloseness()) {
            case VERY_CLOSE -> "pill-danger";
            case CLOSE -> "pill-mint";
            case CASUAL -> "pill-strong";
        };
    }

    public static String longDate(LocalDate date) {
        return date == null ? "" : LONG_DATE.format(date);
    }

    public static String shortDate(LocalDate date) {
        return date == null ? "" : SHORT_DATE.format(date);
    }

    public static String ago(LocalDate date) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(date, LocalDate.now());
        if (days <= 0) {
            return "today";
        }
        if (days < 14) {
            return days + (days == 1 ? " day ago" : " days ago");
        }
        if (days < 60) {
            return (days / 7) + " weeks ago";
        }
        return (days / 30) + " months ago";
    }

    public static String inTime(long days) {
        if (days == 0) {
            return "Today";
        }
        if (days < 14) {
            return "In " + days + (days == 1 ? " day" : " days");
        }
        return "In " + Math.round(days / 7.0) + " weeks";
    }
}