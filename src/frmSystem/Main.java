package frmSystem;
	
import frmSystem.ui.Theme;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.Scene;
import frmSystem.db.Database;
import frmSystem.pages.RootLayout;


public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
	        Theme.loadFonts();
	        Database.init();
	        RootLayout layout = new RootLayout(primaryStage);
	        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
			Scene scene = new Scene(layout.root(),400,400);
	        layout.installShortcuts(scene);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setTitle("FRM - Friends Management System");
	        primaryStage.setMinWidth(Math.min(960, screen.getWidth()));
	        primaryStage.setMinHeight(Math.min(560, screen.getHeight()));
	        primaryStage.setMaximized(true);
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
