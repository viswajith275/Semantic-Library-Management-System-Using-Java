package com.slms.models;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public class MemberLoan {
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

  private long loanId;
  private String bookTitle;
  private String isbn;
  private String barcode;
  private LocalDateTime borrowedAt;
  private LocalDateTime dueDate;
  private long remainingHours;
  private boolean overdue;

  public static MemberLoan from(JsonNode node) {
    MemberLoan loan = new MemberLoan();
    loan.loanId = node.path("loanId").asLong();
    loan.bookTitle = text(node, "bookTitle");
    loan.isbn = text(node, "isbn");
    loan.barcode = text(node, "barcode");
    loan.borrowedAt = parseDate(node.path("borrowedAt"));
    loan.dueDate = parseDate(node.path("dueDate"));
    loan.remainingHours = node.path("remainingHours").asLong(0);
    loan.overdue = node.path("isOverdue").asBoolean(false);
    if (loan.dueDate != null && loan.dueDate.isBefore(LocalDateTime.now())) {
      loan.overdue = true;
    }
    return loan;
  }

  private static String text(JsonNode node, String field) {
    JsonNode v = node.path(field);
    return v.isMissingNode() || v.isNull() ? "" : v.asText();
  }

  private static LocalDateTime parseDate(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) return null;
    String s = node.asText();
    if (s.isBlank()) return null;
    try {
      return LocalDateTime.parse(s);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  public long getLoanId() { return loanId; }
  public String getBookTitle() { return bookTitle; }
  public String getIsbn() { return isbn; }
  public String getBarcode() { return barcode; }
  public LocalDateTime getBorrowedAt() { return borrowedAt; }
  public LocalDateTime getDueDate() { return dueDate; }

  public String getBorrowedAtFormatted() {
    return borrowedAt == null ? "-" : borrowedAt.format(DATE_FMT);
  }

  public String getDueDateFormatted() {
    return dueDate == null ? "-" : dueDate.format(DATE_FMT);
  }

  public long getRemainingDays() {
    if (dueDate == null) return 0;
    return ChronoUnit.DAYS.between(LocalDateTime.now().toLocalDate(), dueDate.toLocalDate());
  }

  public boolean isOverdue() { return overdue; }

  public String getStatusLabel() {
    if (overdue) return "Overdue";
    long days = getRemainingDays();
    if (days <= 0) return "Due today";
    if (days == 1) return "Due tomorrow";
    if (days <= 3) return "Due soon";
    return "On loan";
  }

  public String getStatusClass() {
    if (overdue) return "member-loan-status-overdue";
    long days = getRemainingDays();
    if (days <= 3 && days >= 0) return "member-loan-status-due";
    return "member-loan-status-active";
  }

  public String getTimeLeftText() {
    if (overdue) {
      long daysOverdue = Math.abs(getRemainingDays());
      if (daysOverdue == 0) {
        // calculate hours roughly
        if (dueDate != null) {
          long hours = Math.abs(ChronoUnit.HOURS.between(dueDate, LocalDateTime.now()));
          return hours + (hours == 1 ? " hour overdue" : " hours overdue");
        }
        return "Overdue";
      }
      return daysOverdue + (daysOverdue == 1 ? " day overdue" : " days overdue");
    }
    long days = getRemainingDays();
    if (days == 0) return "Due today";
    if (days == 1) return "1 day left";
    return days + " days left";
  }
}
