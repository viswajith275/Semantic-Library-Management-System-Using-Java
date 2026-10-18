package com.slms.controllers;

import com.slms.models.Book;
import java.io.IOException;
import java.util.List;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Controller for the catalogue FXML. */
public class BookCatalogController {
    private static final ObservableList<Book> BOOKS = FXCollections.observableArrayList(List.of(
        new Book("Clean Code", "Robert C. Martin", "Software Engineering", "9780132350884", 2008, 5, 3, cover("9780132350884")),
        new Book("Deep Learning", "Goodfellow, Bengio & Courville", "Artificial Intelligence", "9780262035613", 2016, 4, 2, cover("9780262035613")),
        new Book("Atomic Habits", "James Clear", "Psychology", "9780735211292", 2018, 4, 0, cover("9780735211292")),
        new Book("Cosmos", "Carl Sagan", "Astronomy", "9780345539434", 1980, 6, 4, cover("9780345539434")),
        new Book("Sapiens", "Yuval Noah Harari", "History", "9780062316097", 2011, 3, 1, cover("9780062316097")),
        new Book("Designing Data-Intensive Applications", "Martin Kleppmann", "Distributed Systems", "9781449373320", 2017, 4, 2, cover("9781449373320")),
        new Book("Artificial Intelligence: A Modern Approach", "Russell & Norvig", "Artificial Intelligence", "9780134610993", 2020, 3, 0, cover("9780134610993")),
        new Book("The Pragmatic Programmer", "Hunt & Thomas", "Software Engineering", "9780135957059", 2019, 5, 3, cover("9780135957059")),
        new Book("Thinking, Fast and Slow", "Daniel Kahneman", "Psychology", "9780374533557", 2011, 4, 2, cover("9780374533557")),
        new Book("A Brief History of Time", "Stephen Hawking", "Astronomy", "9780553380163", 1988, 3, 2, cover("9780553380163")),
        new Book("Hands-On Machine Learning", "Aurélien Géron", "Artificial Intelligence", "9781098125974", 2022, 6, 5, cover("9781098125974")),
        new Book("The Selfish Gene", "Richard Dawkins", "Science", "9780198788607", 1976, 3, 0, cover("9780198788607"))
    ));
    private static final String[] COVER_COLORS = {"#193d68", "#4d39a0", "#9c3f35", "#392d72", "#80501c", "#087366", "#265aa0", "#244b7c"};

    @FXML private Label eyebrow;
    @FXML private Label subtitle;
    @FXML private Button addBook;
    @FXML private Button borrowings;
    @FXML private GridPane bookGrid;
    @FXML private StackPane filterOverlay;
    @FXML private Button statusTrigger;
    @FXML private VBox statusMenu;
    @FXML private Label resultCount;
    @FXML private StackPane createOverlay;
    @FXML private VBox createDialog;
    @FXML private TextField newTitle;
    @FXML private TextField newAuthor;
    @FXML private TextField newIsbn;
    @FXML private ComboBox<String> newGenre;
    @FXML private TextField newYear;
    @FXML private Label createError;
    private boolean librarian;

    @FXML private void initialize() {
        librarian = "Librarian".equals(NavigationController.getCurrentRole());
        eyebrow.setText(librarian ? "CATALOGUE MANAGEMENT" : "BROWSE THE SHELF");
        subtitle.setText(librarian
            ? "Add, edit and retire catalogue records. 12,486 titles indexed across 1,204 concepts."
            : "Search the shelf in plain language, filter by genre or author, and borrow in one tap.");
        addBook.setVisible(librarian); addBook.setManaged(librarian);
        borrowings.setVisible(!librarian); borrowings.setManaged(!librarian);
        newGenre.setItems(FXCollections.observableArrayList("Artificial Intelligence", "Astronomy", "Distributed Systems", "History", "Psychology", "Science", "Software Engineering"));
        newYear.setText(Integer.toString(java.time.Year.now().getValue()));
        for (int i = 0; i < BOOKS.size(); i++) {
            addBookCard(BOOKS.get(i), i);
        }
    }

    @FXML private void openCreateDialog() {
        if (!librarian) return;
        createDialog.setMaxHeight(Region.USE_PREF_SIZE);
        if (createOverlay.getScene() != null && createOverlay.getScene().getRoot() instanceof StackPane appRoot
                && createOverlay.getParent() != appRoot) {
            if (createOverlay.getParent() instanceof Pane currentParent) {
                currentParent.getChildren().remove(createOverlay);
            }
            appRoot.getChildren().add(createOverlay);
        }
        createError.setText(""); createError.setVisible(false); createError.setManaged(false);
        createOverlay.setVisible(true); createOverlay.setManaged(true);
        newTitle.requestFocus();
    }

    @FXML private void closeCreateDialog() {
        createOverlay.setVisible(false); createOverlay.setManaged(false);
    }

    @FXML private void saveBook() {
        String title = newTitle.getText().trim();
        String author = newAuthor.getText().trim();
        String isbn = newIsbn.getText().trim();
        String genre = newGenre.getValue();
        String yearText = newYear.getText().trim();
        int year;
        try {
            year = Integer.parseInt(yearText);
            if (year < 0 || year > java.time.Year.now().getValue()) throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            showCreateError("Enter a valid publication year."); return;
        }
        if (title.isEmpty() || author.isEmpty() || isbn.isEmpty() || genre == null) {
            showCreateError("Complete all fields to save this book."); return;
        }
        if (BOOKS.stream().anyMatch(book -> book.getIsbn().equalsIgnoreCase(isbn))) {
            showCreateError("A book with this ISBN is already in the catalogue."); return;
        }
        String imageIsbn = isbn.replaceAll("[^0-9Xx]", "");
        Book book = new Book(title, author, genre, isbn, year, 1, 1, cover(imageIsbn));
        BOOKS.add(book);
        addBookCard(book, BOOKS.size() - 1);
        resultCount.setText("▽  " + BOOKS.size() + " of " + BOOKS.size() + " titles");
        newTitle.clear(); newAuthor.clear(); newIsbn.clear(); newGenre.getSelectionModel().clearSelection();
        newYear.setText(Integer.toString(java.time.Year.now().getValue()));
        closeCreateDialog();
    }

    private void addBookCard(Book book, int index) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/components/BookCard.fxml"));
            bookGrid.add(loader.load(), index % 4, index / 4);
            loader.<BookCardController>getController().configure(book, index, librarian, COVER_COLORS[index % COVER_COLORS.length]);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load a catalogue card", exception);
        }
    }

    private void showCreateError(String message) {
        createError.setText(message); createError.setVisible(true); createError.setManaged(true);
    }

    @FXML private void toggleStatusMenu() {
        boolean open = !statusMenu.isVisible();
        statusMenu.setVisible(open); statusMenu.setManaged(open);
        if (open) {
            statusMenu.applyCss(); statusMenu.resize(152, 126); statusMenu.layout();
            statusMenu.relocate(filterOverlay.getWidth() - 14 - 152, 14 + 48 + 4);
            statusMenu.toFront(); filterOverlay.setViewOrder(-1);
        } else filterOverlay.setViewOrder(0);
    }

    @FXML private void selectStatus(javafx.event.ActionEvent event) {
        Button option = (Button) event.getSource();
        statusTrigger.setText(option.getText() + "     ▾");
        statusMenu.setVisible(false); statusMenu.setManaged(false); filterOverlay.setViewOrder(0);
    }

    private static String cover(String isbn) { return "https://covers.openlibrary.org/b/isbn/" + isbn + "-M.jpg?default=false"; }
}
