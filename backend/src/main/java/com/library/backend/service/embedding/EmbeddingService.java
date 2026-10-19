package com.library.backend.service.embedding;

public interface EmbeddingService {

    float[] generateEmbedding(String text);

    default String buildEmbeddingText(String title, String author, String coverColor, String summary, String characters) {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isBlank()) {
            sb.append(title.trim()).append(" ");
        }
        if (author != null && !author.isBlank()) {
            sb.append(author.trim()).append(" ");
        }
        if (coverColor != null && !coverColor.isBlank()) {
            sb.append(coverColor.trim()).append(" ");
        }
        if (summary != null && !summary.isBlank()) {
            sb.append(summary.trim()).append(" ");
        }
        if (characters != null && !characters.isBlank()) {
            sb.append(characters.trim()).append(" ");
        }
        return sb.toString().trim().toLowerCase();
    }
}
