package com.slms.controllers;

import javafx.fxml.FXML;

/** Handles navigation from the registration form until account creation is connected. */
public class RegisterController {

    @FXML
    private void createAccount() {
        // TODO: validate the fields and call the registration/authentication service.
        NavigationController.showLogin();
    }

    @FXML
    private void showLogin() {
        NavigationController.showLogin();
    }
}
