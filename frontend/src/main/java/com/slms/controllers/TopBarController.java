package com.slms.controllers;

import com.slms.services.ThemeManager;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.shape.SVGPath;

/** Shows the active authenticated account and theme toggle. */
public class TopBarController implements Initializable {

  @FXML
  private Label avatar;

  @FXML
  private Label profileName;

  @FXML
  private Label profileRole;

  @FXML
  private SVGPath themeIcon;

  @FXML
  private javafx.scene.control.Button notificationButton;

  private static final String MOON =
    "M12 3a6.9 6.9 0 0 0 9 9c-1.04 5.46-6.22 9.1-11.65 7.7A9 9 0 1 1 12 3";
  private static final String SUN =
    "M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z";

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    boolean librarian =
      com.slms.services.AuthSession.isAdmin() ||
      "Librarian".equals(NavigationController.getCurrentRole());
    if (notificationButton != null) {
      notificationButton.setVisible(!librarian);
      notificationButton.setManaged(!librarian);
    }
    String name = com.slms.services.AuthSession.name();
    String fallbackName = librarian ? "Library Admin" : "Library Member";
    String displayName =
      name == null || name.isBlank() ? fallbackName : name.trim();
    if (avatar != null) avatar.setText(
      displayName.substring(0, 1).toUpperCase(java.util.Locale.ROOT)
    );
    if (profileName != null) profileName.setText(displayName);
    if (profileRole != null) profileRole.setText(
      librarian ? "Librarian" : "Member"
    );
    updateIcon();
  }

  @FXML
  private void openNotifications() {
    NavigationController.showNotifications();
  }

  @FXML
  private void toggleTheme() {
    ThemeManager.toggle();
    if (themeIcon != null) updateIcon();
  }

  private void updateIcon() {
    if (themeIcon != null) themeIcon.setContent(
      ThemeManager.isDark() ? SUN : MOON
    );
  }
}
