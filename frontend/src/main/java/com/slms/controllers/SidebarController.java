package com.slms.controllers;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Controls the shared librarian navigation's expanded and compact states. */
public class SidebarController {

    private static final double EXPANDED_WIDTH = 264;
    private static final double COLLAPSED_WIDTH = 76;

    @FXML private VBox sidebar;
    @FXML private VBox brandDetails;
    @FXML private Label workspaceLabel;
    @FXML private Region workspaceTopSpacer;
    @FXML private Region workspaceBottomSpacer;
    @FXML private VBox syncCard;
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
    }

    @FXML
    private void toggleCollapsed() {
        collapsed = !collapsed;

        setVisible(brandDetails, !collapsed);
        setVisible(workspaceLabel, !collapsed);
        setVisible(workspaceTopSpacer, !collapsed);
        setVisible(workspaceBottomSpacer, !collapsed);
        setVisible(syncCard, !collapsed);

        booksButton.setText(collapsed ? "" : "Books");
        membersButton.setText(collapsed ? "" : "Members");
        borrowRecordsButton.setText(collapsed ? "" : "Borrow Records");
        logoutButton.setText(collapsed ? "" : "Log out");
        collapseButton.setText(collapsed ? "" : "Collapse");

        if (collapsed) {
            sidebar.getStyleClass().add("sidebar-collapsed");
        } else {
            sidebar.getStyleClass().remove("sidebar-collapsed");
        }

        animateSidebarWidth(collapsed ? COLLAPSED_WIDTH : EXPANDED_WIDTH);
    }

    @FXML
    private void logOut() {
        NavigationController.showLogin();
    }

    private void animateSidebarWidth(double targetWidth) {
        Timeline transition = new Timeline(
            new KeyFrame(
                Duration.millis(220),
                new KeyValue(sidebar.prefWidthProperty(), targetWidth, Interpolator.EASE_BOTH)
            )
        );
        transition.play();
    }

    private void setVisible(Region node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
