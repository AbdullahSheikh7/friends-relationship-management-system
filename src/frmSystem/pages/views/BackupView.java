package frmSystem.pages.views;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import frmSystem.db.SnapshotStack;
import frmSystem.db.daos.BackupDAO;
import frmSystem.pages.RootLayout;
import frmSystem.ui.UtilsUI;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Backups managed entirely by the app: snapshot versions live inside the vault
 *  database and restores follow the LIFO rule — only the newest version (stack
 *  top) can be restored, and restoring pops it off. */
public class BackupView {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.ENGLISH);

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

        Button create = UtilsUI.button("Create Snapshot Now", "btn-primary");
        create.setOnAction(_ -> app.confirm("Create a backup snapshot?",
                "Your whole database is captured into the vault as a new version on the restore stack.",
                "Create",
                () -> app.runBusy("Saving snapshot...",
                        () -> BackupDAO.insert("Snapshot — " + LocalDateTime.now().format(STAMP)),
                        () -> {
                            app.show(RootLayout.Page.BACKUP);
                            app.toast("Snapshot pushed onto the stack. Newer versions restore first (LIFO).");
                        })));

        VBox page = new VBox(14,
                UtilsUI.col(4, UtilsUI.label("DATA & SYSTEM", "eyebrow", "c-primary"), UtilsUI.label("Backup & History", "h1"),
                        UtilsUI.label("Versions live inside the vault — nothing to carry, nothing to misplace.", "muted")),
                stackCard(create),
                UtilsUI.card(12, UtilsUI.label("Export", "h3"),
                        UtilsUI.wrapped("Plain copies of your data you can open anywhere.", "muted"),
                        UtilsUI.row(8, friends, contacts, notes)));
        return RootLayout.scrollPage(UtilsUI.styled(page, "page"));
    }

    private Node stackCard(Button create) {
        List<BackupDAO.Snapshot> snapshots = BackupDAO.findAll();

        SnapshotStack stack = new SnapshotStack();
        snapshots.forEach(s -> stack.push(s.id()));

        VBox rows = new VBox(8);
        if (snapshots.isEmpty()) {
            rows.getChildren().add(UtilsUI.wrapped(
                    "No snapshots yet. Create the first one — it becomes the top of the restore stack.", "muted"));
        } else {
            for (BackupDAO.Snapshot s : snapshots) {
                boolean top = s.id() == stack.peek();
                rows.getChildren().add(row(s, top));
            }
        }

        return UtilsUI.card(12,
                UtilsUI.row(10,
                        tile("tile-primary", MaterialDesignD.DATABASE, "ic-white"),
                        UtilsUI.col(2,
                                UtilsUI.label("Snapshot Versions  ·  " + snapshots.size() + " on the stack", "h4"),
                                UtilsUI.wrapped("Restore always pops the newest version first — restoring a lower version requires popping every one above it.", "small", "muted")),
                        UtilsUI.spacer(),
                        create),
                rows);
    }

    private Node row(BackupDAO.Snapshot s, boolean top) {
        VBox info = UtilsUI.col(1,
                UtilsUI.label(s.label(), "semi"),
                UtilsUI.label(s.createdAt() + "  ·  " + s.friends() + " friends  ·  " + s.notes() + " notes  ·  "
                        + s.contacts() + " contacts  ·  " + s.gifts() + " gifts", "small", "muted"));

        HBox row = UtilsUI.row(10,
                tile(top ? "tile-primary" : "tile-info",
                        top ? MaterialDesignR.RESTORE : MaterialDesignD.DATABASE_OUTLINE,
                        top ? "ic-white" : "ic-muted"),
                info,
                UtilsUI.spacer());

        if (top) {
            row.getChildren().add(UtilsUI.pill("TOP OF STACK", "pill-primary", "pill-big"));
            Button restore = UtilsUI.button("Restore", "btn-primary");
            restore.setOnAction(_ -> confirmRestore(s));
            row.getChildren().add(restore);
        } else {
            row.getChildren().add(UtilsUI.pill("Waiting for pops", "pill-strong"));
        }
        return row;
    }

    private Node tile(String kind, Ikon ikon, String colorClass) {
        StackPane tile = UtilsUI.tile(kind);
        tile.getChildren().add(UtilsUI.icon(ikon, 16, colorClass));
        return tile;
    }

    private void confirmRestore(BackupDAO.Snapshot s) {
        app.confirm("Restore from " + s.label() + "?",
                "Your live data will be replaced by this version, which is then popped off the stack.",
                "Restore",
                () -> app.runBusy("Restoring snapshot...",
                        () -> {
                            BackupDAO.restore(s.id());
                            BackupDAO.delete(s.id());
                        },
                        () -> {
                            app.show(RootLayout.Page.BACKUP);
                            app.toast("Version popped from the stack. Your data now matches " + s.label() + ".");
                        }));
    }
}
