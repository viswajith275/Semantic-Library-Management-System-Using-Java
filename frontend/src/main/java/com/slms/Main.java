package com.slms;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;


public class Main extends Application {

    @Override
    public void start(Stage stage) {
        Label label = new Label("Hello, JavaFX");
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root,800,600);

        stage.setTitle("SLMS");
        stage.setScene(scene);
        stage.setMaxWidth(1200);
        stage.setMaxHeight(800);
        stage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}