package com.slms.controllers;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;

/** Renders the member discovery catalogue mock. */
public class UserBooksController {
    @FXML private AnchorPane root;

    @FXML
    private void initialize() {
        var view = BookCatalogView.user();
        AnchorPane.setTopAnchor(view, 0.0);
        AnchorPane.setRightAnchor(view, 0.0);
        AnchorPane.setBottomAnchor(view, 0.0);
        AnchorPane.setLeftAnchor(view, 0.0);
        root.getChildren().setAll(view);
    }
}
