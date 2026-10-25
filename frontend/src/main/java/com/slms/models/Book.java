package com.slms.models;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/** Backend book record with UI derived availability. */
public class Book {

  /** One physical copy, including the barcode used at the circulation desk. */
  public record Copy(String barcode, String status) {
    public static Copy from(JsonNode node) {
      return new Copy(text(node, "barcode"), text(node, "status"));
    }

    private static String text(JsonNode node, String field) {
      JsonNode value = node.path(field);
      return value.isMissingNode() || value.isNull() ? "" : value.asText();
    }
  }

  private final String title, author, isbn, coverColor, summary, characters, shelfLocation, availabilityStatus;
  private final int totalCopies, availableCopies;
  private final Double similarityScore;
  private final List<Copy> copies;

  public Book(
    String title,
    String author,
    String isbn,
    String coverColor,
    String summary,
    String characters,
    String shelfLocation,
    int totalCopies,
    int availableCopies,
    String availabilityStatus,
    Double similarityScore,
    List<Copy> copies
  ) {
    this.title = title;
    this.author = author;
    this.isbn = isbn;
    this.coverColor = coverColor;
    this.summary = summary;
    this.characters = characters;
    this.shelfLocation = shelfLocation;
    this.totalCopies = totalCopies;
    this.availableCopies = availableCopies;
    this.availabilityStatus = availabilityStatus;
    this.similarityScore = similarityScore;
    this.copies = copies == null ? List.of() : List.copyOf(copies);
  }

  public static Book from(JsonNode n) {
    List<Copy> copies = new ArrayList<>();
    JsonNode copiesNode = n.path("copies");
    if (copiesNode.isArray()) copiesNode.forEach(copy ->
      copies.add(Copy.from(copy))
    );
    int available = n.path("availableCopies").asInt();
    int total = n.path("totalCopies").asInt();
    Double score = n.path("similarityScore").isNumber()
      ? n.path("similarityScore").asDouble()
      : null;
    return new Book(
      s(n, "title"),
      s(n, "author"),
      s(n, "isbn"),
      s(n, "coverColor"),
      s(n, "summary"),
      s(n, "characters"),
      s(n, "shelfLocation"),
      total,
      available,
      s(n, "availabilityStatus"),
      score,
      copies
    );
  }

  private static String s(JsonNode n, String key) {
    return n.path(key).isNull() ? "" : n.path(key).asText("");
  }

  public String getTitle() {
    return title;
  }

  public String getAuthor() {
    return author;
  }

  public String getIsbn() {
    return isbn;
  }

  public String getCoverColor() {
    return coverColor;
  }

  public String getSummary() {
    return summary;
  }

  public String getCharacters() {
    return characters;
  }

  public String getShelfLocation() {
    return shelfLocation;
  }

  public int getTotalCopies() {
    return totalCopies;
  }

  public int getAvailableCopies() {
    return availableCopies;
  }

  public String getAvailabilityStatus() {
    return availabilityStatus;
  }

  public Double getSimilarityScore() {
    return similarityScore;
  }

  public List<Copy> getCopies() {
    return copies;
  }

  public int getYear() {
    return 0;
  }

  public String getImageUrl() {
    return (
      "https://covers.openlibrary.org/b/isbn/" +
      isbn.replaceAll("[^0-9Xx]", "") +
      "-M.jpg?default=false"
    );
  }

  public String getGenre() {
    return shelfLocation == null || shelfLocation.isBlank()
      ? "Location unavailable"
      : shelfLocation;
  }
}
