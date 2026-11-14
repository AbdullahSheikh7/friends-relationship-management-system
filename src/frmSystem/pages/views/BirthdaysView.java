package frmSystem.pages.views;

import frmSystem.db.daos.FriendDAO;
import frmSystem.db.daos.GiftIdeaDAO;
import frmSystem.db.daos.PrepStepDAO;
import frmSystem.db.models.Friend;
import frmSystem.db.models.GiftIdea;
import frmSystem.db.models.PrepStep;
import frmSystem.pages.RootLayout;
import frmSystem.ui.UtilsUI;

import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Birthdays & Milestones: upcoming list or monthly grid, calendar, gift ideas, and reminders. */
public class BirthdaysView {
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter LONG_DAY = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_SHORT = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    private static final String[] WEEK = {"S", "M", "T", "W", "T", "F", "S"};

    private final RootLayout app;
    private final VBox main = new VBox(14);
    private final HBox modeTabs = UtilsUI.styled(UtilsUI.row(2), "seg");
    private final VBox calendarCard = UtilsUI.card(10);
    private final ComboBox<String> spaceBox = new ComboBox<>();
    private final ComboBox<String> horizonBox = new ComboBox<>();
    private YearMonth month = YearMonth.now();
    private boolean grid;

    public BirthdaysView(RootLayout app) {
        this.app = app;
    }

    public Node build() {
        List<Friend> people = withBirthdays();
        HBox crumbs = UtilsUI.row(8, UtilsUI.label("CALENDAR & CELEBRATIONS", "eyebrow", "c-primary"), UtilsUI.icon(MaterialDesignC.CIRCLE_SMALL, 8, "ic-muted"),
                UtilsUI.label(people.size() + " Milestones Tracked", "small", "med"));
        VBox title = UtilsUI.col(4, crumbs, UtilsUI.label("Birthdays & Milestones", "h1"),
                UtilsUI.wrapped("Never be caught by surprise. Thoughtful reminders without artificial social-media pressure or productivity guilt.", "muted"));
        title.setMaxWidth(560);
        HBox head = UtilsUI.row(10, title);
        head.setAlignment(Pos.BOTTOM_LEFT);

        renderMode();
        spaceBox.getItems().setAll("All Spaces", "Work", "School / Uni", "Neighborhood", "Family Circle", "Online & Gaming");
        spaceBox.getSelectionModel().selectFirst();
        spaceBox.setOnAction(_ -> renderMain());
        horizonBox.getItems().setAll("Next 30 Days", "Next 90 Days", "Next 12 Months");
        horizonBox.getSelectionModel().select(2);
        horizonBox.setOnAction(_ -> renderMain());
        HBox filters = UtilsUI.row(10, modeTabs, spaceBox, horizonBox);

        VBox right = new VBox(14, calendarCard, guideCard(), giftsCard());
        right.setMinWidth(300);
        right.setMaxWidth(300);
        renderCalendar();
        main.setMinWidth(0);
        HBox.setHgrow(main, Priority.ALWAYS);
        renderMain();
        return RootLayout.scrollPage(UtilsUI.styled(new VBox(14, head, filters, new HBox(16, main, right)), "page"));
    }

    private List<Friend> withBirthdays() {
        return FriendDAO.findAll().stream().filter(f -> !f.isArchived() && f.getBirthday() != null)
                .sorted(Comparator.comparing(Friend::daysUntilBirthday)).toList();
    }

    private void renderMode() {
        Button list = UtilsUI.button("Upcoming List", "seg-btn");
        Button monthly = UtilsUI.button("Monthly Grid", "seg-btn");
        (grid ? monthly : list).getStyleClass().add("on");
        list.setOnAction(_ -> {
            grid = false;
            renderMode();
            renderMain();
        });
        monthly.setOnAction(_ -> {
            grid = true;
            renderMode();
            renderMain();
        });
        modeTabs.getChildren().setAll(list, monthly);
    }

    private List<Friend> visible() {
        int horizon = switch (horizonBox.getSelectionModel().getSelectedIndex()) {
            case 0 -> 30;
            case 1 -> 90;
            default -> 366;
        };
        boolean anySpace = spaceBox.getSelectionModel().getSelectedIndex() <= 0;
        return withBirthdays().stream()
                .filter(f -> f.daysUntilBirthday() <= horizon)
                .filter(f -> anySpace || f.getSpace().equals(spaceBox.getValue()))
                .toList();
    }

