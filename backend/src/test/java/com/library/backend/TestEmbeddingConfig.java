package com.library.backend;

import com.library.backend.service.embedding.EmbeddingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Locale;

/**
 * Test double so no test needs a running Ollama.
 * Plain @Configuration (not @TestConfiguration) on purpose: it must be picked up by
 * component scanning in every @SpringBootTest, and @Primary makes it win over
 * OllamaEmbeddingService. Deterministic 256-dim word hashing, enough for ranking assertions.
 */
@Configuration
public class TestEmbeddingConfig {

    @Bean
    @Primary
    EmbeddingService testEmbeddingService() {
        return text -> {
            float[] vector = new float[256];
            if (text != null) {
                for (String word : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
                    if (!word.isEmpty()) {
                        vector[(word.hashCode() & 0x7fffffff) % 256] += 1.0f;
                    }
                }
            }
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
        };
    }
}
