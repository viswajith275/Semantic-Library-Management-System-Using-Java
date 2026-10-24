package com.slms.controllers;

import com.slms.models.MemberLoan;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MemberLoanCardController {

  @FXML
  private Label bookTitle, isbn, barcode, borrowedDate, dueDate, statusBadge, timeLeft;

  public void configure(MemberLoan loan) {
    bookTitle.setText(loan.getBookTitle());
    isbn.setText("ISBN: " + loan.getIsbn());
    barcode.setText(loan.getBarcode());
    borrowedDate.setText(loan.getBorrowedAtFormatted());
    dueDate.setText(loan.getDueDateFormatted());

    statusBadge.setText(loan.getStatusLabel());
    statusBadge
      .getStyleClass()
      .removeAll(
        "member-loan-status-badge-active",
        "member-loan-status-badge-due",
        "member-loan-status-badge-overdue"
      );
    statusBadge.getStyleClass().add(getBadgeClass(loan));

    timeLeft.setText(loan.getTimeLeftText());
    timeLeft
      .getStyleClass()
      .removeAll(
        "member-loan-time-left-normal",
        "member-loan-time-left-warning",
        "member-loan-time-left-danger"
      );
    timeLeft.getStyleClass().add(getTimeClass(loan));
  }

  private String getBadgeClass(MemberLoan loan) {
    if (loan.isOverdue()) return "member-loan-status-badge-overdue";
    long days = loan.getRemainingDays();
    if (days <= 3 && days >= 0) return "member-loan-status-badge-due";
    return "member-loan-status-badge-active";
  }

  private String getTimeClass(MemberLoan loan) {
    if (loan.isOverdue()) return "member-loan-time-left-danger";
    long days = loan.getRemainingDays();
    if (days <= 3 && days >= 0) return "member-loan-time-left-warning";
    return "member-loan-time-left-normal";
  }
}
