package com.slms.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

// Small HTTP client helper service for requests.

// NOTE FOR YOU GAYS:
// USE THIS WHENVER A REQUEST HAS TO BE MADE.
public final class ApiClient {

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final HttpClient HTTP = HttpClient.newHttpClient();
  private static final String BASE_URL = System.getProperty(
    "slms.api",
    "http://localhost:8080"
  );

  private ApiClient() {}

  public static JsonNode request(
    String method,
    String path,
    String token,
    String code,
    JsonNode body
  ) throws Exception {
    HttpRequest.Builder builder = HttpRequest.newBuilder(
      URI.create(BASE_URL + path)
    ).header("Accept", "application/json");
    if (token != null && !token.isBlank()) builder.header(
      "Authorization",
      "Bearer " + token
    );
    if (code != null && !code.isBlank()) builder.header("X-Auth-Code", code);
    if (body != null) builder.header("Content-Type", "application/json");
    //prepares body
    HttpRequest.BodyPublisher publisher =
      body == null
        ? HttpRequest.BodyPublishers.noBody()
        : HttpRequest.BodyPublishers.ofString(
            JSON.writeValueAsString(body),
            StandardCharsets.UTF_8
          );

    HttpResponse<String> response = HTTP.send(
      builder.method(method, publisher).build(),
      HttpResponse.BodyHandlers.ofString()
    );
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      String message = response.body();
      try {
        message = JSON.readTree(response.body())
          .path("message")
          .asText(message);
      } catch (Exception ignored) {}
      throw new ApiException(response.statusCode(), message);
    }
    return response.body().isBlank()
      ? JSON.nullNode()
      : JSON.readTree(response.body());
  }

  public static ObjectNode object() {
    return JSON.createObjectNode();
  }

  public static JsonNode parse(String json) throws Exception {
    return JSON.readTree(json);
  }

  public static String message(Throwable error) {
    Throwable cause = error;
    while (cause.getCause() != null) cause = cause.getCause();
    return cause.getMessage() == null
      ? "Request failed. Check that the backend is running."
      : cause.getMessage();
  }

  public static final class ApiException extends Exception {

    private final int status;

    public ApiException(int status, String message) {
      super(message);
      this.status = status;
    }

    public int status() {
      return status;
    }
  }
}
