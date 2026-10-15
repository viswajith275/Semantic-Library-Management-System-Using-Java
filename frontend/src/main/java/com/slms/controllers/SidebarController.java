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
        // Allow the HBox shell to interpolate between the two intended sidebar widths.
        sidebar.setMinWidth(COLLAPSED_WIDTH);
        sidebar.setMaxWidth(EXPANDED_WIDTH);
        boolean librarian = "Librarian".equals(NavigationController.getCurrentRole());
        workspaceLabel.setText(librarian ? "LIBRARIAN WORKSPACE" : "MY LIBRARY");
        membersButton.setVisible(librarian);
        membersButton.setManaged(librarian);
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
        NavigationController.showLogin();
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
}
