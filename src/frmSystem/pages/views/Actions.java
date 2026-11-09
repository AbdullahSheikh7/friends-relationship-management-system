package frmSystem.pages.views;

import frmSystem.db.daos.FriendDAO;
import frmSystem.db.models.Friend;
import frmSystem.pages.RootLayout;
import frmSystem.ui.UtilsUI;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import java.time.LocalDate;

/** Friend actions used on several screens. */
final class Actions {
    private Actions() {
    }

    static Button logContactButton(RootLayout app, Friend friend) {
        Button button = UtilsUI.button("Log Contact", "btn-primary", "btn-round");
        button.setOnAction(_ -> app.logContactToday(friend, _ -> app.refresh()));
        return button;
    }

    static Button snoozeButton(RootLayout app, Friend friend) {
        Button button = UtilsUI.button("Snooze", "btn-soft", "btn-round");
        button.setGraphicTextGap(6);
        button.setOnAction(_ -> snoozeMenu(app, friend).show(button, Side.BOTTOM, 0, 4));
        return button;
    }

    static ContextMenu snoozeMenu(RootLayout app, Friend friend) {
        ContextMenu menu = new ContextMenu();
        addSnooze(menu, app, friend, "2 days", 2);
        addSnooze(menu, app, friend, "1 week", 7);
        addSnooze(menu, app, friend, "2 weeks", 14);
        return menu;
    }

    private static void addSnooze(ContextMenu menu, RootLayout app, Friend friend, String text, int days) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(_ -> {
            FriendDAO.snooze(friend.getId(), LocalDate.now().plusDays(days));
            app.toast("Snoozed " + friend.firstName() + " for " + text + ".");
            app.refresh();
        });
        menu.getItems().add(item);
    }

    /** More menu for a friend: edit, favorite, archive, delete. */
    static ContextMenu friendMenu(RootLayout app, Friend friend, Runnable afterDelete) {
        MenuItem open = new MenuItem("View profile");
        open.setOnAction(_ -> app.showFriendProfile(friend.getId()));
        MenuItem edit = new MenuItem("Edit profile");
        edit.setOnAction(_ -> new AddFriendDialog(app, friend).open());
        MenuItem favorite = new MenuItem(friend.isFavorite() ? "Remove from favorites" : "Add to favorites");
        favorite.setOnAction(_ -> {
            friend.setFavorite(!friend.isFavorite());
            FriendDAO.update(friend);
            app.refresh();
        });
        MenuItem archive = new MenuItem(friend.isArchived() ? "Restore from archive" : "Archive");
        archive.setOnAction(_ -> {
            friend.setArchived(!friend.isArchived());
            FriendDAO.update(friend);
            app.toast(friend.isArchived() ? friend.firstName() + " moved to the archive." : friend.firstName() + " restored.");
            app.refresh();
        });
        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(_ -> app.confirm("Delete " + friend.getName() + "?",
                "This removes the friend from your lists. You can undo right after.", "Delete", () -> {
                    Friend snapshot = friend;
                    FriendDAO.delete(friend.getId());
                    afterDelete.run();
                    app.toast(friend.getName() + " deleted.", "Undo", () -> {
                        FriendDAO.insert(snapshot);   // gets a new id; see note below
                        app.refresh();
                    });
                }));
        return new ContextMenu(open, edit, favorite, archive, delete);
    }

    static void showBelow(ContextMenu menu, Node anchor) {
        menu.show(anchor, Side.BOTTOM, 0, 4);
    }
}