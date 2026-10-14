package com.slms.models;

/** Book details used by the mock catalogue until the backend is connected. */
public class Book {
    private final String title;
    private final String author;
    private final String genre;
    private final String isbn;
    private final int year;
    private final int totalCopies;
    private final int availableCopies;
    private final String imageUrl;

    public Book(String title, String author, String genre, String isbn, int year,
                int totalCopies, int availableCopies, String imageUrl) {
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.isbn = isbn;
        this.year = year;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        this.imageUrl = imageUrl;
    }

    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getGenre() { return genre; }
    public String getIsbn() { return isbn; }
    public int getYear() { return year; }
    public int getTotalCopies() { return totalCopies; }
    public int getAvailableCopies() { return availableCopies; }
    public String getImageUrl() { return imageUrl; }
}
