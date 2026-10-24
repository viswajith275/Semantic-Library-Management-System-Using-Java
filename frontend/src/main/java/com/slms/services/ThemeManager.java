package com.slms.services;

import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
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
            var url = ThemeManager.class.getResource("/styles/dark.css");
            if (root == null || url == null) return;
            applyToTree(root, url.toExternalForm(), "dark".equals(theme));
        });
    }

    private static void applyToTree(Parent parent, String darkStylesheet, boolean dark) {
        parent.getStylesheets().removeIf(sheet -> sheet.contains("dark.css"));
        if (dark) parent.getStylesheets().add(darkStylesheet);
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Parent nested) applyToTree(nested, darkStylesheet, dark);
        }
    }
}
