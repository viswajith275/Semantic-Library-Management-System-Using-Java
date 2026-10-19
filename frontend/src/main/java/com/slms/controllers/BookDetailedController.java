package com.slms.controllers;

import com.slms.models.Book;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

/** Controller for the selected book detail FXML. */
public class BookDetailedController {
    @FXML private Label genre;
    @FXML private Label status;
    @FXML private Label title;
    @FXML private Label author;
    @FXML private Label description;
    @FXML private Label copies;
    @FXML private Label isbn;
    @FXML private Label year;
    @FXML private Label topic;
    @FXML private Label coverTitle;
    @FXML private Label coverAuthor;
    @FXML private ImageView coverImage;
    @FXML private Region progress;
    @FXML private Button back;
    @FXML private Button mainAction;

    @FXML private void initialize() {
        Book book = NavigationController.getSelectedBook();
        boolean librarian = "Librarian".equals(NavigationController.getCurrentRole());
        back.setCursor(Cursor.HAND);
        mainAction.setCursor(Cursor.HAND);
        mainAction.setVisible(false); mainAction.setManaged(false);
        if (book == null) return;
        genre.setText(book.getGenre());
        topic.setText(book.getGenre());
        status.setText(book.getAvailableCopies() > 0 ? "Available" : "Checked out");
        status.getStyleClass().setAll(book.getAvailableCopies() > 0 ? "detail-available" : "detail-unavailable");
        title.setText(book.getTitle());
        author.setText("by " + book.getAuthor());
        description.setText(book.getSummary()==null||book.getSummary().isBlank()?"No description provided.":book.getSummary());
        copies.setText(book.getAvailableCopies() + " / " + book.getTotalCopies());
        isbn.setText(book.getIsbn()); year.setText("—");
        coverTitle.setText(book.getTitle()); coverAuthor.setText(book.getAuthor());
        double ratio = book.getTotalCopies() == 0 ? 0 : (double) book.getAvailableCopies() / book.getTotalCopies();
        progress.setMaxWidth(Math.max(0, (300 - 34) * ratio));
        Image image = new Image(book.getImageUrl(), 300, 400, false, true, true);
        coverImage.setImage(image);
        Rectangle clip = new Rectangle(300, 400); clip.setArcWidth(16); clip.setArcHeight(16); coverImage.setClip(clip);
        image.errorProperty().addListener((obs, old, failed) -> coverImage.setVisible(!failed));
    }

    @FXML private void backToCatalogue() { NavigationController.showCatalogue(); }

    private static String descriptionFor(Book book) {
        if (book.getTitle().equals("Clean Code")) return "A handbook of agile software craftsmanship that argues code quality is a professional obligation. Martin walks through naming, functions, comments, formatting, objects, error handling and successive refinement using real refactoring case studies.";
        return book.getTitle() + " by " + book.getAuthor() + " explores the foundations, practical ideas and lasting influence of " + book.getGenre().toLowerCase() + ". A recommended title from the library catalogue.";
    }
}
