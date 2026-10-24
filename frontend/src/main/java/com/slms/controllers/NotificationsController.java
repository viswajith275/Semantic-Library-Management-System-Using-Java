package com.slms.controllers;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class NotificationsController {
    @FXML private VBox notificationsList;
    @FXML private VBox emptyState;
    @FXML private Label unreadCount;

    @FXML
    private void initialize() {
        loadNotifications();
    }

    private void loadNotifications() {
        notificationsList.getChildren().clear();
        
        // Sample notifications
        addNotification("Book Available", "Clean Code is now available for pickup at the circulation desk.", "2 hours ago", "available", true);
        addNotification("Due Reminder", "Atomic Habits is due in 2 days. Please return or renew it soon.", "5 hours ago", "due", true);
        addNotification("Welcome!", "Welcome to the Semantic Library Management System. Start exploring our collection.", "1 day ago", "info", false);
        addNotification("Overdue Notice", "Design Patterns was due yesterday. Please return it as soon as possible.", "2 days ago", "overdue", false);
        
        long unread = 2;
        unreadCount.setText(unread + " unread");
        
        if (notificationsList.getChildren().isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
        } else {
            emptyState.setVisible(false);
            emptyState.setManaged(false);
        }
    }

    private void addNotification(String title, String message, String time, String type, boolean unread) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/components/NotificationCard.fxml"));
            notificationsList.getChildren().add(loader.load());
            NotificationCardController controller = loader.getController();
            controller.configure(title, message, time, type, unread);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load notification card", e);
        }
    }

    @FXML
    private void goBack() {
        NavigationController.showCatalogue();
    }

    @FXML
    private void markAllRead() {
        loadNotifications();
        unreadCount.setText("0 unread");
    }
}
