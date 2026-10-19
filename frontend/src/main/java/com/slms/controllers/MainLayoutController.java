package com.slms.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import java.io.IOException;

/**
 * Keeps the reusable top bar and sidebar mounted while individual page content changes.
 */
public class MainLayoutController {

    @FXML
    private StackPane contentHost;
    @FXML
    private void initialize() {
        boolean librarian = "Librarian".equals(NavigationController.getCurrentRole());
        try {
            Node booksPage;
            if (NavigationController.getSelectedBook() != null) {
                booksPage = new FXMLLoader(
                    MainLayoutController.class.getResource("/views/BookDetailed.fxml")
                ).load();
            } else if ("Members".equals(NavigationController.getSelectedPage())) {
                booksPage = new FXMLLoader(MainLayoutController.class.getResource("/views/Members.fxml")).load();
            } else {
                booksPage = new FXMLLoader(
                    MainLayoutController.class.getResource("/views/BookCatalog.fxml")
                ).load();
            }
            contentHost.getChildren().setAll(booksPage);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load the Books page", exception);
        }
    }

    /** Replaces the central area with a Books, Members, or Borrow Records page node. */
    public void setContent(Node pageContent) {
        contentHost.getChildren().setAll(pageContent);
    }
}
