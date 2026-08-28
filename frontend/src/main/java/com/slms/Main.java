package com.slms;

import com.slms.controllers.NavigationController;

import javafx.application.Application;
import javafx.stage.Stage;


public class Main extends Application {

    @Override
    public void start(Stage stage) {
        NavigationController.initialize(stage);
    }
    public static void main(String[] args) {
        launch(args);
    }
}
