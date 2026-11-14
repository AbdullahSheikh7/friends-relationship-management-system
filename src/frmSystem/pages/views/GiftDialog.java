package frmSystem.pages.views;

import frmSystem.db.daos.FriendDAO;
import frmSystem.db.daos.GiftIdeaDAO;
import frmSystem.db.models.Friend;
import frmSystem.db.models.GiftIdea;
import frmSystem.pages.RootLayout;
import frmSystem.ui.UtilsUI;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.Comparator;

/** Creates a gift idea for a friend. The friend comes preselected when the
 *  dialog opens from a profile, otherwise it offers the full active directory. */
public class GiftDialog {
    private final RootLayout app;
    private final Friend preselected;

    public GiftDialog(RootLayout app, Friend preselected) {
        this.app = app;
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
        TextField item = new TextField();
        item.setPromptText("Gift idea (e.g. Vinyl record)");
        item.getStyleClass().add("field");
        TextField price = new TextField();
        price.setPromptText("Price in USD (optional)");
        price.getStyleClass().add("field");
        Label error = UtilsUI.label("", "field-error");

        if (preselected != null) {
            friendBox.setValue(preselected);
        }

        Button cancel = UtilsUI.button("Cancel", "btn-soft");
        cancel.setOnAction(_ -> app.closeModal());
        Button save = UtilsUI.button("Save Gift Idea", "btn-primary");
        save.setOnAction(_ -> {
            if (friendBox.getValue() == null) {
                error.setText("Please select a friend.");
                return;
            }
            if (item.getText().isBlank()) {
                error.setText("Please write the gift idea first.");
                return;
            }
            int priceUsd = 0;
            String priceText = price.getText().replace("$", "").trim();
            if (!priceText.isEmpty()) {
                try {
                    priceUsd = Integer.parseInt(priceText);
                } catch (NumberFormatException e) {
                    error.setText("Price must be a whole number.");
                    return;
                }
                if (priceUsd < 0) {
                    error.setText("Price cannot be negative.");
                    return;
                }
            }
            GiftIdea gift = new GiftIdea();
            gift.setFriendId(friendBox.getValue().getId());
            gift.setItem(item.getText().trim());
            gift.setPriceUsd(priceUsd);
            gift.setPurchased(false);
            GiftIdeaDAO.insert(gift);
            app.closeModal();
            app.toast("Gift idea saved for " + friendBox.getValue().firstName() + ".");
            app.refresh();
        });

        Node actions = UtilsUI.row(8, UtilsUI.spacer(), cancel, save);
        VBox modal = new VBox(10, UtilsUI.label("New Gift Idea", "h2"), friendBox, item, price, error, actions);
        modal.getStyleClass().add("modal");
        modal.setPadding(new Insets(22));
        modal.setMaxSize(500, Region.USE_PREF_SIZE);
        app.openModal(modal);
    }
}