    private void renderMain() {
        main.getChildren().clear();
        if (grid) {
            main.getChildren().add(monthGrid());
            return;
        }
        List<Friend> people = visible();
        if (people.isEmpty()) {
            main.getChildren().add(UtilsUI.card(6, UtilsUI.label("No birthdays in this window.", "h4"), UtilsUI.label("Try a wider horizon.", "muted")));
            return;
        }
        main.getChildren().add(hero(people.get(0)));
        YearMonth thisMonth = YearMonth.now();
        Map<YearMonth, List<Friend>> byMonth = new LinkedHashMap<>();
        List<Friend> later = new ArrayList<>();
        for (Friend f : people) {
            YearMonth key = YearMonth.from(f.nextBirthday());
            if (key.isAfter(thisMonth.plusMonths(1))) {
                later.add(f);
            } else {
                byMonth.computeIfAbsent(key, _ -> new ArrayList<>()).add(f);
            }
        }
        byMonth.forEach((key, friends) -> {
            String hint = key.equals(thisMonth) ? "This Month" : "Next Month";
            main.getChildren().add(monthHeader(key.format(MONTH), hint + " · " + friends.size() + " Celebrations",
                    key.equals(thisMonth) ? "Current Window" : "Ahead of time", key.equals(thisMonth)));
            friends.forEach(f -> main.getChildren().add(birthdayRow(f)));
        });
        if (!later.isEmpty()) {
            main.getChildren().add(monthHeader("Later & Beyond", "Upcoming Highlights", "", false));
            GridPane cards = new GridPane();
            cards.setHgap(10);
            cards.setVgap(10);
            for (int i = 0; i < 3; i++) {
                ColumnConstraints column = new ColumnConstraints();
                column.setPercentWidth(100 / 3.0);
                cards.getColumnConstraints().add(column);
            }
            for (int i = 0; i < later.size(); i++) {
                cards.add(laterCard(later.get(i)), i % 3, i / 3);
            }
            main.getChildren().add(cards);
        }
    }

    private Node monthHeader(String title, String hint, String side, boolean current) {
        return UtilsUI.row(8, UtilsUI.icon(MaterialDesignC.CIRCLE_SMALL, 8, current ? "ic-primary" : "ic-muted"), UtilsUI.label(title, "h3"), UtilsUI.label(hint, "small", "muted"),
                UtilsUI.spacer(), UtilsUI.label(side, "small", "semi", "muted"));
    }

    private Node hero(Friend f) {
        HBox top = UtilsUI.row(8, UtilsUI.bullet("APPROACHING", "pill", "pill-danger", "pill-big"), UtilsUI.label(UtilsUI.inTime(f.daysUntilBirthday()) + " (" + UtilsUI.shortDate(f.nextBirthday()) + ")", "small", "semi", "muted"),
                UtilsUI.spacer(), UtilsUI.label("Private Reminder", "small", "med"));
        HBox nameRow = UtilsUI.row(8, UtilsUI.label(f.getName(), "h3"), UtilsUI.pill(f.getCloseness().label(), UtilsUI.closenessKind(f)), UtilsUI.pill(f.getSpace(), "pill-strong"));
        Label turning = UtilsUI.label("Turning " + f.turningAge() + " · " + f.nextBirthday().format(LONG_DAY), "h4", "c-danger");
        VBox info = UtilsUI.col(6, nameRow, turning);
        giftFor(f).ifPresent(g -> info.getChildren().add(UtilsUI.styled(UtilsUI.row(10,
                UtilsUI.col(1, UtilsUI.label("ATTACHED GIFT IDEA NOTE", "eyebrow", "muted"), UtilsUI.label(g.getItem(), "med"))), "tint")));
        HBox.setHgrow(info, Priority.ALWAYS);
        HBox middle = UtilsUI.row(16, UtilsUI.avatar(f, 84), info);

        Button gifts = UtilsUI.button("View Gift Ideas & Notes", "btn-primary");
        gifts.setOnAction(_ -> app.showFriendProfile(f.getId()));
        Button message = UtilsUI.button("Send Message / Card", "btn-soft");
        message.setOnAction(_ -> app.openLogContact(f));
        Button snooze = UtilsUI.button("Snooze 2 days", "btn-plain");
        snooze.setOnAction(_ -> app.toast("Birthday reminder snoozed for 2 days."));
        VBox card = new VBox(14, top, middle, UtilsUI.row(8, gifts, message, UtilsUI.spacer(), snooze));
        card.getStyleClass().add("hero");
        return card;
    }

