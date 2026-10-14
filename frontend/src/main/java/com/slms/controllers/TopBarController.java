package com.slms.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Shows the role selected in the mock sign-in flow. */
public class TopBarController {
    @FXML private Label avatar;
    @FXML private Label profileName;
    @FXML private Label profileRole;

    @FXML
    private void initialize() {
        boolean librarian = "Librarian".equals(NavigationController.getCurrentRole());
        avatar.setText(librarian ? "L" : "U");
        profileName.setText(librarian ? "Library Admin" : "Library User");
        profileRole.setText(librarian ? "Librarian" : "User");
    }
}
