package com.slms.controllers;

import com.slms.models.Book;
import com.slms.services.CoverImageService;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/** Binds catalogue book values and actions to the FXML card. */
public class BookCardController {

  private java.util.function.Consumer<Book> editAction;
  private java.util.function.Consumer<Book> deleteAction;

  @FXML
  private Label stock;

  @FXML
  private Label title;

  @FXML
  private Label author;

  @FXML
  private Label fallbackTitle;

  @FXML
  private Label fallbackAuthor;

  @FXML
  private Label isbn;

  @FXML
  private Label genre;

  @FXML
  private Label year;

  @FXML
  private Label copies;

  @FXML
  private VBox barcodeSection;

  @FXML
  private FlowPane barcodeList;

  @FXML
  private ImageView coverImage;

  @FXML
  private Button viewButton;

  @FXML
  private Button editButton;

  @FXML
  private Button deleteButton;

  @FXML
  private Button borrowButton;

  @FXML
  private Button interestButton;
  private java.util.function.Consumer<Book> interestAction;

  void configure(Book book, int index, boolean librarian, String color) {
    configure(book, index, librarian, color, b -> {}, b -> {});
  }

  void configure(
    Book book,
    int index,
    boolean librarian,
    String color,
    java.util.function.Consumer<Book> editAction,
    java.util.function.Consumer<Book> deleteAction
  ) {
    this.editAction = editAction;
    this.deleteAction = deleteAction;
    stock.setText(
      book.getAvailableCopies() == 0
        ? "All copies out"
        : book.getAvailableCopies() + " available"
    );
    stock
      .getStyleClass()
      .setAll(book.getAvailableCopies() == 0 ? "stock-out" : "stock-available");
    title.setText(book.getTitle());
    author.setText(book.getAuthor());
    isbn.setText(book.getIsbn());
    fallbackTitle.setText(book.getTitle());
    fallbackAuthor.setText(book.getAuthor());
    genre.setText(book.getShelfLocation() == null || book.getShelfLocation().isBlank()
      ? "Location unavailable" : book.getShelfLocation());
    year.setVisible(false);
    year.setManaged(false);
    copies.setText(book.getTotalCopies() + " copies");
    copies.setVisible(librarian);
    copies.setManaged(librarian);
    renderBarcodes(book, librarian);
    editButton.setVisible(librarian);
    editButton.setManaged(librarian);
    deleteButton.setVisible(librarian);
    deleteButton.setManaged(librarian);
    borrowButton.setVisible(false);
    borrowButton.setManaged(false);
    interestButton.setVisible(!librarian);
    interestButton.setManaged(!librarian);
    viewButton.setOnAction(event ->
      NavigationController.showBookDetails(book, librarian)
    );
    editButton.setOnAction(event -> this.editAction.accept(book));
    deleteButton.setOnAction(event -> this.deleteAction.accept(book));
    interestButton.setOnAction(event -> { if (interestAction != null) interestAction.accept(book); });
    coverImage.setImage(null);
    coverImage.setVisible(true);
    Rectangle clip = new Rectangle(78, 112);
    clip.setArcWidth(12);
    clip.setArcHeight(12);
    coverImage.setClip(clip);
    CoverImageService.load(book, coverImage, 78, 112);
    coverImage.getParent().setStyle("-fx-background-color: " + color + ";");

    viewButton.setCursor(Cursor.HAND);
    editButton.setCursor(Cursor.HAND);
    deleteButton.setCursor(Cursor.HAND);
    borrowButton.setCursor(Cursor.HAND);
    interestButton.setCursor(Cursor.HAND);
  }

  void setInterestAction(java.util.function.Consumer<Book> action, boolean pinned) {
    interestAction = action;
    interestButton.setText(pinned ? "In wishlist" : "Add to wishlist");
    interestButton.setAccessibleText(pinned ? "Remove book from wishlist" : "Add book to wishlist");
    interestButton.getStyleClass().remove("book-interest-pinned");
    if (pinned) interestButton.getStyleClass().add("book-interest-pinned");
  }

  private void renderBarcodes(Book book, boolean librarian) {
    barcodeSection.setVisible(librarian);
    barcodeSection.setManaged(librarian);
    barcodeList.getChildren().clear();
    if (!librarian) return;

    if (book.getCopies().isEmpty()) {
      Label none = new Label("No copies yet");
      none.getStyleClass().addAll("book-barcode-chip", "book-barcode-none");
      barcodeList.getChildren().add(none);
      return;
    }

    for (Book.Copy copy : book.getCopies()) {
      Label chip = new Label(copy.barcode());
      chip.getStyleClass().addAll("book-barcode-chip", statusStyle(copy.status()));
      chip.setMaxWidth(150);
      Tooltip.install(chip, new Tooltip(copy.barcode() + " · " + statusLabel(copy.status())));
      barcodeList.getChildren().add(chip);
    }
  }

  private static String statusStyle(String status) {
    return switch (status == null ? "" : status) {
      case "AVAILABLE" -> "book-barcode-available";
      case "BORROWED" -> "book-barcode-borrowed";
      case "MAINTENANCE" -> "book-barcode-maintenance";
      default -> "book-barcode-none";
    };
  }

  private static String statusLabel(String status) {
    return switch (status == null ? "" : status) {
      case "AVAILABLE" -> "Available";
      case "BORROWED" -> "On loan";
      case "MAINTENANCE" -> "Maintenance";
      default -> "Unknown status";
    };
  }
}
