package com.slms.models;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * UI representation of a library member returned by {@code GET /api/admin/members}.
 *
 * <p>Access codes are account credentials, so they are only surfaced to administrators on
 * the member directory page and are never persisted by this model beyond the current load.</p>
 */
public class Member {

  private long id;
  private String name;
  private String email;
  private String customCode;
  private boolean active;
  private long activeBorrowedCount;

  public static Member from(JsonNode node) {
    Member member = new Member();
    member.id = node.path("id").asLong();
    member.name = text(node, "name");
    member.email = text(node, "email");
    member.customCode = text(node, "customCode");
    member.active = node.path("active").asBoolean(true);
    member.activeBorrowedCount = node.path("activeBorrowedCount").asLong();
    return member;
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.path(field);
    return value.isMissingNode() || value.isNull() ? "" : value.asText();
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getCustomCode() {
    return customCode;
  }

  public boolean isActive() {
    return active;
  }

  public long getActiveBorrowedCount() {
    return activeBorrowedCount;
  }

  public String getInitials() {
    if (name == null || name.isBlank()) return "?";
    String[] parts = name.trim().split("\\s+");
    if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
    return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
  }

  /** Client-side match used by the directory search field. */
  public boolean matches(String query) {
    if (query == null || query.isBlank()) return true;
    String needle = query.trim().toLowerCase();
    return contains(name, needle) || contains(email, needle) || contains(customCode, needle);
  }

  private static boolean contains(String value, String needle) {
    return value != null && value.toLowerCase().contains(needle);
  }
}
