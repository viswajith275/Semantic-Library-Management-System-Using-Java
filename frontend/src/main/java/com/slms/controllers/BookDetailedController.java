package com.slms.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.slms.models.Book;
import com.slms.services.ApiClient;
import com.slms.services.AuthSession;
import com.slms.services.CoverImageService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/** Detail page populated from a book record and live semantic matches. */
public class BookDetailedController {

  @FXML
  private Label status, title, author, description, copies, isbn, shelfLocation, topic;

  @FXML
  private Label coverTitle, coverAuthor, similarState;

  @FXML
  private ImageView coverImage;

  @FXML
  private Region progress;

  @FXML
  private StackPane stockTrack;

  @FXML
  private HBox similarBooks;

  @FXML
  private javafx.scene.control.Button back;

  @FXML
  private void initialize() {
    Book book = NavigationController.getSelectedBook();
    boolean librarian = "Librarian".equals(
      NavigationController.getCurrentRole()
    );
    back.setCursor(Cursor.HAND);
    if (book == null) return;
    renderBook(book);
    similarState.setText("Finding books with similar ideas…");
    loadCompleteBook(book, librarian);
  }

  private void renderBook(Book book) {
    topic.setText(
      book.getCharacters() == null || book.getCharacters().isBlank()
        ? "No concepts listed"
        : book.getCharacters()
    );
    boolean available = book.getAvailableCopies() > 0;
    status.setText(statusText(book, available));
    status
      .getStyleClass()
      .setAll(available ? "detail-available" : "detail-unavailable");
    title.setText(book.getTitle());
    author.setText("by " + book.getAuthor());
    description.setText(
      book.getSummary() == null || book.getSummary().isBlank()
        ? "No description provided."
        : book.getSummary()
    );
    copies.setText(
      book.getTotalCopies() > 0
        ? book.getAvailableCopies() + " / " + book.getTotalCopies()
        : book.getAvailableCopies() + " available"
    );
    isbn.setText(book.getIsbn());
    shelfLocation.setText(
      book.getShelfLocation() == null || book.getShelfLocation().isBlank()
        ? "Not assigned"
        : book.getShelfLocation()
    );
    coverTitle.setText(book.getTitle());
    coverAuthor.setText(book.getAuthor());
    stockTrack.setVisible(book.getTotalCopies() > 0);
    stockTrack.setManaged(book.getTotalCopies() > 0);
    double ratio =
      book.getTotalCopies() <= 0
        ? 0
        : (double) book.getAvailableCopies() / book.getTotalCopies();
    progress.setMaxWidth(Math.max(0, (300 - 34) * ratio));
    coverImage.setImage(null);
    coverImage.setVisible(true);
    Rectangle clip = new Rectangle(300, 400);
    clip.setArcWidth(16);
    clip.setArcHeight(16);
    coverImage.setClip(clip);
    CoverImageService.load(book, coverImage, 300, 400);
  }

  private void loadCompleteBook(Book initialBook, boolean librarian) {
    Task<Book> task = new Task<>() {
      @Override
      protected Book call() throws Exception {
        String isbn = URLEncoder.encode(
          initialBook.getIsbn(),
          StandardCharsets.UTF_8
        );
        JsonNode result = ApiClient.request(
          "GET",
          "/api/library/books/" + isbn,
          librarian ? AuthSession.token() : null,
          librarian ? null : AuthSession.code(),
          null
        );
        return Book.from(result);
      }
    };
    task.setOnSucceeded(event -> {
      Book fullBook = task.getValue();
      renderBook(fullBook);
      loadSimilarBooks(fullBook, librarian);
    });
    task.setOnFailed(event -> loadSimilarBooks(initialBook, librarian));
    Thread thread = new Thread(task, "slms-book-details");
    thread.setDaemon(true);
    thread.start();
  }

  private static String statusText(Book book, boolean available) {
    if (available) return "Available";
    return switch (
      book.getAvailabilityStatus() == null ? "" : book.getAvailabilityStatus()
    ) {
      case "ALL_BORROWED" -> "All copies borrowed";
      case "OVERDUE_PENDING_RETURN" -> "Return overdue";
      case "UNAVAILABLE_MAINTENANCE" -> "Under maintenance";
      case "NO_COPIES" -> "No copies";
      default -> "Unavailable";
    };
  }

  private void loadSimilarBooks(Book book, boolean librarian) {
    similarState.setText("Finding books with similar ideas…");
    similarState.setVisible(true);
    Task<List<Book>> task = new Task<>() {
      @Override
      protected List<Book> call() throws Exception {
        String query = String.join(
          " ",
          List.of(
            safe(book.getTitle()),
            safe(book.getAuthor()),
            safe(book.getCharacters()),
            safe(book.getSummary())
          )
        ).trim();
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        JsonNode response = ApiClient.request(
          "GET",
          "/api/library/search?q=" + encoded + "&topK=5",
          librarian ? AuthSession.token() : null,
          librarian ? null : AuthSession.code(),
          null
        );
        List<Book> matches = new ArrayList<>();
        if (response.isArray()) {
          response.forEach(node -> {
            Book match = Book.from(node);
            if (
              !match.getIsbn().equals(book.getIsbn()) &&
              matches
                .stream()
                .noneMatch(existing ->
                  existing.getIsbn().equals(match.getIsbn())
                )
            ) {
              matches.add(match);
            }
          });
        }
        return matches.stream().limit(2).toList();
      }
    };
    task.setOnSucceeded(event ->
      renderSimilarBooks(task.getValue(), librarian)
    );
    task.setOnFailed(event -> {
      similarBooks.getChildren().clear();
      similarState.setText("Similar books are unavailable right now.");
      similarState.setVisible(true);
      similarState.setManaged(true);
    });
    Thread thread = new Thread(task, "slms-semantic-book-matches");
    thread.setDaemon(true);
    thread.start();
  }

  private void renderSimilarBooks(List<Book> matches, boolean librarian) {
    similarBooks.getChildren().clear();
    if (matches.isEmpty()) {
      similarState.setText("No similar titles were found for this book.");
      similarState.setVisible(true);
      similarState.setManaged(true);
      return;
    }
    similarState.setVisible(false);
    similarState.setManaged(false);
    for (Book match : matches) {
      VBox card = new VBox(7);
      card.getStyleClass().add("detail-neighbor");
      card.setMaxWidth(Double.MAX_VALUE);
      HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
      card.setCursor(Cursor.HAND);

      Label matchTitle = new Label(match.getTitle());
      matchTitle.setWrapText(true);
      matchTitle.getStyleClass().add("detail-neighbor-title");
      Label matchAuthor = new Label(match.getAuthor());
      matchAuthor.setWrapText(true);
      matchAuthor.getStyleClass().add("detail-neighbor-author");
      Label score = new Label(scoreText(match.getSimilarityScore()));
      score.getStyleClass().add("detail-neighbor-score");
      card.getChildren().addAll(matchTitle, matchAuthor, score);
      card.setOnMouseClicked(event ->
        NavigationController.showBookDetails(match, librarian)
      );
      similarBooks.getChildren().add(card);
    }
  }

  private static String scoreText(Double score) {
    if (score == null || !Double.isFinite(score)) return "Semantic match";
    return Math.round(score * 100) + "% match";
  }

  private static String safe(String value) {
    return value == null ? "" : value.trim();
  }

  @FXML
  private void backToCatalogue() {
    if (
      "BrowseBooks".equals(NavigationController.getSelectedPage())
    ) NavigationController.showBrowseBooks();
    else NavigationController.showCatalogue();
  }
}
