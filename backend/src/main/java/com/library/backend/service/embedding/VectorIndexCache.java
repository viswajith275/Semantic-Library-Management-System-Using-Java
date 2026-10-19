package com.library.backend.service.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.backend.entity.Book;
import com.library.backend.repository.BookRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VectorIndexCache {

    private static final Logger log = LoggerFactory.getLogger(VectorIndexCache.class);

    private final BookRepository bookRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, float[]> index = new ConcurrentHashMap<>();

    public record ScoredIsbn(String isbn, double score) {}

    public VectorIndexCache(BookRepository bookRepository, ObjectMapper objectMapper) {
        this.bookRepository = bookRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        // shortcut: loads every embedding into memory at startup; fine for a branch-sized catalogue,
        // load lazily or use a real vector store when the catalogue outgrows heap.
        log.info("Initializing VectorIndexCache from database...");
        List<Book> books = bookRepository.findAll();
        int loaded = 0;
        for (Book book : books) {
            if (book.getEmbeddingJson() != null && !book.getEmbeddingJson().isBlank()) {
                try {
                    float[] vector = objectMapper.readValue(book.getEmbeddingJson(), float[].class);
                    index.put(book.getIsbn(), vector);
                    loaded++;
                } catch (Exception e) {
                    log.error("Failed to parse embedding for ISBN {}: {}", book.getIsbn(), e.getMessage());
                }
            }
        }
        log.info("Loaded {} book vectors into in-memory index.", loaded);
    }

    public void put(String isbn, float[] vector) {
        if (isbn != null && vector != null) {
            index.put(isbn, vector);
        }
    }

    public void remove(String isbn) {
        if (isbn != null) {
            index.remove(isbn);
        }
    }

    public float[] getVector(String isbn) {
        return isbn != null ? index.get(isbn) : null;
    }

    // shortcut: linear scan over every book, fine for a branch-sized catalogue; move to an ANN index
    // (or a SQL vector extension) when the catalogue outgrows memory.
    public List<ScoredIsbn> searchTopK(float[] queryVector, int topK) {
        return searchTopK(queryVector, topK, Collections.emptySet());
    }

    public List<ScoredIsbn> searchTopK(float[] queryVector, int topK, Set<String> exclude) {
        if (queryVector == null || topK <= 0 || index.isEmpty()) {
            return Collections.emptyList();
        }

        List<ScoredIsbn> scored = new ArrayList<>(index.size());
        int mismatched = 0;
        for (Map.Entry<String, float[]> entry : index.entrySet()) {
            if (exclude.contains(entry.getKey())) {
                continue;
            }
            if (entry.getValue().length != queryVector.length) {
                mismatched++;
                continue;
            }
            double score = cosineSimilarity(queryVector, entry.getValue());
            if (score > 0.001) {
                scored.add(new ScoredIsbn(entry.getKey(), score));
            }
        }
        if (mismatched > 0) {
            log.warn("Skipped {} book vectors whose dimension differs from the query ({}). "
                + "Re-embed the catalogue after changing the embedding model.", mismatched, queryVector.length);
        }

        return scored.stream()
            .sorted(Comparator.comparingDouble(ScoredIsbn::score).reversed())
            .limit(topK)
            .toList();
    }

    public static double cosineSimilarity(float[] v1, float[] v2) {
        if (v1 == null || v2 == null || v1.length != v2.length) {
            return 0.0;
        }
        double dot = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
        }
        return dot;
    }

    public String serializeVector(float[] vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize embedding vector to JSON", e);
        }
    }
}
