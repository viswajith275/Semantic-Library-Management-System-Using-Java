package com.slms.controllers;

import com.slms.models.Book;
import java.io.IOException;
import java.util.List;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Controller for the catalogue FXML. */
public class BookCatalogController {
    private static final List<Book> BOOKS = List.of(
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
    );
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

    @FXML private void initialize() {
        boolean librarian = "Librarian".equals(NavigationController.getCurrentRole());
        eyebrow.setText(librarian ? "CATALOGUE MANAGEMENT" : "BROWSE THE SHELF");
        subtitle.setText(librarian
            ? "Add, edit and retire catalogue records. 12,486 titles indexed across 1,204 concepts."
            : "Search the shelf in plain language, filter by genre or author, and borrow in one tap.");
        addBook.setVisible(librarian); addBook.setManaged(librarian);
        borrowings.setVisible(!librarian); borrowings.setManaged(!librarian);
        for (int i = 0; i < BOOKS.size(); i++) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/components/BookCard.fxml"));
                bookGrid.add(loader.load(), i % 4, i / 4);
                loader.<BookCardController>getController().configure(BOOKS.get(i), i, librarian, COVER_COLORS[i % COVER_COLORS.length]);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not load a catalogue card", exception);
            }
        }
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
