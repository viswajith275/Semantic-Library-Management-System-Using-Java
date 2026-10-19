package com.slms.controllers;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.slms.models.Book;
import com.slms.services.AuthSession;

/** Central place for switching JavaFX scenes. */
public final class NavigationController {
    private static Scene scene;
    private static String currentRole = "User";
    private static Book selectedBook;
    private static String selectedPage = "Books";

    private NavigationController() { }

    /** Application entry point: always start on the shared login scene. */
    public static void initialize(Stage stage) {
        AuthSession.restore();
        if (AuthSession.isAdmin()) currentRole="Librarian"; else if (AuthSession.isAuthenticated()) currentRole="User";
        scene = new Scene(load(AuthSession.isAuthenticated()?"MainLayout":"Login"), 1440, 900);
        stage.setTitle("SLMS | Semantic Library");
        stage.setMinWidth(1050);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    public static void showLogin() {
        AuthSession.clear(); currentRole = "User"; selectedBook = null;
        show("Login");
    }

    /**
     * Temporary landing route until authentication and role-specific home content are connected.
     * The reusable shell provides an empty central content area for now.
     */
    public static void openHome(String role) {
        if (!AuthSession.isAuthenticated()) { showLogin(); return; }
        currentRole = role;
        selectedBook = null;
        selectedPage = "Books";
        show("MainLayout");
    }

    public static String getCurrentRole() {
        return currentRole;
    }

    public static void showBookDetails(Book book, boolean librarian) {
        selectedBook = book;
        show("MainLayout");
    }

    static Book getSelectedBook() { return selectedBook; }
    static String getSelectedPage() { return selectedPage; }
    public static void showMembers() { selectedBook = null; selectedPage = "Members"; show("MainLayout"); }
    public static void showBooks() { selectedBook = null; selectedPage = "Books"; show("MainLayout"); }
    public static void clearSelectedBook() { selectedBook = null; }

    public static void showCatalogue() {
        openHome(currentRole);
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
