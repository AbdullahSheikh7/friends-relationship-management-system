package frmSystem.pages.views;

import frmSystem.pages.RootLayout;
import frmSystem.ui.UtilsUI;

import javafx.scene.Node;
import javafx.scene.layout.VBox;

/** Built-in help, shortcuts, and version details. */
public class HelpView {

    public Node build() {
        VBox page = new VBox(14,
                UtilsUI.col(4, UtilsUI.label("DATA & SYSTEM", "eyebrow", "c-primary"), UtilsUI.label("Help & About", "h1"),
                        UtilsUI.label("Everything you need, without the pressure.", "muted")),
                UtilsUI.card(10, UtilsUI.label("Getting started", "h3"),
                        qa("How do I add a friend?", "Click Add Friend in the sidebar and follow the six steps. Only the name is required."),
                        qa("What does Log Contact do?", "It records that you were in touch today and restarts that friend's reminder."),
                        qa("Can I log a past catch-up?", "Yes. Use Quick Log Contact and pick the date."),
                        qa("Where is my data?", "Only on this device. FRM never sends anything over the network.")),
                UtilsUI.card(10, UtilsUI.label("Keyboard shortcuts", "h3"), shortcut("Ctrl+N", "Add a friend"),
                        shortcut("Ctrl+E", "Quick log contact"), shortcut("Ctrl+J", "New note"), shortcut("Esc", "Close a window")),
                UtilsUI.card(6, UtilsUI.label("About", "h3"), UtilsUI.label("FRM - Friends Management System, version 1.2.0", "muted"), UtilsUI.label("Salim Habib University Project", "muted")));
        return RootLayout.scrollPage(UtilsUI.styled(page, "page"));
    }

    private Node qa(String question, String answer) {
        return UtilsUI.col(2, UtilsUI.label(question, "semi"), UtilsUI.wrapped(answer, "muted"));
    }

    private Node shortcut(String keys, String action) {
        return UtilsUI.row(10, UtilsUI.pill(keys, "pill-strong"), UtilsUI.label(action));
    }
}