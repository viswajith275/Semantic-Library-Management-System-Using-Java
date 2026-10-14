package com.slms.controllers;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

public class LoginController {
    @FXML private VBox clientForm;
    @FXML private VBox librarianForm;
    private boolean librarianMode;

    @FXML
    private void signIn() {
        // Mock flow: either sign-in form opens the shared dashboard.
        NavigationController.openHome(librarianMode ? "Librarian" : "User");
    }

    @FXML
    private void showLibrarianLogin() {
        librarianMode = true;
        clientForm.setVisible(false);
        clientForm.setManaged(false);
        librarianForm.setVisible(true);
        librarianForm.setManaged(true);
    }

    @FXML
    private void showClientLogin() {
        librarianMode = false;
        librarianForm.setVisible(false);
        librarianForm.setManaged(false);
        clientForm.setVisible(true);
        clientForm.setManaged(true);
    }
}
