package com.slms.controllers;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Central place for switching JavaFX scenes. */
public final class NavigationController {
    private static Scene scene;
    private static String currentRole = "User";

    private NavigationController() { }

    /** Application entry point: always start on the shared login scene. */
    public static void initialize(Stage stage) {
        scene = new Scene(load("Login"), 1440, 900);
        stage.setTitle("SLMS | Semantic Library");
        stage.setMinWidth(1050);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    public static void showLogin() {
        show("Login");
    }

    /**
     * Temporary landing route until authentication and role-specific home content are connected.
     * The reusable shell provides an empty central content area for now.
     */
    public static void openHome(String role) {
        currentRole = role;
        show("MainLayout");
    }

    public static String getCurrentRole() {
        return currentRole;
    }

    private static void show(String viewName) {
        scene.setRoot(load(viewName));
    }

    private static Parent load(String viewName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                NavigationController.class.getResource("/views/" + viewName + ".fxml")
            );
            Parent root = loader.load();
            root.getStylesheets().add(
                NavigationController.class.getResource("/styles/login.css").toExternalForm()
            );
            root.getStylesheets().add(
                NavigationController.class.getResource("/styles/layout.css").toExternalForm()
            );
            return root;
        } catch (IOException | NullPointerException exception) {
            throw new IllegalStateException("Could not load FXML view: " + viewName, exception);
        }
    }
}
