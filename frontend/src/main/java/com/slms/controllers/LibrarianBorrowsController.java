package com.slms.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.slms.models.BorrowRecord;
import com.slms.services.ApiClient;
import com.slms.services.AuthSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Librarian circulation desk: active loans, checkout, returns and renewals.
 *
 * <p>Only active loans exist in the backend contract; there is deliberately no fines or
 * payment handling on this page.</p>
 */
public class LibrarianBorrowsController {

  private static final DateTimeFormatter DUE_FORMAT =
    DateTimeFormatter.ofPattern("d MMM yyyy");

  @FXML
  private Label status, activeCount, dueSoonCount, overdueCount, resultCount, emptyTitle, emptySubtitle, loadError;

  @FXML
  private Label issueError, extendError, extendSubtitle;

  @FXML
  private TextField searchField, barcodeField, memberCodeField, daysField, extendDaysField;

  @FXML
  private javafx.scene.control.Button clearSearch;

  @FXML
  private javafx.scene.control.MenuButton filterTrigger;

  @FXML
  private VBox loanList;

  @FXML
  private VBox emptyState;

  @FXML
  private ScrollPane loanScroll;

  @FXML
  private StackPane issueOverlay, extendOverlay;

  @FXML
  private VBox issueDialog, extendDialog;

  private final List<BorrowRecord> loans = new ArrayList<>();
  private String selectedFilter = "All loans";
  private BorrowRecord extendingLoan;

  @FXML
  private void initialize() {
    status.managedProperty().bind(status.textProperty().isNotEmpty());
    status.visibleProperty().bind(status.textProperty().isNotEmpty());
    searchField
      .textProperty()
      .addListener((observable, oldValue, newValue) -> {
        clearSearch.setVisible(!newValue.isBlank());
        clearSearch.setManaged(!newValue.isBlank());
        render();
      });
    loadLoans();
  }

  @FXML
  private void refresh() {
    loadLoans();
  }

  @FXML
  private void clearSearch() {
    searchField.clear();
    searchField.requestFocus();
  }

  private void loadLoans() {
    resultCount.setText("Loading loans…");
    loadError.setVisible(false);
    loadError.setManaged(false);

    Task<JsonNode> task = new Task<>() {
      @Override
      protected JsonNode call() throws Exception {
        return ApiClient.request(
          "GET",
          "/api/admin/loans/active",
          AuthSession.token(),
          null,
          null
        );
      }
    };
    task.setOnSucceeded(e -> {
      loans.clear();
      JsonNode data = task.getValue();
      if (data.isArray()) data.forEach(node ->
        loans.add(BorrowRecord.from(node))
      );
      render();
    });
    task.setOnFailed(e -> {
      loanList.getChildren().clear();
      loanList.setVisible(false);
      loanList.setManaged(false);
      emptyState.setVisible(false);
      emptyState.setManaged(false);
      resultCount.setText("Could not load loans");
      loadError.setText(
        "Could not load active loans: " + ApiClient.message(task.getException())
      );
      loadError.setVisible(true);
      loadError.setManaged(true);
    });
    Thread thread = new Thread(task, "slms-list-loans");
    thread.setDaemon(true);
    thread.start();
  }

  private void render() {
    loanList.getChildren().clear();
    String query = searchField.getText();
    List<BorrowRecord> visible = loans
      .stream()
      .filter(this::matchesFilter)
      .filter(loan -> loan.matches(query))
      .toList();

    for (BorrowRecord loan : visible) {
      try {
        FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/views/components/LoanRow.fxml")
        );
        loanList.getChildren().add(loader.load());
        loader
          .<LoanRowController>getController()
          .configure(loan, this::openExtend, this::returnLoan);
      } catch (IOException exception) {
        throw new IllegalStateException("Could not load a loan row", exception);
      }
    }

    boolean hasLoans = !visible.isEmpty();
    loanList.setVisible(hasLoans);
    loanList.setManaged(hasLoans);
    emptyState.setVisible(!hasLoans);
    emptyState.setManaged(!hasLoans);
    if (loans.isEmpty()) {
      emptyTitle.setText("No books are currently on loan");
      emptySubtitle.setText(
        "Once a loan is issued, its book and return date will appear here."
      );
    } else {
      emptyTitle.setText("No loans match these filters");
      emptySubtitle.setText(
        "Try a different search or choose another loan status."
      );
    }
    loadError.setVisible(false);
    loadError.setManaged(false);
    resultCount.setText(
      visible.size() == 1 ? "1 active loan" : visible.size() + " active loans"
    );

