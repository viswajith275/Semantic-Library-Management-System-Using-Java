package com.slms.controllers;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;

/** Renders the librarian catalogue mock. */
public class LibrarianBooksController {
    @FXML private AnchorPane root;

    @FXML
    private void initialize() {
        var view = BookCatalogView.librarian();
        AnchorPane.setTopAnchor(view, 0.0);
        AnchorPane.setRightAnchor(view, 0.0);
        AnchorPane.setBottomAnchor(view, 0.0);
        AnchorPane.setLeftAnchor(view, 0.0);
        root.getChildren().setAll(view);
    }
}
