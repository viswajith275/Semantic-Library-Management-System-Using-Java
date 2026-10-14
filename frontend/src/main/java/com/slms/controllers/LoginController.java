package com.slms.controllers;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

public class LoginController {
    @FXML private VBox clientForm;
    @FXML private VBox librarianForm;

    @FXML
    private void signIn() {
        // Mock flow: either sign-in form opens the shared dashboard.
        NavigationController.openHome();
    }

    @FXML
    private void showLibrarianLogin() {
        clientForm.setVisible(false);
        clientForm.setManaged(false);
        librarianForm.setVisible(true);
        librarianForm.setManaged(true);
    }

    @FXML
    private void showClientLogin() {
        librarianForm.setVisible(false);
        librarianForm.setManaged(false);
        clientForm.setVisible(true);
        clientForm.setManaged(true);
    }
}