    private java.util.Optional<GiftIdea> giftFor(Friend f) {
        return GiftIdeaDAO.findByFriend(f.getId()).stream().filter(g -> !g.isPurchased()).findFirst();
    }

    private Node dateBox(Friend f, boolean milestone) {
        VBox box = UtilsUI.col(0, UtilsUI.label(f.nextBirthday().format(MONTH_SHORT).toUpperCase(Locale.ENGLISH), "tiny", "semi", milestone ? "c-warn" : "c-primary"),
                UtilsUI.label(String.valueOf(f.nextBirthday().getDayOfMonth()), "h2"));
        box.getStyleClass().add("date-box");
        if (milestone) {
            box.getStyleClass().add("date-box-warn");
        }
        box.setMinWidth(58);
        return box;
    }

    private Node birthdayRow(Friend f) {
        boolean milestone = f.turningAge() % 10 == 0;
        HBox nameRow = UtilsUI.row(8, UtilsUI.label(f.getName(), "h4"));
        if (milestone) {
            nameRow.getChildren().add(UtilsUI.pill(f.turningAge() + "th Milestone!", "pill-brown"));
        }
        nameRow.getChildren().add(UtilsUI.pill(UtilsUI.inTime(f.daysUntilBirthday()), f.daysUntilBirthday() <= 7 ? "pill-danger" : "pill-strong"));
        VBox text = UtilsUI.col(2, nameRow, UtilsUI.label("Turning " + f.turningAge() + " · " + f.getCloseness().label() + " · " + f.getSpace(), "small", "muted"));
        giftFor(f).ifPresent(g -> text.getChildren().add(UtilsUI.row(6, UtilsUI.label(g.getItem() + " on wishlist", "small"))));
        Button action;
        if (milestone) {
            action = UtilsUI.button("Group Plan", "btn-brown");
            action.setOnAction(_ -> app.showFriendProfile(f.getId()));
        } else if (f.daysUntilBirthday() <= 7) {
            action = UtilsUI.button("Alert Active", "btn-soft");
            action.setOnAction(_ -> app.showFriendProfile(f.getId()));
        } else {
            action = UtilsUI.button("Note", "btn-plain");
            action.setOnAction(_ -> new NoteDialog(app, null, f).open());
        }
        HBox row = UtilsUI.row(12, dateBox(f, milestone), UtilsUI.avatar(f, 42), text, UtilsUI.spacer(), action);
        row.getStyleClass().add("card");
        if (milestone) {
            row.getStyleClass().add("accent-left");
        }
        row.setOnMouseClicked(_ -> app.showFriendProfile(f.getId()));
        return row;
    }

    private Node laterCard(Friend f) {
        HBox top = UtilsUI.row(4, UtilsUI.pill(UtilsUI.shortDate(f.nextBirthday()).toUpperCase(Locale.ENGLISH), "pill-strong"), UtilsUI.spacer());
        VBox card = UtilsUI.card(6, top, UtilsUI.label(f.getName(), "h4"), UtilsUI.label("Turning " + f.turningAge(), "small", "muted"), UtilsUI.label(UtilsUI.inTime(f.daysUntilBirthday()), "small", "med"));
        card.setOnMouseClicked(_ -> app.showFriendProfile(f.getId()));
        card.setStyle("-fx-cursor: hand;");
        return card;
    }

