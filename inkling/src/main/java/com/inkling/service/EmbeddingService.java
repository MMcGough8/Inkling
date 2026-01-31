package com.inkling.service;

import com.inkling.model.Document;
import com.inkling.model.Document.Status;
import com.inkling.model.DocumentChunk;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class EmbeddingService {

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Inject
    DocumentService documentService;

    @ConfigProperty(name = "inkling.chunk.size", defaultValue = "1000")
    int chunkSize;

    @ConfigProperty(name = "inkling.chunk.overlap", defaultValue = "200")
    int chunkOverlap;

    /**
     * Process a document: chunk the text, generate embeddings, and store everything.
     */
    @Transactional
    public void processDocument(Document document, String extractedText) {
        try {
            document.status = Status.PROCESSING;

            // Split text into chunks
            List<String> chunks = chunkText(extractedText);

            // Process each chunk
            for (int i = 0; i < chunks.size(); i++) {
                String chunkContent = chunks.get(i);

                // Generate embedding via OpenAI
                Embedding embedding = embeddingModel.embed(chunkContent).content();

                // Store in pgvector, get back the ID
                TextSegment segment = TextSegment.from(chunkContent);
                String embeddingId = embeddingStore.add(embedding, segment);

                // Create and persist the chunk entity
                DocumentChunk chunk = new DocumentChunk();
                chunk.document = document;
                chunk.content = chunkContent;
                chunk.chunkIndex = i;
                chunk.embeddingId = embeddingId;
                chunk.persist();

                // Keep both sides of relationship in sync
                document.chunks.add(chunk);
            }

            document.status = Status.READY;

        } catch (Exception e) {
            document.status = Status.FAILED;
            throw new RuntimeException("Failed to process document: " + document.name, e);
        }
    }

    /**
     * Split text into overlapping chunks.
     *
     * Example with size=100, overlap=20:
     * Text: [-------- 100 chars --------][-------- 100 chars --------]
     * Chunk 1: [-------- 100 chars --------]
     * Chunk 2:                        [-------- 100 chars --------]
     *                                 ↑ 20 char overlap
     */
    List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            // Calculate end position
            int end = Math.min(start + chunkSize, text.length());

            // Extract the chunk
            String chunk = text.substring(start, end);
            chunks.add(chunk);

            // Move start position (size minus overlap)
            start += chunkSize - chunkOverlap;

            // Safety: ensure we make progress even if overlap >= size
            if (chunkSize <= chunkOverlap) {
                start = end;
            }
        }

        return chunks;
    }

    /**
     * Delete all embeddings for a document (called before document deletion).
     */
    public void deleteEmbeddings(Document document) {
        for (DocumentChunk chunk : document.chunks) {
            if (chunk.embeddingId != null) {
                embeddingStore.remove(chunk.embeddingId);
            }
        }
    }
}
