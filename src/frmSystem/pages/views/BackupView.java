package frmSystem.pages.views;

import frmSystem.pages.RootLayout;
import frmSystem.ui.UtilsUI;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Backup & Export. Backup copies the SQLite file; Restore replaces it. */
public class BackupView {
    private static final Path DB_FILE = Path.of("students.db");

    private final RootLayout app;

    public BackupView(RootLayout app) {
        this.app = app;
    }

    public Node build() {
        Button friends = UtilsUI.button("Export Friends (CSV)", "btn-soft");
        friends.setOnAction(_ -> Exports.friendsCsv(app));
        Button contacts = UtilsUI.button("Export Contact Log (CSV)", "btn-soft");
        contacts.setOnAction(_ -> Exports.contactsCsv(app));
        Button notes = UtilsUI.button("Export Notes (Markdown)", "btn-soft");
        notes.setOnAction(_ -> Exports.notesMarkdown(app));

        Button backup = UtilsUI.button("Back Up Now", "btn-primary");
        backup.setOnAction(_ -> backupNow());
        Button restore = UtilsUI.button("Restore From Backup", "btn-soft");
        restore.setOnAction(_ -> restoreNow());

        VBox page = new VBox(14,
                UtilsUI.col(4, UtilsUI.label("DATA & SYSTEM", "eyebrow", "c-primary"), UtilsUI.label("Backup & Export", "h1"),
                        UtilsUI.label("Your data stays on this device. Keep a copy wherever you like.", "muted")),
                UtilsUI.card(12, UtilsUI.label("Backup", "h3"),
                        UtilsUI.wrapped("A backup is a full copy of your students.db file.", "muted"),
                        UtilsUI.row(8, backup, restore)),
                UtilsUI.card(12, UtilsUI.label("Export", "h3"),
                        UtilsUI.wrapped("Save your data in plain formats you can open anywhere.", "muted"),
                        UtilsUI.row(8, friends, contacts, notes)));
        return RootLayout.scrollPage(UtilsUI.styled(page, "page"));
    }

    private void backupNow() {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("students-backup.db");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite database", "*.db"));
        File target = chooser.showSaveDialog(app.stage());
        if (target == null) {
            return;
        }
        try {
            Files.copy(DB_FILE, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            app.toast("Backup saved to " + target.getName() + ".");
        } catch (IOException e) {
            app.toast("Backup failed. Close the app and copy students.db manually.");
        }
    }

    private void restoreNow() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite database", "*.db"));
        File source = chooser.showOpenDialog(app.stage());
        if (source == null) {
            return;
        }
        app.confirm("Restore from " + source.getName() + "?",
                "This replaces your current data. The app should be restarted afterwards.",
                "Restore",
                () -> {
                    try {
                        Files.copy(source.toPath(), DB_FILE, StandardCopyOption.REPLACE_EXISTING);
                        app.toast("Restored from " + source.getName() + ". Please restart the app.");
                    } catch (IOException e) {
                        app.toast("Restore failed. Make sure the app is closed and try again.");
                    }
                });
    }
}