package frmSystem.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javafx.scene.text.Font;

public class Theme {
    private static final List<String> FONTS = List.of(
		"Inter-Regular", "Inter-Italic", "Inter-Medium", "Inter-SemiBold", "Inter-Bold",
		"PlusJakartaSans-SemiBold", "PlusJakartaSans-Bold"
	);
	
	public static void loadFonts() {
        for (String name : FONTS) {
            try (InputStream in = Theme.class.getResourceAsStream("/resources/fonts/" + name + ".ttf")) {
                if (in != null) {
                    Font.loadFont(in, 13);
                }
            } catch (IOException e) {
            	e.printStackTrace();
            }
        }
    }
	
}
