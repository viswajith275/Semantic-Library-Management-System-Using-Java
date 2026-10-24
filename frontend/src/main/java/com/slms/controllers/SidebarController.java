package com.slms.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Controls the shared librarian navigation's expanded and compact states. */
public class SidebarController {

    private static final double EXPANDED_WIDTH = 264;
    private static final double COLLAPSED_WIDTH = 76;

    @FXML private VBox sidebar;
    @FXML private VBox brandDetails;
    @FXML private Label workspaceLabel;
    @FXML private Region workspaceTopSpacer;
    @FXML private Region workspaceBottomSpacer;
    @FXML private Button booksButton;
    @FXML private Button membersButton;
    @FXML private Button borrowRecordsButton;
    @FXML private Button logoutButton;
    @FXML private Button collapseButton;

    private boolean collapsed;

    @FXML
    private void initialize() {
        // Keep the expanded sidebar from being squeezed when catalogue cards load.
        sidebar.setMinWidth(EXPANDED_WIDTH);
        sidebar.setPrefWidth(EXPANDED_WIDTH);
        sidebar.setMaxWidth(EXPANDED_WIDTH);
        HBox.setHgrow(sidebar, Priority.NEVER);
        boolean librarian = "Librarian".equals(NavigationController.getCurrentRole());
        workspaceLabel.setText(librarian ? "LIBRARIAN WORKSPACE" : "MY LIBRARY");
        membersButton.setVisible(librarian);
        membersButton.setManaged(librarian);
        booksButton.setOnAction(e -> NavigationController.showCatalogue());
        membersButton.setOnAction(e -> NavigationController.showMembers());
        borrowRecordsButton.setOnAction(e -> {
            if ("Librarian".equals(NavigationController.getCurrentRole())) {
                NavigationController.clearSelectedBook();
                try { java.lang.reflect.Method m = NavigationController.class.getMethod("showBorrowRecords"); m.invoke(null); } catch (Exception ex) { try { java.lang.reflect.Method m2 = NavigationController.class.getMethod("showCatalogue"); m2.invoke(null); } catch (Exception ignored) {} }
            } else {
                NavigationController.clearSelectedBook();
                try { java.lang.reflect.Method m = NavigationController.class.getMethod("showMyBorrowings"); m.invoke(null); } catch (Exception ex) { try { java.lang.reflect.Method m2 = NavigationController.class.getMethod("showCatalogue"); m2.invoke(null); } catch (Exception ignored) {} }
            }
        });
        boolean onMembers = "Members".equals(NavigationController.getSelectedPage());
        boolean onBorrowRecords = "BorrowRecords".equals(NavigationController.getSelectedPage());
        boolean onMyBorrows = "MyBorrows".equals(NavigationController.getSelectedPage());
        setActive(booksButton, !onMembers && !onBorrowRecords && !onMyBorrows);
        setActive(membersButton, onMembers && librarian);
        setActive(borrowRecordsButton, (onBorrowRecords && librarian) || (onMyBorrows && !librarian));
        borrowRecordsButton.setText(librarian ? "Borrow Records" : "My Borrowings");
    }

    @FXML
    private void toggleCollapsed() {
        collapsed = !collapsed;

        setVisible(brandDetails, !collapsed);
        setVisible(workspaceLabel, !collapsed);
        setVisible(workspaceTopSpacer, !collapsed);
        setVisible(workspaceBottomSpacer, !collapsed);
        booksButton.setText(collapsed ? "" : "Books");
        membersButton.setText(collapsed ? "" : "Members");
        borrowRecordsButton.setText(collapsed ? "" : ("Librarian".equals(NavigationController.getCurrentRole()) ? "Borrow Records" : "My Borrowings"));
        logoutButton.setText(collapsed ? "" : "Log out");
        collapseButton.setText(collapsed ? "" : "Collapse");

        if (collapsed) {
            sidebar.getStyleClass().add("sidebar-collapsed");
        } else {
            sidebar.getStyleClass().remove("sidebar-collapsed");
        }

        setSidebarWidth(collapsed ? COLLAPSED_WIDTH : EXPANDED_WIDTH);
    }

    @FXML
    private void logOut() {
        if (com.slms.services.AuthSession.isAdmin()) {
            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
                @Override protected Void call() throws Exception {
                    com.slms.services.ApiClient.request("POST", "/api/auth/admin/logout", com.slms.services.AuthSession.token(), null, null);
                    return null;
                }
            };
            task.setOnSucceeded(e -> NavigationController.showLogin());
            task.setOnFailed(e -> javafx.application.Platform.runLater(NavigationController::showLogin));
            Thread thread = new Thread(task, "slms-logout"); thread.setDaemon(true); thread.start();
        } else NavigationController.showLogin();
    }

    private void setSidebarWidth(double width) {
        // The sidebar is the first child of the MainLayout HBox. Set all three width
        // constraints together so the HBox immediately gives the remaining space to
        // the page content instead of retaining the sidebar's FXML prefWidth.
        sidebar.setMinWidth(width);
        sidebar.setPrefWidth(width);
        sidebar.setMaxWidth(width);
        HBox.setHgrow(sidebar, Priority.NEVER);
        if (sidebar.getParent() != null) {
            sidebar.getParent().requestLayout();
        }
        sidebar.resize(width, sidebar.getHeight());
    }

    private void setVisible(Region node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void setActive(Button button, boolean active) {
        button.getStyleClass().remove("nav-active");
        button.setStyle(active
            ? "-fx-background-color: #e8efff; -fx-background-radius: 14; -fx-border-color: #5b6df6 transparent transparent transparent; -fx-border-width: 0 0 0 3; -fx-padding: 10 12 10 9; -fx-text-fill: #1e3b88; -fx-font-weight: 700;"
            : "");
        if (active) button.getStyleClass().add("nav-active");
    }
}
