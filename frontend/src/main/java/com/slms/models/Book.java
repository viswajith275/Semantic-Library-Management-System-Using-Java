package com.slms.models;

import com.fasterxml.jackson.databind.JsonNode;

/** Backend book record with UI derived availability. */
public class Book {
    private final String title, author, isbn, coverColor, summary, characters, shelfLocation, availabilityStatus;
    private final int totalCopies, availableCopies;
    public Book(String title, String author, String isbn, String coverColor, String summary,
                String characters, String shelfLocation, int totalCopies, int availableCopies, String availabilityStatus) {
        this.title=title; this.author=author; this.isbn=isbn; this.coverColor=coverColor;
        this.summary=summary; this.characters=characters; this.shelfLocation=shelfLocation;
        this.totalCopies=totalCopies; this.availableCopies=availableCopies; this.availabilityStatus=availabilityStatus;
    }
    public static Book from(JsonNode n) {
        return new Book(s(n,"title"), s(n,"author"), s(n,"isbn"), s(n,"coverColor"), s(n,"summary"),
                s(n,"characters"), s(n,"shelfLocation"), n.path("totalCopies").asInt(),
                n.path("availableCopies").asInt(), s(n,"availabilityStatus"));
    }
    private static String s(JsonNode n,String key) { return n.path(key).isNull()?"":n.path(key).asText(""); }
    public String getTitle(){return title;} public String getAuthor(){return author;} public String getIsbn(){return isbn;}
    public String getCoverColor(){return coverColor;} public String getSummary(){return summary;} public String getCharacters(){return characters;}
    public String getShelfLocation(){return shelfLocation;} public int getTotalCopies(){return totalCopies;} public int getAvailableCopies(){return availableCopies;}
    public String getAvailabilityStatus(){return availabilityStatus;}
    public String getGenre(){ return coverColor == null || coverColor.isBlank() ? "Library title" : coverColor; }
    public int getYear(){ return 0; }
    public String getImageUrl(){ return "https://covers.openlibrary.org/b/isbn/"+isbn.replaceAll("[^0-9Xx]", "")+"-M.jpg?default=false"; }
}
