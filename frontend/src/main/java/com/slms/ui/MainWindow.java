package com.slms.ui;

import com.slms.ui.pages.LoginPage;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainWindow {

    private final Stage stage;

    public MainWindow(Stage stage) {
        this.stage = stage;

        initialize();
    }

    private void initialize() {

    

        Scene scene = new Scene(new LoginPage().getRoot(), 1400, 900);

        stage.setTitle("SLMS");
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }
}