package com.slms.controllers;

import javafx.fxml.FXML;

public class LoginController {
    @FXML
    private void signIn() {
        // Temporary default route. Replace with authenticated role-based routing later.
        NavigationController.openHome(false);
    }

    @FXML
    private void showRegister() {
        NavigationController.showRegister();
    }
}
