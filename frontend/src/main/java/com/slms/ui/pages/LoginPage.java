package com.slms.ui.pages;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class LoginPage {

    private Parent root;

    public LoginPage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxmls/Login.fxml")
            );
            root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Parent getRoot() {
        return root;
    }
}