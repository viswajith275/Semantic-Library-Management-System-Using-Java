package com.slms.controllers;

import com.slms.models.Book;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

/** Binds catalogue book values and actions to the FXML card. */
public class BookCardController {
    @FXML private Label stock;
    @FXML private Label title;
    @FXML private Label author;
    @FXML private Label fallbackTitle;
    @FXML private Label fallbackAuthor;
    @FXML private Label isbn;
    @FXML private Label genre;
    @FXML private Label year;
    @FXML private Label copies;
    @FXML private ImageView coverImage;
    @FXML private Button viewButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button borrowButton;
    @FXML private Button interestButton;

    void configure(Book book, int index, boolean librarian, String color) {
        stock.setText(book.getAvailableCopies() == 0 ? "All copies out" : book.getAvailableCopies() + " available");
        stock.getStyleClass().setAll(book.getAvailableCopies() == 0 ? "stock-out" : "stock-available");
        title.setText(book.getTitle()); author.setText(book.getAuthor()); isbn.setText(book.getIsbn());
        fallbackTitle.setText(book.getTitle()); fallbackAuthor.setText(book.getAuthor());
        genre.setText(book.getGenre()); year.setText(Integer.toString(book.getYear())); copies.setText(book.getTotalCopies() + " copies");
        copies.setVisible(librarian); copies.setManaged(librarian);
        editButton.setVisible(librarian); editButton.setManaged(librarian);
        deleteButton.setVisible(librarian); deleteButton.setManaged(librarian);
        borrowButton.setVisible(!librarian && book.getAvailableCopies() > 0); borrowButton.setManaged(borrowButton.isVisible());
        interestButton.setVisible(!librarian && book.getAvailableCopies() == 0); interestButton.setManaged(interestButton.isVisible());
        viewButton.setOnAction(event -> NavigationController.showBookDetails(book, librarian));
        Image image = new Image(book.getImageUrl(), 78, 112, false, true, true);
        coverImage.setImage(image);
        Rectangle clip = new Rectangle(78, 112); clip.setArcWidth(12); clip.setArcHeight(12); coverImage.setClip(clip);
        image.errorProperty().addListener((obs, oldValue, failed) -> coverImage.setVisible(!failed));
        coverImage.getParent().setStyle("-fx-background-color: " + color + ";");
        viewButton.setCursor(Cursor.HAND); editButton.setCursor(Cursor.HAND); deleteButton.setCursor(Cursor.HAND);
        borrowButton.setCursor(Cursor.HAND); interestButton.setCursor(Cursor.HAND);
    }
}