    long dueSoon = loans.stream().filter(BorrowRecord::isDueSoon).count();
    long overdue = loans.stream().filter(BorrowRecord::isOverdue).count();
    activeCount.setText(String.valueOf(loans.size()));
    dueSoonCount.setText(String.valueOf(dueSoon));
    overdueCount.setText(String.valueOf(overdue));
  }

  private boolean matchesFilter(BorrowRecord loan) {
    return switch (selectedFilter) {
      case "Due soon" -> loan.isDueSoon();
      case "Overdue" -> loan.isOverdue();
      default -> true;
    };
  }

  @FXML
  private void selectFilter(ActionEvent event) {
    selectedFilter = ((MenuItem) event.getSource()).getText();
    filterTrigger.setText(selectedFilter);
    render();
  }

  /* ------------------------------------------------------------------ */
  /* Checkout                                                            */
  /* ------------------------------------------------------------------ */

  @FXML
  private void openIssue() {
    barcodeField.clear();
    memberCodeField.clear();
    daysField.setText("7");
    issueError.setVisible(false);
    issueError.setManaged(false);
    attachOverlayToWindowRoot(issueOverlay);
    issueOverlay.setVisible(true);
    issueOverlay.setManaged(true);
    issueDialog.setMaxHeight(Region.USE_PREF_SIZE);
    barcodeField.requestFocus();
  }

  @FXML
  private void closeIssue() {
    issueOverlay.setVisible(false);
    issueOverlay.setManaged(false);
  }

  @FXML
  private void issueOverlayClicked(MouseEvent event) {
    if (event.getTarget() == issueOverlay) closeIssue();
  }

  @FXML
  private void dialogClicked(MouseEvent event) {
    event.consume();
  }

  @FXML
  private void issueLoan() {
    String barcode = barcodeField.getText().trim();
    String memberCode = memberCodeField.getText().trim();
    Integer days = parseDays(daysField.getText());

    if (barcode.isBlank() || memberCode.isBlank()) {
      showIssueError("Enter both the copy barcode and the member access code.");
      return;
    }
    if (days == null) {
      showIssueError(
        "Days to borrow must be a whole number between 1 and 365."
      );
      return;
    }

    var body = ApiClient.object()
      .put("barcode", barcode)
      .put("memberCustomCode", memberCode)
      .put("daysToBorrow", days);

    Task<JsonNode> task = new Task<>() {
      @Override
      protected JsonNode call() throws Exception {
        return ApiClient.request(
          "POST",
          "/api/admin/loans/checkout",
          AuthSession.token(),
          null,
          body
        );
      }
    };
    task.setOnSucceeded(e -> {
      String due = formatDue(task.getValue().path("dueDate").asText());
      closeIssue();
      showStatus(
        due.isBlank()
          ? "Loan issued for " + barcode + "."
          : "Loan issued for " + barcode + ". Due " + due + "."
      );
      loadLoans();
    });
    task.setOnFailed(e ->
      showIssueError(ApiClient.message(task.getException()))
    );
    Thread thread = new Thread(task, "slms-issue-loan");
    thread.setDaemon(true);
    thread.start();
  }

  /* ------------------------------------------------------------------ */
  /* Extend                                                              */
  /* ------------------------------------------------------------------ */

  private void openExtend(BorrowRecord loan) {
    extendingLoan = loan;
    extendSubtitle.setText(
      "“" +
        loan.getBookTitle() +
        "” · " +
        loan.getMemberName() +
        ". Currently due " +
        loan.getDueDisplay() +
        "."
    );
    extendDaysField.setText("7");
    extendError.setVisible(false);
    extendError.setManaged(false);
    attachOverlayToWindowRoot(extendOverlay);
    extendOverlay.setVisible(true);
    extendOverlay.setManaged(true);
    extendDialog.setMaxHeight(Region.USE_PREF_SIZE);
    extendDaysField.requestFocus();
  }

  @FXML
  private void closeExtend() {
    extendOverlay.setVisible(false);
    extendOverlay.setManaged(false);
    extendingLoan = null;
  }

  @FXML
  private void extendOverlayClicked(MouseEvent event) {
    if (event.getTarget() == extendOverlay) closeExtend();
  }

  @FXML
  private void extendLoan() {
    if (extendingLoan == null) return;
    Integer days = parseDays(extendDaysField.getText());
    if (days == null) {
      showExtendError(
        "Additional days must be a whole number between 1 and 365."
      );
      return;
    }

    BorrowRecord loan = extendingLoan;
    var body = ApiClient.object().put("additionalDays", days);
    Task<JsonNode> task = new Task<>() {
      @Override
      protected JsonNode call() throws Exception {
        return ApiClient.request(
          "PATCH",
          "/api/admin/loans/" + loan.getId() + "/extend",
          AuthSession.token(),
          null,
          body
        );
      }
    };
    task.setOnSucceeded(e -> {
      String due = formatDue(task.getValue().path("dueDate").asText());
      closeExtend();
      showStatus(
        due.isBlank()
          ? "Loan extended for “" + loan.getBookTitle() + "”."
          : "Loan extended. New due date: " + due + "."
      );
      loadLoans();
    });
    task.setOnFailed(e ->
      showExtendError(ApiClient.message(task.getException()))
    );
    Thread thread = new Thread(task, "slms-extend-loan");
    thread.setDaemon(true);
    thread.start();
  }

  /* ------------------------------------------------------------------ */
  /* Return                                                              */
  /* ------------------------------------------------------------------ */

  private void returnLoan(BorrowRecord loan) {
    Alert confirm = new Alert(
      Alert.AlertType.CONFIRMATION,
      "Return “" +
        loan.getBookTitle() +
        "” from " +
        loan.getMemberName() +
        "? Copy " +
        loan.getBarcode() +
        " will become available again.",
      ButtonType.CANCEL,
      ButtonType.OK
    );
    confirm.setHeaderText("Return book");
    confirm
      .showAndWait()
      .filter(result -> result == ButtonType.OK)
      .ifPresent(result -> performReturn(loan));
  }

  private void performReturn(BorrowRecord loan) {
    var body = ApiClient.object().put("barcode", loan.getBarcode());
    Task<JsonNode> task = new Task<>() {
      @Override
      protected JsonNode call() throws Exception {
        return ApiClient.request(
          "POST",
          "/api/admin/loans/return",
          AuthSession.token(),
          null,
          body
        );
      }
    };
    task.setOnSucceeded(e -> {
      showStatus("“" + loan.getBookTitle() + "” has been returned.");
      loadLoans();
    });
    task.setOnFailed(e -> {
      Alert error = new Alert(
        Alert.AlertType.ERROR,
        ApiClient.message(task.getException())
      );
      error.setHeaderText("Could not return book");
      error.showAndWait();
    });
    Thread thread = new Thread(task, "slms-return-loan");
    thread.setDaemon(true);
    thread.start();
  }

  /* ------------------------------------------------------------------ */
  /* Helpers                                                             */
  /* ------------------------------------------------------------------ */

  private void attachOverlayToWindowRoot(StackPane overlay) {
    if (overlay.getScene() == null) return;
    Parent root = overlay.getScene().getRoot();
    if (overlay.getParent() == root) return;
    if (overlay.getParent() instanceof Pane pane) pane
      .getChildren()
      .remove(overlay);
    if (root instanceof StackPane appRoot) {
      appRoot.getChildren().add(overlay);
      StackPane.setAlignment(overlay, Pos.CENTER);
    }
  }

  private static Integer parseDays(String value) {
    try {
      int days = Integer.parseInt(value == null ? "" : value.trim());
      return days >= 1 && days <= 365 ? days : null;
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private static String formatDue(String iso) {
    if (iso == null || iso.isBlank()) return "";
    try {
      LocalDateTime due = LocalDateTime.parse(iso);
      return due.format(DUE_FORMAT);
    } catch (DateTimeParseException ignored) {
      return "";
    }
  }

  private void showStatus(String message) {
    status.setText(message);
  }

  private void showIssueError(String message) {
    issueError.setText(message);
    issueError.setVisible(true);
    issueError.setManaged(true);
  }

  private void showExtendError(String message) {
    extendError.setText(message);
    extendError.setVisible(true);
    extendError.setManaged(true);
  }
}
