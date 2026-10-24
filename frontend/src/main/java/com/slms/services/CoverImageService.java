package com.slms.services;

import com.slms.models.Book;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Shared, bounded cover loader with memory and local disk caching. */
public final class CoverImageService {
    private static final Path CACHE_DIR = Path.of(
        System.getProperty("user.home"), ".slms", "cover-cache"
    );
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(4))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
    private static final ExecutorService DOWNLOADS = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "slms-cover-download");
        thread.setDaemon(true);
        return thread;
    });
    private static final ConcurrentHashMap<String, Image> IMAGES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CompletableFuture<Image>> PENDING = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> MISSING_UNTIL = new ConcurrentHashMap<>();
    private static final long MISSING_TTL_MS = Duration.ofMinutes(15).toMillis();

    private CoverImageService() { }

    /** Loads an ISBN cover without blocking the JavaFX application thread. */
    public static void load(Book book, ImageView target, double width, double height) {
        String isbn = book == null || book.getIsbn() == null
            ? "" : book.getIsbn().replaceAll("[^0-9Xx]", "").toUpperCase(Locale.ROOT);
        if (isbn.isBlank()) return;

        String key = isbn + "-" + (int) width + "x" + (int) height;
        Image cached = IMAGES.get(key);
        if (cached != null) {
            target.setImage(cached);
            target.setVisible(true);
            return;
        }
        if (MISSING_UNTIL.getOrDefault(key, 0L) > System.currentTimeMillis()) return;

        CompletableFuture<Image> future = PENDING.computeIfAbsent(key, ignored ->
            CompletableFuture.supplyAsync(() -> fetch(isbn, key, width, height), DOWNLOADS)
                .whenComplete((image, error) -> PENDING.remove(key))
        );
        future.thenAccept(image -> Platform.runLater(() -> {
            if (image != null && target.getScene() != null) {
                target.setImage(image);
                target.setVisible(true);
            }
        }));
    }

    private static Image fetch(String isbn, String key, double width, double height) {
        Path cacheFile = CACHE_DIR.resolve(key + ".img");
        try {
            if (Files.isRegularFile(cacheFile)) {
                Image cached = decode(Files.readAllBytes(cacheFile), width, height);
                if (cached != null) {
                    IMAGES.put(key, cached);
                    return cached;
                }
                Files.deleteIfExists(cacheFile);
            }

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://covers.openlibrary.org/b/isbn/" + isbn + "-M.jpg?default=false"))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "SLMS/1.0 (desktop library catalogue)")
                .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                .GET()
                .build();
            HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (response.statusCode() < 200 || response.statusCode() >= 300
                || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                markMissing(key);
                return null;
            }

            byte[] bytes = response.body();
            Image image = decode(bytes, width, height);
            if (image == null) {
                markMissing(key);
                return null;
            }
            IMAGES.put(key, image);
            try {
                Files.createDirectories(CACHE_DIR);
                Files.write(cacheFile, bytes);
            } catch (Exception ignored) {
                // A read-only cache must not prevent showing a fetched cover.
            }
            return image;
        } catch (Exception ignored) {
            markMissing(key);
            return null;
        }
    }

    private static Image decode(byte[] bytes, double width, double height) {
        try {
            Image image = new Image(new ByteArrayInputStream(bytes), width, height, false, true);
            return image.isError() ? null : image;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void markMissing(String key) {
        MISSING_UNTIL.put(key, System.currentTimeMillis() + MISSING_TTL_MS);
    }
}
