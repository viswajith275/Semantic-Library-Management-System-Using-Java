package com.slms.controllers;

import com.slms.models.Book;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.Cursor;

/** Builds the two role-specific, sample-data catalogue screens. */
final class BookCatalogView {
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

    private BookCatalogView() { }

    static Node librarian() { return build(true); }
    static Node user() { return build(false); }

    private static String cover(String isbn) {
        return "https://covers.openlibrary.org/b/isbn/" + isbn + "-M.jpg?default=false";
    }

    private static Node build(boolean librarian) {
        VBox page = new VBox(20);
        page.getStyleClass().add("catalog-page");
        page.setPadding(new Insets(30, 32, 30, 32));

        HBox heading = new HBox(16);
        heading.setAlignment(Pos.BOTTOM_LEFT);
        VBox headingCopy = new VBox(6);
        Label eyebrow = new Label(librarian ? "CATALOGUE MANAGEMENT" : "BROWSE THE SHELF");
        eyebrow.getStyleClass().add("catalog-eyebrow");
        Label title = new Label("Books");
        title.getStyleClass().add("catalog-title");
        Label subtitle = new Label(librarian
            ? "Add, edit and retire catalogue records. 12,486 titles indexed across 1,204 concepts."
            : "Search the shelf in plain language, filter by genre or author, and borrow in one tap.");
        subtitle.getStyleClass().add("catalog-subtitle");
        headingCopy.getChildren().addAll(eyebrow, title, subtitle);
        heading.getChildren().add(headingCopy);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        heading.getChildren().add(spacer);
        if (librarian) {
            Button addBook = new Button("＋  Add Book");
            addBook.getStyleClass().add("catalog-primary-button");
            heading.getChildren().add(addBook);
        } else {
            Button borrowings = new Button("⇄  My borrowings");
            borrowings.getStyleClass().add("catalog-secondary-button");
            heading.getChildren().add(borrowings);
        }

        StackPane filterPanel = filters();
        GridPane grid = new GridPane();
        grid.getStyleClass().add("book-grid");
        grid.setHgap(14);
        grid.setVgap(14);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(25);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }
        for (int i = 0; i < BOOKS.size(); i++) {
            grid.add(bookCard(BOOKS.get(i), i, librarian), i % 4, i / 4);
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("catalog-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        page.getChildren().addAll(heading, filterPanel, scroll);

        BorderPane wrapper = new BorderPane(page);
        wrapper.getStyleClass().add("catalog-wrapper");
        return wrapper;
    }

    private static StackPane filters() {
        StackPane overlay = new StackPane();
        overlay.setAlignment(Pos.TOP_LEFT);
        VBox panel = new VBox(8);
        panel.getStyleClass().add("catalog-filters");
        HBox row = new HBox(9);
        row.setAlignment(Pos.TOP_LEFT);
        TextField search = new TextField();
        search.setPromptText("⌕   Search books by title, author, ISBN or keyword...");
        search.getStyleClass().add("catalog-search");
        HBox.setHgrow(search, Priority.ALWAYS);
        row.getChildren().add(search);
        overlay.getChildren().add(panel);
        row.getChildren().add(statusFilter(overlay));
        Label count = new Label("▽  12 of 12 titles");
        count.getStyleClass().add("catalog-result-count");
        panel.getChildren().addAll(row, count);
        return overlay;
    }

    private static VBox statusFilter(StackPane overlay) {
        VBox dropdown = new VBox(4);
        dropdown.setMinWidth(152);
        dropdown.setPrefWidth(152);
        dropdown.setMaxWidth(152);
        dropdown.setAlignment(Pos.TOP_RIGHT);

        Button trigger = new Button("Any status     ▾");
        trigger.getStyleClass().add("catalog-status-trigger");
        trigger.setMinSize(152, 48);
        trigger.setPrefSize(152, 48);
        trigger.setMaxSize(152, 48);

        VBox menu = new VBox(3);
        menu.getStyleClass().add("catalog-status-menu");
        menu.setMinWidth(152);
        menu.setPrefWidth(152);
        menu.setVisible(false);
        menu.setManaged(false);
        menu.setPrefHeight(126);

        for (String value : List.of("Any status", "Available", "Sold out")) {
            Button option = new Button(value);
            option.getStyleClass().add("catalog-status-option");
            option.setMaxWidth(Double.MAX_VALUE);
            option.setOnAction(event -> {
                trigger.setText(value + "     ▾");
                menu.setVisible(false);
                overlay.setViewOrder(0);
            });
            menu.getChildren().add(option);
        }

        trigger.setOnAction(event -> {
            boolean open = !menu.isVisible();
            menu.setVisible(open);
            if (open) {
                menu.applyCss();
                menu.resize(152, 126);
                menu.layout();
                menu.relocate(overlay.getWidth() - 14 - 152, 14 + 48 + 4);
                menu.toFront();
                overlay.setViewOrder(-1);
            } else {
                overlay.setViewOrder(0);
            }
        });
        overlay.widthProperty().addListener((observable, oldWidth, newWidth) -> {
            if (menu.isVisible()) {
                menu.relocate(newWidth.doubleValue() - 14 - 152, 14 + 48 + 4);
            }
        });
        overlay.getChildren().add(menu);
        dropdown.getChildren().add(trigger);
        return dropdown;
    }

    private static VBox bookCard(Book book, int index, boolean librarian) {
        VBox card = new VBox();
        card.getStyleClass().add("book-card");
        VBox details = new VBox(10);
        details.setPadding(new Insets(14, 14, 12, 14));

        HBox top = new HBox(13);
        top.setAlignment(Pos.TOP_LEFT);
        StackPane cover = coverTile(book, index);
        VBox info = new VBox(5);
        HBox availability = new HBox();
        Label stock = new Label(book.getAvailableCopies() == 0 ? "All copies out" : book.getAvailableCopies() + " available");
        stock.getStyleClass().add(book.getAvailableCopies() == 0 ? "stock-out" : "stock-available");
        availability.getChildren().add(stock);
        Label name = new Label(book.getTitle());
        name.getStyleClass().add("book-title");
        name.setWrapText(true);
        Label author = new Label(book.getAuthor());
        author.getStyleClass().add("book-author");
        author.setWrapText(true);
        Label isbn = new Label(book.getIsbn());
        isbn.getStyleClass().add("book-isbn");
        info.getChildren().addAll(availability, name, author, isbn);
        HBox.setHgrow(info, Priority.ALWAYS);
        top.getChildren().addAll(cover, info);

        HBox tags = new HBox(6);
        Label genre = new Label(book.getGenre());
        genre.getStyleClass().add("book-tag");
        Label year = new Label(Integer.toString(book.getYear()));
        year.getStyleClass().add("book-tag");
        tags.getChildren().addAll(genre, year);
        if (librarian) {
            Label copies = new Label(book.getTotalCopies() + " copies");
            copies.getStyleClass().add("book-tag");
            tags.getChildren().add(copies);
        }
        details.getChildren().addAll(top, tags);

        HBox actions = new HBox(19);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(10, 14, 10, 14));
        actions.getStyleClass().add("book-card-actions");
        Button view = action("View", "book-action", "M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12Z M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z");
        view.setOnAction(event -> NavigationController.showBookDetails(book, librarian));
        actions.getChildren().add(view);
        if (librarian) {
            actions.getChildren().add(action("Edit", "book-action", "M12 20h9 M16.5 3.5a2.12 2.12 0 0 1 3 3L8 18l-4 1 1-4Z"));
            actions.getChildren().add(action("", "book-action-delete", "M3 6h18 M8 6V4h8v2m3 0-1 14H6L5 6m5 4v6m4-6v6"));
        } else if (book.getAvailableCopies() > 0) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            actions.getChildren().add(spacer);
            actions.getChildren().add(action("Borrow", "book-borrow-button", "M12 5v14m-7-7h14"));
        } else {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            actions.getChildren().add(spacer);
            actions.getChildren().add(action("I’m interested", "book-interest-button", "M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9m-8 13h4"));
        }
        card.getChildren().addAll(details, new Separator(), actions);
        return card;
    }

    private static StackPane coverTile(Book book, int index) {
        StackPane tile = new StackPane();
        tile.getStyleClass().add("book-cover");
        tile.setStyle("-fx-background-color: " + COVER_COLORS[index % COVER_COLORS.length] + ";");
        tile.setMinSize(78, 112);
        tile.setPrefSize(78, 112);
        tile.setMaxSize(78, 112);
        VBox fallback = new VBox(5);
        fallback.setAlignment(Pos.CENTER);
        fallback.setPadding(new Insets(6));
        Label title = new Label(book.getTitle());
        title.getStyleClass().add("cover-fallback-title");
        title.setWrapText(true);
        Label author = new Label(book.getAuthor());
        author.getStyleClass().add("cover-fallback-author");
        author.setWrapText(true);
        fallback.getChildren().addAll(title, author);
        tile.getChildren().add(fallback);
        try {
            Image image = new Image(book.getImageUrl(), 78, 112, false, true, true);
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(78);
            imageView.setFitHeight(112);
            imageView.setPreserveRatio(false);
            Rectangle clip = new Rectangle(78, 112);
            clip.setArcWidth(12);
            clip.setArcHeight(12);
            imageView.setClip(clip);
            image.errorProperty().addListener((obs, oldValue, failed) -> imageView.setVisible(!failed));
            tile.getChildren().add(imageView);
        } catch (IllegalArgumentException ignored) {
            // The styled title and author remain as a local cover fallback.
        }
        return tile;
    }

    private static Button action(String text, String style, String iconPath) {
        Button button = new Button(text);
        button.getStyleClass().add(style);
        SVGPath icon = new SVGPath();
        icon.setContent(iconPath);
        icon.getStyleClass().add("book-action-icon");
        button.setGraphic(icon);
        button.setGraphicTextGap(7);
        button.setAccessibleText(text.isBlank() ? "Delete book" : text);
        button.setCursor(Cursor.HAND);
        return button;
    }
}
