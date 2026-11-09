package frmSystem.pages.views;

import frmSystem.db.daos.FriendDAO;
import frmSystem.db.models.Cadence;
import frmSystem.db.models.Friend;
import frmSystem.pages.RootLayout;
import frmSystem.ui.UtilsUI;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;

/** App settings. */
public class SettingsView {
    private final RootLayout app;

    public SettingsView(RootLayout app) {
        this.app = app;
    }

    public Node build() {
        ComboBox<String> cadence = new ComboBox<>();
        for (Cadence option : Cadence.values()) {
            cadence.getItems().add(option.label());
        }
        cadence.setValue(Cadence.MONTHLY.label());

        ComboBox<String> dateFormat = new ComboBox<>();
        dateFormat.getItems().setAll("Oct 24, 2024", "24 Oct 2024", "2024-10-24");
        dateFormat.getSelectionModel().selectFirst();

        ComboBox<String> backups = new ComboBox<>();
        backups.getItems().setAll("Daily", "Weekly", "Monthly");
        backups.getSelectionModel().select(1);

        CheckBox birthdays = new CheckBox();
        birthdays.setSelected(true);

        Button reset = UtilsUI.button("Reset to Defaults", "btn-soft");
        reset.setOnAction(_ -> app.confirm("Reset settings?", "Your friends and notes stay as they are.", "Reset", () -> {
            app.show(RootLayout.Page.SETTINGS);
            app.toast("Settings reset.");
        }));
        Button deleteAll = UtilsUI.button("Delete All Data", "btn-soft");
        deleteAll.setOnAction(_ -> app.confirm(
                "Delete all data?",
                "This removes every friend, note, contact, and gift idea. This cannot be undone.",
                "Delete All",
                () -> {
                    for (Friend f : FriendDAO.findAll()) {
                        FriendDAO.delete(f.getId());   // cascades to notes / contacts / gifts
                    }
                    app.toast("All data deleted.");
                    app.refresh();
                    app.show(RootLayout.Page.SETTINGS);
                }));

        VBox page = new VBox(14,
                UtilsUI.col(4, UtilsUI.label("DATA & SYSTEM", "eyebrow", "c-primary"), UtilsUI.label("Settings", "h1"),
                        UtilsUI.label("Make FRM feel like yours.", "muted")),
                UtilsUI.card(12, UtilsUI.label("Appearance", "h3"),
                        setting("Date format", "How dates appear", dateFormat)),
                UtilsUI.card(12, UtilsUI.label("Reminders", "h3"),
                        setting("Default rhythm", "Used when adding a friend", cadence),
                        setting("Birthday reminders", "Show upcoming birthdays", birthdays)),
                UtilsUI.card(12, UtilsUI.label("Data", "h3"),
                        setting("Automatic backup", "How often to copy your data", backups),
                        UtilsUI.row(8, reset, deleteAll)));
        return RootLayout.scrollPage(UtilsUI.styled(page, "page"));
    }

    private Node setting(String title, String description, Node control) {
        return UtilsUI.styled(UtilsUI.row(8, UtilsUI.col(1, UtilsUI.label(title, "semi"), UtilsUI.label(description, "small", "muted")), UtilsUI.spacer(), control), "tint");
    }
}