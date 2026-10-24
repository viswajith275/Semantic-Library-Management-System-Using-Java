package com.slms.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

public class NotificationCardController {

  @FXML
  private StackPane iconBox;

  @FXML
  private SVGPath icon;

  @FXML
  private Label title, message, time, badge;

  public void configure(
    String notificationTitle,
    String notificationMessage,
    String notificationTime,
    String type,
    boolean unread
  ) {
    title.setText(notificationTitle);
    message.setText(notificationMessage);
    time.setText(notificationTime);

    badge.setVisible(unread);
    badge.setManaged(unread);

    if (unread) {
      iconBox.getStyleClass().add("notification-icon-box-unread");
    }

    switch (type) {
      case "available" -> {
        icon.setContent("M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z");
        iconBox.getStyleClass().add("notification-icon-box-success");
      }
      case "due" -> {
        icon.setContent("M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z");
        iconBox.getStyleClass().add("notification-icon-box-warning");
      }
      case "overdue" -> {
        icon.setContent(
          "M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
        );
        iconBox.getStyleClass().add("notification-icon-box-danger");
      }
      default -> icon.setContent(
        "M12 8h.01M11 12h1v4h1m9-4a10 10 0 11-20 0 10 10 0 0120 0Z"
      );
    }
  }
}
