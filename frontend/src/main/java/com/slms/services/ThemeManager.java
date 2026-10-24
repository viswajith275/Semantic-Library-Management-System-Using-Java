package com.slms.services;

import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.scene.Scene;

/** Persists and applies dark/light theme to the active JavaFX scene. */
public final class ThemeManager {
    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);
    private static final String KEY = "theme";
    private static Scene scene;

    private ThemeManager() {}

    public static void initialize(Scene appScene) {
        scene = appScene;
        apply(load());
    }

    public static boolean isDark() {
        return "dark".equals(load());
    }

    public static void toggle() {
        apply(isDark() ? "light" : "dark");
    }

    private static String load() {
        return PREFS.get(KEY, "light");
    }

    private static void apply(String theme) {
        if (scene == null) return;
        PREFS.put(KEY, theme);
        Platform.runLater(() -> {
            var root = scene.getRoot();
            if (root == null) return;
            root.getStylesheets().removeIf(sheet -> sheet.contains("dark.css"));
            if ("dark".equals(theme)) {
                var url = ThemeManager.class.getResource("/styles/dark.css");
                if (url != null) {
                    root.getStylesheets().add(url.toExternalForm());
                }
            }
        });
    }
}
