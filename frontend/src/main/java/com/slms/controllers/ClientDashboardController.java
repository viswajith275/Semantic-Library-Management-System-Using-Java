package com.slms.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.slms.models.Book;
import com.slms.services.ApiClient;
import com.slms.services.AuthSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Member landing page backed by the library dashboard and semantic search APIs. */
public class ClientDashboardController {

  private static final String[] COVER_COLORS = {
    "#193d68", "#4d39a0", "#9c3f35", "#392d72", "#80501c", "#087366"
  };

  @FXML private Label welcomeTitle, bookCount, availableCount, pinnedCount;
  @FXML private Label booksHeading, resultCount, loadError, booksEmptyTitle, booksEmptyCopy;
  @FXML private TextField searchField;
  @FXML private GridPane bookGrid;
  @FXML private VBox booksEmptyState;

  private final Set<String> pinnedIsbns = new HashSet<>();
  private List<Book> recommendations = List.of();
  private List<Book> displayedBooks = List.of();

  private record DashboardPayload(JsonNode dashboard, Set<String> pins) {}

  @FXML
  private void initialize() {
    String name = AuthSession.name();
    welcomeTitle.setText(name == null || name.isBlank() ? "Welcome to your library" : "Welcome back, " + name.trim());
    loadDashboard();
  }

  private void loadDashboard() {
    setLoading(true);
    Task<DashboardPayload> task = new Task<>() {
      @Override
      protected DashboardPayload call() throws Exception {
        JsonNode dashboard = ApiClient.request("GET", "/api/dashboard", null, AuthSession.code(), null);
        Set<String> pins = new HashSet<>();
        try {
          JsonNode response = ApiClient.request("GET", "/api/member/pins", null, AuthSession.code(), null);
          if (response.isArray()) response.forEach(pin -> pins.add(pin.path("isbn").asText()));
        } catch (Exception ignored) {
          // Dashboard data remains useful even if wishlist loading is temporarily unavailable.
        }
        return new DashboardPayload(dashboard, pins);
      }
    };
    task.setOnSucceeded(event -> {
      JsonNode dashboard = task.getValue().dashboard();
      pinnedIsbns.clear();
      pinnedIsbns.addAll(task.getValue().pins());
      JsonNode stats = dashboard.path("stats");
      bookCount.setText(String.valueOf(stats.path("totalBooks").asLong(0)));
      availableCount.setText(String.valueOf(stats.path("availableCopies").asLong(0)));
      pinnedCount.setText(String.valueOf(dashboard.path("pinnedBooks").asLong(pinnedIsbns.size())));

      List<Book> books = new ArrayList<>();
      JsonNode recommendationData = dashboard.path("recommendations");
      if (recommendationData.isArray()) recommendationData.forEach(node -> books.add(Book.from(node)));
      recommendations = List.copyOf(books);
      booksHeading.setText("Recommended for you");
      renderBooks(recommendations);
      resultCount.setText(recommendations.size() + (recommendations.size() == 1 ? " pick" : " picks"));
      loadError.setVisible(false);
      loadError.setManaged(false);
      setLoading(false);
    });
    task.setOnFailed(event -> {
      setLoading(false);
      loadError.setText("Could not load your dashboard: " + ApiClient.message(task.getException()));
      loadError.setVisible(true);
      loadError.setManaged(true);
      renderBooks(List.of());
    });
    Thread thread = new Thread(task, "slms-client-dashboard");
    thread.setDaemon(true);
    thread.start();
  }

  @FXML
  private void search() {
    String query = searchField.getText() == null ? "" : searchField.getText().trim();
    if (query.isBlank()) {
      booksHeading.setText("Recommended for you");
      renderBooks(recommendations);
      resultCount.setText(recommendations.size() + (recommendations.size() == 1 ? " pick" : " picks"));
      return;
    }

    booksHeading.setText("Search results");
    resultCount.setText("Searching…");
    booksEmptyTitle.setText("No matching books found");
    booksEmptyCopy.setText("Try another title, author, subject, or idea.");
    Task<List<Book>> task = new Task<>() {
      @Override
      protected List<Book> call() throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        JsonNode response = ApiClient.request(
          "GET", "/api/library/search?q=" + encoded + "&topK=12", null, AuthSession.code(), null
        );
        List<Book> found = new ArrayList<>();
        if (response.isArray()) response.forEach(node -> found.add(Book.from(node)));
        return found;
      }
    };
    task.setOnSucceeded(event -> {
      renderBooks(task.getValue());
      resultCount.setText(task.getValue().size() + (task.getValue().size() == 1 ? " result" : " results"));
      loadError.setVisible(false);
      loadError.setManaged(false);
    });
    task.setOnFailed(event -> {
      resultCount.setText("Search unavailable");
      loadError.setText("Could not search books: " + ApiClient.message(task.getException()));
      loadError.setVisible(true);
      loadError.setManaged(true);
    });
    Thread thread = new Thread(task, "slms-client-semantic-search");
    thread.setDaemon(true);
    thread.start();
  }

  private void renderBooks(List<Book> books) {
    displayedBooks = List.copyOf(books);
    bookGrid.getChildren().clear();
    int index = 0;
    for (Book book : books) {
      try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/components/BookCard.fxml"));
        bookGrid.add(loader.load(), index % 3, index / 3);
        BookCardController card = loader.getController();
        card.configure(book, index, false, COVER_COLORS[index % COVER_COLORS.length]);
        card.setInterestAction(this::togglePin, pinnedIsbns.contains(book.getIsbn()));
      } catch (IOException exception) {
        throw new IllegalStateException("Could not load a recommended book card", exception);
      }
      index++;
    }
    boolean hasBooks = !books.isEmpty();
    bookGrid.setVisible(hasBooks);
    bookGrid.setManaged(hasBooks);
    booksEmptyState.setVisible(!hasBooks);
    booksEmptyState.setManaged(!hasBooks);
  }

  private void togglePin(Book book) {
    boolean removing = pinnedIsbns.contains(book.getIsbn());
    Task<JsonNode> task = new Task<>() {
      @Override
      protected JsonNode call() throws Exception {
        String path = "/api/member/pins/" + URLEncoder.encode(book.getIsbn(), StandardCharsets.UTF_8);
        return ApiClient.request(removing ? "DELETE" : "POST", path, null, AuthSession.code(), null);
      }
    };
    task.setOnSucceeded(event -> {
      if (removing) pinnedIsbns.remove(book.getIsbn()); else pinnedIsbns.add(book.getIsbn());
      pinnedCount.setText(String.valueOf(pinnedIsbns.size()));
      renderBooks(currentBooks());
    });
    task.setOnFailed(event -> {
      Alert alert = new Alert(Alert.AlertType.ERROR, "Could not update your wishlist: " + ApiClient.message(task.getException()), ButtonType.OK);
      alert.show();
    });
    Thread thread = new Thread(task, "slms-dashboard-pin-book");
    thread.setDaemon(true);
    thread.start();
  }

  private List<Book> currentBooks() {
    return displayedBooks;
  }

  @FXML
  private void viewAllBooks() {
    NavigationController.showBrowseBooks();
  }

  private void setLoading(boolean loading) {
    if (loading) {
      bookCount.setText("—");
      availableCount.setText("—");
      pinnedCount.setText("—");
      resultCount.setText("Loading your recommendations…");
    }
  }
}
