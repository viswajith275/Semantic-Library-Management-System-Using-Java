package com.slms.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Shows the active authenticated account. */
public class TopBarController {
    @FXML private Label avatar;
    @FXML private Label profileName;
    @FXML private Label profileRole;

    @FXML
    private void initialize() {
        boolean librarian = "Librarian".equals(NavigationController.getCurrentRole());
        String name = com.slms.services.AuthSession.name();
        avatar.setText(name == null || name.isBlank() ? (librarian ? "A" : "M") : name.substring(0,1).toUpperCase());
        profileName.setText(name == null || name.isBlank() ? (librarian ? "Library Admin" : "Library Client") : name);
        profileRole.setText(librarian ? "Admin" : "Client");
    }
}
