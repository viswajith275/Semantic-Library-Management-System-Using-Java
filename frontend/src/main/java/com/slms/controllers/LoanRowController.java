package com.slms.controllers;

import com.slms.models.BorrowRecord;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** One active loan row in the librarian borrow-records table. */
public class LoanRowController {

  @FXML
  private HBox root;

  @FXML
  private Label bookTitle, isbn, barcode, memberName, memberCode, borrowed, due, dueHint, status;

  private BorrowRecord loan;
  private Consumer<BorrowRecord> extendAction, returnAction;

  public void configure(
    BorrowRecord loan,
    Consumer<BorrowRecord> extendAction,
    Consumer<BorrowRecord> returnAction
  ) {
    this.loan = loan;
    this.extendAction = extendAction;
    this.returnAction = returnAction;

    bookTitle.setText(loan.getBookTitle());
    isbn.setText("ISBN " + loan.getIsbn());
    barcode.setText(loan.getBarcode());
    memberName.setText(loan.getMemberName());
    memberCode.setText(loan.getMemberCustomCode());
    borrowed.setText(loan.getBorrowedDisplay());
    due.setText(loan.getDueDisplay());
    dueHint.setText(loan.getDueHint());

    status.setText(loan.getStatusLabel());
    status
      .getStyleClass()
      .removeAll(
        "loan-status-active",
        "loan-status-due",
        "loan-status-overdue"
      );
    status.getStyleClass().add(loan.getStatusStyleClass());

    due.getStyleClass().removeAll("loan-date-overdue", "loan-date-due");
    dueHint
      .getStyleClass()
      .removeAll("loan-due-hint-overdue", "loan-due-hint-due");
    if (loan.isOverdue()) {
      due.getStyleClass().add("loan-date-overdue");
      dueHint.getStyleClass().add("loan-due-hint-overdue");
    } else if (loan.isDueSoon()) {
      due.getStyleClass().add("loan-date-due");
      dueHint.getStyleClass().add("loan-due-hint-due");
    }
  }

  @FXML
  private void extend() {
    if (extendAction != null) extendAction.accept(loan);
  }

  @FXML
  private void returnLoan() {
    if (returnAction != null) returnAction.accept(loan);
  }
}
