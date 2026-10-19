package com.library.backend.service.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.backend.exception.EmbeddingUnavailableException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Ollama-backed embeddings. There is deliberately no local fallback: if Ollama is not
 * reachable the call fails (HTTP 503) instead of silently producing incomparable vectors.
 */
@Service
public class OllamaEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.embedding.ollama.url:http://localhost:11434/api/embeddings}")
    private String url;

    @Value("${app.embedding.ollama.model:nomic-embed-text}")
    private String model;

    @Value("${app.embedding.ollama.verify-on-startup:true}")
    private boolean verifyOnStartup;

    public OllamaEmbeddingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    @PostConstruct
    void verifyOllamaAvailable() {
        if (!verifyOnStartup) {
            return;
        }
        try {
            embed("ping");
            log.info("Ollama embedding model '{}' reachable at {}", model, url);
        } catch (EmbeddingUnavailableException e) {
            throw new IllegalStateException("Ollama is not available. " + e.getMessage()
                + " Start it with 'ollama serve' and pull the model with 'ollama pull " + model + "'.", e);
        }
    }

    @Override
    public float[] generateEmbedding(String text) {
        return embed(text == null ? "" : text);
    }

    private float[] embed(String text) {
        String responseBody;
        try {
            responseBody = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", model, "prompt", text))
                .retrieve()
                .body(String.class);
        } catch (Exception e) {
            throw new EmbeddingUnavailableException(
                "Could not reach Ollama at " + url + " (model '" + model + "'): " + e.getMessage(), e);
        }
        return l2Normalize(parseEmbedding(responseBody));
    }

    private float[] parseEmbedding(String responseBody) {
        JsonNode node;
        try {
            node = responseBody == null ? null : objectMapper.readTree(responseBody).get("embedding");
        } catch (Exception e) {
            throw new EmbeddingUnavailableException("Could not parse Ollama response: " + e.getMessage(), e);
        }
        if (node == null || !node.isArray() || node.isEmpty()) {
            throw new EmbeddingUnavailableException(
                "Ollama returned no embedding (is model '" + model + "' pulled?). Response: " + responseBody);
        }
        float[] vector = new float[node.size()];
        for (int i = 0; i < node.size(); i++) {
            vector[i] = (float) node.get(i).asDouble();
        }
        return vector;
    }

    private static float[] l2Normalize(float[] vector) {
        double sumSquares = 0.0;
        for (float value : vector) {
            sumSquares += value * (double) value;
        }
        if (sumSquares > 0.0) {
            float norm = (float) Math.sqrt(sumSquares);
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }
}
