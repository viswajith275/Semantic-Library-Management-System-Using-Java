package com.slms.controllers;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

/**
 * Keeps the reusable top bar and sidebar mounted while individual page content changes.
 */
public class MainLayoutController {

    @FXML
    private StackPane contentHost;

    /** Replaces the central area with a Books, Members, or Borrow Records page node. */
    public void setContent(Node pageContent) {
        contentHost.getChildren().setAll(pageContent);
    }
}
