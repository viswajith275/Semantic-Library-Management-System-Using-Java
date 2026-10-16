package com.slms.controllers;

import com.slms.models.Book;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Detail page for a selected catalogue book. */
final class BookDetailView {
    private BookDetailView() { }

    static Node create(Book book, boolean librarian) {
        VBox page = new VBox(22);
        page.getStyleClass().add("detail-page");
        page.setPadding(new Insets(36, 7, 32, 7));
        page.setMaxWidth(1160);

        Button back = new Button("←   Back to catalogue");
        back.getStyleClass().add("detail-back-button");
        back.setCursor(Cursor.HAND);
        back.setOnAction(event -> NavigationController.showCatalogue());

        HBox content = new HBox(28);
        content.setAlignment(Pos.TOP_LEFT);
        VBox left = new VBox(14);
        left.setMinWidth(300);
        left.setPrefWidth(300);
        left.setMaxWidth(300);
        StackPane cover = largeCover(book);
        Button mainAction = new Button(librarian ? "Edit this book" : "⇄   Borrow this book");
        mainAction.getStyleClass().add("detail-primary-button");
        mainAction.setMaxWidth(Double.MAX_VALUE);
        mainAction.setCursor(Cursor.HAND);
        mainAction.setOnAction(event -> { });
        VBox copies = new VBox(9);
        copies.getStyleClass().add("detail-copies-card");
        HBox copyLine = new HBox();
        Label copiesLabel = new Label("Copies on shelf");
        Label copyCount = new Label(book.getAvailableCopies() + " / " + book.getTotalCopies());
        copyCount.getStyleClass().add("detail-copy-count");
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        copyLine.getChildren().addAll(copiesLabel, gap, copyCount);
        Region progress = new Region();
        progress.getStyleClass().add("detail-stock-progress");
        double ratio = book.getTotalCopies() == 0 ? 0 : (double) book.getAvailableCopies() / book.getTotalCopies();
        progress.setMaxWidth(Math.max(0, (300 - 34) * ratio));
        StackPane progressTrack = new StackPane(progress);
        progressTrack.setAlignment(Pos.CENTER_LEFT);
        progressTrack.getStyleClass().add("detail-stock-track");
        copies.getChildren().addAll(copyLine, progressTrack);
        left.getChildren().addAll(cover, mainAction, copies);

        VBox right = new VBox(15);
        right.setMinWidth(0);
        HBox badges = new HBox(8);
        Label genre = new Label(book.getGenre()); genre.getStyleClass().add("detail-genre");
        Label status = new Label(book.getAvailableCopies() > 0 ? "Available" : "Checked out");
        status.getStyleClass().add(book.getAvailableCopies() > 0 ? "detail-available" : "detail-unavailable");
        Label rating = new Label("★  4.7"); rating.getStyleClass().add("detail-rating");
        badges.getChildren().addAll(genre, status, rating);
        Label title = new Label(book.getTitle()); title.getStyleClass().add("detail-title"); title.setWrapText(true);
        Label author = new Label("by " + book.getAuthor() + " · " + (book.getYear() < 2010 ? "Prentice Hall" : "Library Press") + " · " + book.getYear());
        author.getStyleClass().add("detail-author");
        Label descriptionHeading = heading("DESCRIPTION");
        Label description = new Label(descriptionFor(book)); description.getStyleClass().add("detail-description"); description.setWrapText(true);

        VBox information = new VBox(18);
        information.getStyleClass().add("detail-information");
        information.getChildren().add(heading("BOOK INFORMATION"));
        HBox row1 = new HBox(24, detailFact("▤", "Publisher", "Prentice Hall"), detailFact("#", "ISBN", book.getIsbn()), detailFact("▣", "Pages", "464"));
        HBox row2 = new HBox(24, detailFact("◎", "Language", "English"), detailFact("▱", "Publication year", Integer.toString(book.getYear())), detailFact("♧", "Shelf location", "CS-04 · Rack B"));
        row1.setFillHeight(true);
        row2.setFillHeight(true);
        information.getChildren().addAll(row1, row2);
        VBox topics = new VBox(11);
        topics.getChildren().add(heading("RELATED TOPICS IN THE KNOWLEDGE GRAPH"));
        HBox topicRow = new HBox(8);
        for (String topic : new String[]{book.getGenre(), "Refactoring", "Code Quality", "Design Principles"}) {
            Label chip = new Label(topic); chip.getStyleClass().add("detail-topic"); topicRow.getChildren().add(chip);
        }
        topics.getChildren().add(topicRow);
        VBox related = new VBox(12);
        related.getStyleClass().add("detail-related");
        related.getChildren().addAll(heading("SEMANTIC NEIGHBOURS"), new Label("Similar & recommended books"));
        related.getChildren().get(1).getStyleClass().add("detail-related-title");
        HBox neighborRow = new HBox(12);
        neighborRow.getChildren().addAll(neighbor("The Pragmatic Programmer", "Hunt & Thomas", "98% similar"), neighbor("Deep Learning", "Goodfellow, Bengio & Courville", "62% similar"));
        related.getChildren().add(neighborRow);
        right.getChildren().addAll(badges, title, author, descriptionHeading, description, information, topics, related);
        HBox.setHgrow(right, Priority.ALWAYS);
        content.getChildren().addAll(left, right);
        page.getChildren().addAll(back, content);
        StackPane canvas = new StackPane(page);
        canvas.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(canvas);
        scroll.setFitToWidth(true); scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("detail-scroll");
        StackPane wrapper = new StackPane(scroll);
        wrapper.getStyleClass().add("detail-wrapper");
        return wrapper;
    }

