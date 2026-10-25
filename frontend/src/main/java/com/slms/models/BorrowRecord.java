package com.slms.models;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * UI model for an active loan from {@code GET /api/admin/loans/active}.
 *
 * <p>The backend supplies the overdue flag; the remaining day counts are derived locally for
 * display only. Servers remain the source of truth for circulation rules.</p>
 */
public class BorrowRecord {

  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern(
    "d MMM yyyy"
  );

  private long id;
  private String bookTitle;
  private String isbn;
  private String barcode;
  private String memberName;
  private String memberCustomCode;
  private LocalDateTime borrowedAt;
  private LocalDateTime dueDate;
  private boolean overdue;

  public static BorrowRecord from(JsonNode node) {
    BorrowRecord loan = new BorrowRecord();
    loan.id = node.path("id").asLong();
    loan.bookTitle = text(node, "bookTitle");
    loan.isbn = text(node, "isbn");
    loan.barcode = text(node, "barcode");
    loan.memberName = text(node, "memberName");
    loan.memberCustomCode = text(node, "memberCustomCode");
    loan.borrowedAt = dateTime(node, "borrowedAt");
    loan.dueDate = dateTime(node, "dueDate");
    loan.overdue = node.path("isOverdue").asBoolean(false);
    return loan;
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.path(field);
    return value.isMissingNode() || value.isNull() ? "" : value.asText();
  }

  private static LocalDateTime dateTime(JsonNode node, String field) {
    String value = text(node, field);
    if (value.isBlank()) return null;
    try {
      return LocalDateTime.parse(value);
    } catch (DateTimeParseException ignored) {
      return null;
    }
  }

  public long getId() {
    return id;
  }

  public String getBookTitle() {
    return bookTitle;
  }

  public String getIsbn() {
    return isbn;
  }

  public String getBarcode() {
    return barcode;
  }

  public String getMemberName() {
    return memberName;
  }

  public String getMemberCustomCode() {
    return memberCustomCode;
  }

  public LocalDateTime getBorrowedAt() {
    return borrowedAt;
  }

  public LocalDateTime getDueDate() {
    return dueDate;
  }

  public String getBorrowedDisplay() {
    return borrowedAt == null ? "—" : borrowedAt.format(DATE);
  }

  public String getDueDisplay() {
    return dueDate == null ? "—" : dueDate.format(DATE);
  }

  /** Whole days from today until the due date; negative means overdue. */
  public long getDaysUntilDue() {
    if (dueDate == null) return 0;
    return ChronoUnit.DAYS.between(LocalDate.now(), dueDate.toLocalDate());
  }

  public boolean isOverdue() {
    return overdue || getDaysUntilDue() < 0;
  }

  public boolean isDueSoon() {
    return !isOverdue() && getDaysUntilDue() <= 3;
  }

  public String getStatusLabel() {
    if (isOverdue()) return "Overdue";
    if (isDueSoon()) return "Due soon";
    return "On loan";
  }

  public String getStatusStyleClass() {
    if (isOverdue()) return "loan-status-overdue";
    if (isDueSoon()) return "loan-status-due";
    return "loan-status-active";
  }

  /** Short helper line shown under the due date. */
  public String getDueHint() {
    if (dueDate == null) return "";
    long days = getDaysUntilDue();
    if (days < 0) {
      long late = Math.abs(days);
      return late + (late == 1 ? " day overdue" : " days overdue");
    }
    if (days == 0) return "Due today";
    if (days == 1) return "Due tomorrow";
    return "Due in " + days + " days";
  }

  public boolean matches(String query) {
    if (query == null || query.isBlank()) return true;
    String needle = query.trim().toLowerCase();
    return (
      contains(bookTitle, needle) ||
      contains(isbn, needle) ||
      contains(barcode, needle) ||
      contains(memberName, needle) ||
      contains(memberCustomCode, needle)
    );
  }

  private static boolean contains(String value, String needle) {
    return value != null && value.toLowerCase().contains(needle);
  }
}
