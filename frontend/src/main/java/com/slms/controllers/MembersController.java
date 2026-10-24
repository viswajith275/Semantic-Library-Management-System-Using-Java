package com.slms.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.slms.models.Member;
import com.slms.services.ApiClient;
import com.slms.services.AuthSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.event.ActionEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MembersController {

  @FXML private StackPane overlay;
  @FXML private VBox dialog;
  @FXML private TextField name, email, searchField, codeField;
  @FXML private Label error, status, memberCount, emptyState, loadError;
  @FXML private Label dialogTitle, dialogSubtitle;
  @FXML private javafx.scene.control.Button submitButton, copyButton, clearSearch;
  @FXML private MenuButton statusFilter;
  @FXML private VBox codeBox;
  @FXML private VBox memberList;
  @FXML private javafx.scene.control.ScrollPane memberScroll;

  private final List<Member> members = new ArrayList<>();
  private Long editingId;
  private String selectedStatus = "All statuses";

  @FXML
  private void initialize() {
    status.managedProperty().bind(status.textProperty().isNotEmpty());
    status.visibleProperty().bind(status.textProperty().isNotEmpty());
    searchField.textProperty().addListener((observable, oldValue, newValue) -> {
      clearSearch.setVisible(!newValue.isBlank());
      clearSearch.setManaged(!newValue.isBlank());
      render();
    });
    loadMembers();
  }

  @FXML
  private void clearSearch() {
    searchField.clear();
    searchField.requestFocus();
  }

  @FXML
  private void refresh() {
    loadMembers();
  }

  private void loadMembers() {
    memberCount.setText("Loading members…");
    loadError.setVisible(false);
    loadError.setManaged(false);

    Task<JsonNode> task = new Task<>() {
      @Override
      protected JsonNode call() throws Exception {
        return ApiClient.request("GET", "/api/admin/members", AuthSession.token(), null, null);
      }
    };
    task.setOnSucceeded(e -> {
      members.clear();
      JsonNode data = task.getValue();
      if (data.isArray()) data.forEach(node -> members.add(Member.from(node)));
      render();
    });
    task.setOnFailed(e -> {
      memberList.getChildren().clear();
      memberList.setVisible(false);
      memberList.setManaged(false);
      emptyState.setVisible(false);
      emptyState.setManaged(false);
      memberCount.setText("Could not load members");
      loadError.setText("Could not load members: " + ApiClient.message(task.getException()));
      loadError.setVisible(true);
      loadError.setManaged(true);
    });
    Thread thread = new Thread(task, "slms-list-members");
    thread.setDaemon(true);
    thread.start();
  }

  private void render() {
    memberList.getChildren().clear();
    String query = searchField.getText();
    List<Member> visible = members.stream()
      .filter(member -> member.matches(query))
      .filter(member -> switch (selectedStatus) {
        case "Active" -> member.isActive();
        case "Blocked" -> !member.isActive();
        default -> true;
      })
      .toList();

    for (Member member : visible) {
      try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/components/MemberRow.fxml"));
        memberList.getChildren().add(loader.load());
        loader.<MemberRowController>getController()
          .configure(member, this::edit, this::toggleActive, this::regenerateCode);
      } catch (IOException exception) {
        throw new IllegalStateException("Could not load a member row", exception);
      }
    }

    boolean hasMembers = !visible.isEmpty();
    memberList.setVisible(hasMembers);
    memberList.setManaged(hasMembers);
    emptyState.setVisible(!hasMembers);
    emptyState.setManaged(!hasMembers);
    emptyState.setText(members.isEmpty()
      ? "No client accounts yet. Use “Create client” to add the first member."
      : "No members match your search.");
    loadError.setVisible(false);
    loadError.setManaged(false);
    memberCount.setText(visible.size() == members.size()
      ? members.size() + (members.size() == 1 ? " member account" : " member accounts")
      : "Showing " + visible.size() + " of " + members.size() + " member accounts");
  }

  @FXML
  private void selectStatus(ActionEvent event) {
    selectedStatus = ((MenuItem) event.getSource()).getText();
    statusFilter.setText(selectedStatus);
    render();
  }

  @FXML
  private void openCreate() {
    editingId = null;
    dialogTitle.setText("Create member account");
    dialogSubtitle.setText("Share the generated access code with the member.");
    submitButton.setText("Create account");
    name.clear();
    email.clear();
    error.setVisible(false);
    error.setManaged(false);
    codeBox.setVisible(false);
    codeBox.setManaged(false);
    attachToWindowRoot();
    showDialog();
  }

  private void edit(Member member) {
    editingId = member.getId();
    dialogTitle.setText("Edit member account");
    dialogSubtitle.setText("Update the member details. Their access code stays the same.");
    submitButton.setText("Save changes");
    name.setText(member.getName());
    email.setText(member.getEmail());
    error.setVisible(false);
    error.setManaged(false);
    codeBox.setVisible(false);
    codeBox.setManaged(false);
    attachToWindowRoot();
    showDialog();
  }

  private void showDialog() {
    overlay.setVisible(true);
    overlay.setManaged(true);
    dialog.setMaxHeight(Region.USE_PREF_SIZE);
  }

  private void attachToWindowRoot() {
    if (overlay.getScene() == null) return;
    Parent windowRoot = overlay.getScene().getRoot();
    if (windowRoot == overlay.getParent()) return;
    if (overlay.getParent() instanceof Pane old) old.getChildren().remove(overlay);
    if (windowRoot instanceof StackPane root) {
      root.getChildren().add(overlay);
      StackPane.setAlignment(overlay, Pos.CENTER);
    }
  }

  @FXML
  private void closeCreate() {
    overlay.setVisible(false);
    overlay.setManaged(false);
    editingId = null;
  }

  @FXML
  private void overlayClicked(MouseEvent event) {
    if (event.getTarget() == overlay) closeCreate();
  }

  @FXML
  private void dialogClicked(MouseEvent event) {
    event.consume();
  }

  @FXML
  private void create() {
    String memberName = name.getText().trim();
    String memberEmail = email.getText().trim();
    if (memberName.isBlank() || memberEmail.isBlank()) {
      showError("Enter the member name and email.");
      return;
    }

    boolean editing = editingId != null;
    var body = ApiClient.object().put("name", memberName).put("email", memberEmail);
    String method = editing ? "PUT" : "POST";
    String path = editing ? "/api/admin/members/" + editingId : "/api/admin/members";

    Task<JsonNode> task = new Task<>() {
      @Override
      protected JsonNode call() throws Exception {
        return ApiClient.request(method, path, AuthSession.token(), null, body);
      }
    };
    task.setOnSucceeded(e -> {
      JsonNode response = task.getValue();
      if (editing) {
        status.setText("Changes saved for " + memberName + ".");
        closeCreate();
      } else {
        String accessCode = response.path("customCode").asText();
        codeField.setText(accessCode);
        codeBox.setVisible(true);
        codeBox.setManaged(true);
        status.setText("New member account created for " + memberName + ".");
        searchField.clear();
        error.setVisible(false);
        error.setManaged(false);
        javafx.application.Platform.runLater(() -> {
          codeField.requestFocus();
          codeField.selectAll();
        });
      }
      loadMembers();
    });
    task.setOnFailed(e -> showError(ApiClient.message(task.getException())));
    Thread thread = new Thread(task, "slms-save-member");
    thread.setDaemon(true);
    thread.start();
  }

  @FXML
  private void copyCode() {
    String accessCode = codeField.getText();
    if (accessCode == null || accessCode.isBlank()) return;

    javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
    content.putString(accessCode);
    javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
    status.setText("Access code copied to clipboard.");
    copyButton.setText("Copied ✓");

    javafx.animation.PauseTransition reset = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.8));
    reset.setOnFinished(event -> copyButton.setText("Copy"));
    reset.play();
  }

  private void toggleActive(Member member) {
    boolean blocking = member.isActive();
    Alert confirm = new Alert(
      Alert.AlertType.CONFIRMATION,
      blocking
        ? "Block “" + member.getName() + "”? They will not be able to sign in or borrow until unblocked."
        : "Unblock “" + member.getName() + "”? They will regain access immediately.",
      ButtonType.CANCEL,
      ButtonType.OK
    );
    confirm.setHeaderText(blocking ? "Block member" : "Unblock member");
    confirm.showAndWait()
      .filter(result -> result == ButtonType.OK)
      .ifPresent(result -> updateMember(member, body -> body.put("active", !member.isActive()),
        member.getName() + (blocking ? " has been blocked." : " has been unblocked.")));
  }

  private void regenerateCode(Member member) {
    Alert confirm = new Alert(
      Alert.AlertType.CONFIRMATION,
      "Regenerate the access code for “" + member.getName() + "”? Their current code will stop working immediately.",
      ButtonType.CANCEL,
      ButtonType.OK
    );
    confirm.setHeaderText("Regenerate access code");
    confirm.showAndWait()
      .filter(result -> result == ButtonType.OK)
      .ifPresent(result -> updateMember(member, body -> body.put("regenerateCode", true), null));
  }

  private interface BodyEditor {
    void edit(com.fasterxml.jackson.databind.node.ObjectNode body);
  }

  /** Sends a partial update for a member, then refreshes the directory. */
  private void updateMember(Member member, BodyEditor editor, String successMessage) {
    var body = ApiClient.object();
    editor.edit(body);

    Task<JsonNode> task = new Task<>() {
      @Override
      protected JsonNode call() throws Exception {
        return ApiClient.request(
          "PUT",
          "/api/admin/members/" + member.getId(),
          AuthSession.token(),
          null,
          body
        );
      }
    };
    task.setOnSucceeded(e -> {
      JsonNode response = task.getValue();
      if (successMessage != null) {
        status.setText(successMessage);
      } else {
        String newCode = response.path("customCode").asText();
        status.setText("Access code regenerated for " + member.getName() + ".");
        showCodeDialog(member.getName(), newCode);
      }
      loadMembers();
    });
    task.setOnFailed(e -> showActionError(ApiClient.message(task.getException())));
    Thread thread = new Thread(task, "slms-update-member");
    thread.setDaemon(true);
    thread.start();
  }

  private void showCodeDialog(String memberName, String accessCode) {
    TextField field = new TextField(accessCode);
    field.setEditable(false);
    field.setPrefWidth(320);
    field.selectAll();
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Access code regenerated");
    alert.setHeaderText("New access code for " + memberName);
    alert.getDialogPane().setContent(field);
    alert.showAndWait();
  }

  private void showActionError(String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR, message);
    alert.setHeaderText("Could not update member");
    alert.showAndWait();
  }

  private void showError(String message) {
    error.setText(message);
    error.setVisible(true);
    error.setManaged(true);
  }
}