    private static StackPane largeCover(Book book) {
        StackPane pane = new StackPane(); pane.getStyleClass().add("detail-cover");
        pane.setMinHeight(400); pane.setPrefHeight(400); pane.setMaxHeight(400);
        pane.setStyle("-fx-background-color: linear-gradient(to bottom right, #16395f, #355bb2);");
        VBox fallback = new VBox(); fallback.setPadding(new Insets(16)); fallback.setAlignment(Pos.TOP_LEFT);
        Label title = new Label(book.getTitle()); title.getStyleClass().add("detail-cover-title"); title.setWrapText(true);
        Label author = new Label(book.getAuthor()); author.getStyleClass().add("detail-cover-author");
        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
        fallback.getChildren().addAll(title, spacer, author); pane.getChildren().add(fallback);
        try {
            Image image = new Image(book.getImageUrl(), 300, 400, false, true, true);
            ImageView view = new ImageView(image); view.setFitWidth(300); view.setFitHeight(400); view.setPreserveRatio(false);
            Rectangle clip = new Rectangle(300, 400); clip.setArcWidth(16); clip.setArcHeight(16); view.setClip(clip);
            image.errorProperty().addListener((obs, old, failed) -> view.setVisible(!failed)); pane.getChildren().add(view);
        } catch (IllegalArgumentException ignored) { }
        return pane;
    }

    private static VBox detailFact(String icon, String name, String value) {
        VBox fact = new VBox(4); HBox line = new HBox(8);
        Label symbol = new Label(icon); symbol.getStyleClass().add("detail-fact-icon");
        Label caption = new Label(name); caption.getStyleClass().add("detail-fact-label");
        line.getChildren().addAll(symbol, caption);
        Label content = new Label(value); content.getStyleClass().add("detail-fact-value");
        content.setWrapText(true);
        fact.setMinWidth(0);
        fact.getChildren().addAll(line, content); HBox.setHgrow(fact, Priority.ALWAYS); return fact;
    }

    private static Label heading(String text) { Label label = new Label(text); label.getStyleClass().add("detail-section-heading"); return label; }
    private static String descriptionFor(Book book) {
        if (book.getTitle().equals("Clean Code")) return "A handbook of agile software craftsmanship that argues code quality is a professional obligation. Martin walks through naming, functions, comments, formatting, objects, error handling and successive refinement using real refactoring case studies.";
        return book.getTitle() + " by " + book.getAuthor() + " explores the foundations, practical ideas and lasting influence of " + book.getGenre().toLowerCase() + ". A recommended title from the library catalogue.";
    }
    private static VBox neighbor(String title, String author, String similarity) {
        VBox card = new VBox(5); card.getStyleClass().add("detail-neighbor");
        Label name = new Label(title); name.getStyleClass().add("detail-neighbor-title"); name.setWrapText(true);
        Label by = new Label(author); by.getStyleClass().add("detail-neighbor-author"); by.setWrapText(true);
        Label score = new Label(similarity); score.getStyleClass().add("detail-neighbor-score");
        card.getChildren().addAll(name, by, score); HBox.setHgrow(card, Priority.ALWAYS); return card;
    }
}
