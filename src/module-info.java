module FriendsRelationshipManagementSystem {
	requires javafx.controls;
	requires java.prefs;
	requires javafx.graphics;
	requires java.desktop;
	requires java.sql;
	requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;

	opens frmSystem to javafx.graphics, javafx.fxml;
}
