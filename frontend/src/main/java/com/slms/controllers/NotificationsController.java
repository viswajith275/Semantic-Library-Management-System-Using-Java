package com.slms.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.slms.services.ApiClient;
import com.slms.services.AuthSession;
import java.io.IOException;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Member wishlist backed by the existing book pin API. */
public class NotificationsController {

  @FXML
  private VBox notificationsList;

  @FXML
  private VBox emptyState;

  @FXML
  private Label unreadCount;

  @FXML
  private Label statusMessage;

  @FXML
  private void initialize() {
    loadNotifications();
  }

  @FXML
  private void refresh() {
    loadNotifications();
  }

  private void loadNotifications() {
    notificationsList.getChildren().clear();
    statusMessage.setVisible(false);
    statusMessage.setManaged(false);
    Task<JsonNode> task = new Task<>() {
      @Override
      protected JsonNode call() throws Exception {
        return ApiClient.request(
          "GET",
          "/api/member/pins",
          null,
          AuthSession.code(),
          null
        );
      }
    };
    task.setOnSucceeded(e -> {
      JsonNode pins = task.getValue();
      int count = pins != null && pins.isArray() ? pins.size() : 0;
      if (pins != null && pins.isArray()) pins.forEach(this::addPinnedBook);
      unreadCount.setText(
        count + (count == 1 ? " saved book" : " saved books")
      );
      boolean empty = count == 0;
      emptyState.setVisible(empty);
      emptyState.setManaged(empty);
    });
    task.setOnFailed(e -> {
      emptyState.setVisible(false);
      emptyState.setManaged(false);
      statusMessage.setText(
        "Could not load your wishlist: " +
          ApiClient.message(task.getException())
      );
      statusMessage.setVisible(true);
      statusMessage.setManaged(true);
    });
    Thread thread = new Thread(task, "slms-member-wishlist");
    thread.setDaemon(true);
    thread.start();
  }

  private void addPinnedBook(JsonNode book) {
    String title = book.path("title").asText("Book");
    String availability = book.path("availabilityStatus").asText("");
    long copies = book.path("availableCopies").asLong(0);
    boolean available =
      copies > 0 || availability.equalsIgnoreCase("AVAILABLE");
    String message = available
      ? title +
        " is available now. Visit the circulation desk to borrow a copy."
      : title +
        " is in your wishlist. Refresh to check its latest availability.";
    try {
      FXMLLoader loader = new FXMLLoader(
        getClass().getResource("/views/components/NotificationCard.fxml")
      );
      notificationsList.getChildren().add(loader.load());
      loader
        .<NotificationCardController>getController()
        .configure(
          available ? "Available from your wishlist" : "Saved to your wishlist",
          message,
          available ? "Available now" : "Wishlist",
          available ? "available" : "info",
          available
        );
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load wishlist item", e);
    }
  }

  @FXML
  private void goBack() {
    NavigationController.showCatalogue();
  }
}
