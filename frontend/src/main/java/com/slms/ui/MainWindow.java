package com.slms.ui;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainWindow {

    private final Stage stage;

    public MainWindow(Stage stage) {
        this.stage = stage;

        initialize();
    }

    private void initialize() {

        StackPane root = new StackPane();

        Label label = new Label("Semantic Library Management System");

        root.getChildren().add(label);

        Scene scene = new Scene(root, 1400, 900);

        stage.setTitle("SLMS");
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }
}