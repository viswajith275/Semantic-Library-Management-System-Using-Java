package com.slms.controllers;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

/** Retained for compatibility with the legacy UserBooks FXML route. */
public class UserBooksController {
    @FXML private AnchorPane root;

    @FXML private void initialize() {
        try {
            Node page = FXMLLoader.load(getClass().getResource("/views/BookCatalog.fxml"));
            AnchorPane.setTopAnchor(page, 0.0); AnchorPane.setRightAnchor(page, 0.0);
            AnchorPane.setBottomAnchor(page, 0.0); AnchorPane.setLeftAnchor(page, 0.0);
            root.getChildren().setAll(page);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load the catalogue", exception);
        }
    }
}