    private Node monthGrid() {
        List<Friend> people = visible();
        GridPane cal = new GridPane();
        cal.setHgap(6);
        cal.setVgap(6);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100 / 7.0);
            cal.getColumnConstraints().add(column);
            Label day = UtilsUI.label(new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"}[i], "small", "semi", "muted");
            cal.add(day, i, 0);
        }
        LocalDate first = month.atDay(1);
        int offset = first.getDayOfWeek() == DayOfWeek.SUNDAY ? 0 : first.getDayOfWeek().getValue();
        for (int d = 1; d <= month.lengthOfMonth(); d++) {
            LocalDate date = month.atDay(d);
            VBox cell = UtilsUI.styled(UtilsUI.col(3, UtilsUI.label(String.valueOf(d), "small", "semi")), "cal-cell");
            people.stream().filter(f -> f.nextBirthday().equals(date)).forEach(f -> {
                Label pill = UtilsUI.pill(f.firstName(), "pill-danger");
                pill.setOnMouseClicked(_ -> app.showFriendProfile(f.getId()));
                pill.setStyle("-fx-cursor: hand;");
                cell.getChildren().add(pill);
            });
            int position = offset + d - 1;
            cal.add(cell, position % 7, position / 7 + 1);
        }
        return UtilsUI.card(12, UtilsUI.label(month.format(MONTH), "h3"), cal);
    }

    private void renderCalendar() {
        Button previous = UtilsUI.iconButton(MaterialDesignC.CHEVRON_LEFT, "btn-plain");
        previous.setOnAction(_ -> {
            month = month.minusMonths(1);
            renderCalendar();
            if (grid) {
                renderMain();
            }
        });
        Button next = UtilsUI.iconButton(MaterialDesignC.CHEVRON_RIGHT, "btn-plain");
        next.setOnAction(_ -> {
            month = month.plusMonths(1);
            renderCalendar();
            if (grid) {
                renderMain();
            }
        });
        HBox title = UtilsUI.row(6, UtilsUI.label(month.format(MONTH), "h4"), UtilsUI.spacer(), previous, next);

        GridPane cal = new GridPane();
        cal.setHgap(2);
        cal.setVgap(3);
        cal.setAlignment(Pos.CENTER);
        for (int i = 0; i < 7; i++) {
            Label head = UtilsUI.label(WEEK[i], "cal-day", "semi", "muted");
            cal.add(head, i, 0);
        }
        LocalDate first = month.atDay(1);
        int offset = first.getDayOfWeek() == DayOfWeek.SUNDAY ? 0 : first.getDayOfWeek().getValue();
        LocalDate date = first.minusDays(offset);
        List<Friend> people = withBirthdays();
        LocalDate nextBirthday = people.isEmpty() ? null : people.get(0).nextBirthday();
        for (int i = 0; i < 42; i++) {
            LocalDate day = date.plusDays(i);
            if (i >= 35 && day.getMonthValue() != month.getMonthValue()) {
                break;
            }
            Label cell = UtilsUI.label(String.valueOf(day.getDayOfMonth()), "cal-day");
            if (day.getMonthValue() != month.getMonthValue()) {
                cell.getStyleClass().add("cal-other");
            } else if (day.equals(nextBirthday)) {
                cell.getStyleClass().add("cal-next");
            } else if (people.stream().anyMatch(f -> f.nextBirthday().equals(day))) {
                cell.getStyleClass().add("cal-birthday");
            } else if (day.equals(LocalDate.now())) {
                cell.getStyleClass().add("cal-today");
            }
            StackPane holder = new StackPane(cell);
            cal.add(holder, i % 7, i / 7 + 1);
        }
        HBox legend = UtilsUI.row(10, UtilsUI.bullet("Approaching", "tiny", "med", "c-danger"), UtilsUI.bullet("Birthday", "tiny", "med", "c-warn"), UtilsUI.bullet("Today", "tiny", "med", "c-primary"));
        calendarCard.getChildren().setAll(title, cal, legend);
    }

    private Node guideCard() {
        List<Friend> milestones = withBirthdays().stream().filter(f -> f.turningAge() % 10 == 0).toList();
        String who = milestones.isEmpty() ? "your friend" : milestones.get(0).firstName();
        VBox items = new VBox(8);
        for (PrepStep step : PrepStepDAO.findAll()) {
            items.getChildren().add(stepRow(step));
        }
        Button add = UtilsUI.button("+ Add preparation step", "btn-soft");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setOnAction(_ -> addStepDialog());
        return UtilsUI.card(10, UtilsUI.row(8, UtilsUI.col(1, UtilsUI.label("Milestone Preparation Guide", "h4"), UtilsUI.label("For big birthdays (30th, 40th, weddings)", "tiny", "semi", "muted"))),
                UtilsUI.wrapped("Entering a new decade is a significant emotional threshold. Here is a thoughtful checklist for " + who + "'s upcoming milestone:", "small", "muted"),
                items, add);
    }

    private Node stepRow(PrepStep step) {
        CheckBox box = new CheckBox();
        box.setSelected(step.isDone());
        Label label = UtilsUI.wrapped(step.getText(), "small");
        if (step.isDone()) {
            label.getStyleClass().add("strike");
        }
        box.setOnAction(_ -> {
            PrepStepDAO.setDone(step.getId(), box.isSelected());
            if (box.isSelected()) {
                label.getStyleClass().add("strike");
            } else {
                label.getStyleClass().remove("strike");
            }
        });
        Button trash = UtilsUI.iconButton(MaterialDesignD.DELETE_OUTLINE, "icon-btn");
        trash.setOnAction(_ -> app.confirm("Delete preparation step", "Remove \"" + step.getText() + "\" from the guide?", "Delete", () -> {
            PrepStepDAO.delete(step.getId());
            app.toast("Preparation step removed.");
            app.refresh();
        }));
        HBox row = UtilsUI.row(8, box, label, UtilsUI.spacer(), trash);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private void addStepDialog() {
        TextField input = new TextField();
        input.setPromptText("e.g. Reserve a table at their favorite place");
        input.getStyleClass().add("field");
        Label error = UtilsUI.label("", "field-error");
        Button cancel = UtilsUI.button("Cancel", "btn-soft");
        cancel.setOnAction(_ -> app.closeModal());
        Button save = UtilsUI.button("Add Step", "btn-primary");
        save.setOnAction(_ -> {
            String text = input.getText().trim();
            if (text.isEmpty()) {
                error.setText("Please write the step first.");
                return;
            }
            PrepStepDAO.insert(text);
            app.closeModal();
            app.toast("Preparation step added.");
            app.refresh();
        });
        Node actions = UtilsUI.row(8, UtilsUI.spacer(), cancel, save);
        VBox modal = new VBox(10, UtilsUI.label("New Preparation Step", "h2"), input, error, actions);
        modal.getStyleClass().add("modal");
        modal.setPadding(new Insets(22));
        modal.setMaxSize(460, Region.USE_PREF_SIZE);
        app.openModal(modal);
    }

    private Node giftsCard() {
        List<GiftIdea> gifts = GiftIdeaDAO.findAll();
        long open = gifts.stream().filter(g -> !g.isPurchased()).count();
        Button add = UtilsUI.button("Add Gift Idea", "btn-soft");
        add.setOnAction(_ -> new GiftDialog(app, null).open());
        VBox card = UtilsUI.card(8, UtilsUI.row(8, UtilsUI.label("Gift Ideas Registry", "h4"), UtilsUI.spacer(), add, UtilsUI.pill(open + " Unpurchased", "pill-mint")));
        if (gifts.isEmpty()) {
            card.getChildren().add(UtilsUI.label("No gift ideas yet — add the first one.", "small", "muted"));
        }
        gifts.forEach(g -> {
            CheckBox box = new CheckBox();
            box.setSelected(g.isPurchased());
            box.setOnAction(_ -> {
                g.setPurchased(box.isSelected());
                GiftIdeaDAO.update(g);
                app.refresh();
            });
            String owner = FriendDAO.findById(g.getFriendId()).map(f -> f.getName() + " (" + UtilsUI.shortDate(f.nextBirthday()) + ")").orElse("");
            VBox text = UtilsUI.col(1, UtilsUI.label(g.getItem(), "small", "semi"), UtilsUI.label("For " + owner, "tiny", "muted"));
            Button trash = UtilsUI.iconButton(MaterialDesignD.DELETE_OUTLINE, "icon-btn");
            trash.setOnAction(_ -> app.confirm("Delete gift idea", "Remove \"" + g.getItem() + "\" from the registry?", "Delete", () -> {
                GiftIdeaDAO.delete(g.getId());
                app.toast("Gift idea removed.");
                app.refresh();
            }));
            card.getChildren().add(UtilsUI.styled(UtilsUI.row(8, box, text, UtilsUI.spacer(), UtilsUI.label("$" + g.getPriceUsd(), "small", "semi", "c-primary"), trash), "tint"));
        });
        return card;
    }
}