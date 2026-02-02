package com.inkling.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class EmbeddingServiceTest {

    @Inject
    EmbeddingService embeddingService;

    @Test
    void chunkText_shouldSplitTextIntoChunks() {
        // Given: text longer than chunk size
        String text = "A".repeat(2500);

        // When: chunking with default settings (1000 size, 200 overlap)
        List<String> chunks = embeddingService.chunkText(text);

        // Then: should create multiple chunks
        assertTrue(chunks.size() > 1, "Should create multiple chunks");

        // First chunk should be full size
        assertEquals(1000, chunks.get(0).length());
    }

    @Test
    void chunkText_shouldCreateOverlap() {
        // Given: text that will create exactly 2 chunks
        String text = "A".repeat(500) + "B".repeat(500) + "C".repeat(300);
        // Total: 1300 chars. With size=1000, overlap=200:
        // Chunk 1: 0-1000 (AAAA...ABBB...)
        // Chunk 2: 800-1300 (BBB...CCC)

        // When
        List<String> chunks = embeddingService.chunkText(text);

        // Then: chunks should overlap
        assertEquals(2, chunks.size());

        // The overlap region (positions 800-1000) should appear in both chunks
        String endOfFirst = chunks.get(0).substring(800); // last 200 chars of chunk 1
        String startOfSecond = chunks.get(1).substring(0, 200); // first 200 chars of chunk 2
        assertEquals(endOfFirst, startOfSecond, "Chunks should overlap by 200 characters");
    }

    @Test
    void chunkText_shouldHandleShortText() {
        // Given: text shorter than chunk size
        String text = "Short text";

        // When
        List<String> chunks = embeddingService.chunkText(text);

        // Then: should return single chunk with original text
        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0));
    }

    @Test
    void chunkText_shouldHandleEmptyText() {
        // Given
        String text = "";

        // When
        List<String> chunks = embeddingService.chunkText(text);

        // Then
        assertTrue(chunks.isEmpty());
    }

    @Test
    void chunkText_shouldHandleNullText() {
        // Given
        String text = null;

        // When
        List<String> chunks = embeddingService.chunkText(text);

        // Then
        assertTrue(chunks.isEmpty());
    }

    @Test
    void chunkText_shouldHandleExactChunkSize() {
        // Given: text exactly chunk size (1000)
        // With overlap=200, step=800, so text of 1000 chars creates:
        // Chunk 1: 0-1000 (full chunk)
        // Chunk 2: 800-1000 (overlap portion)
        String text = "X".repeat(1000);

        // When
        List<String> chunks = embeddingService.chunkText(text);

        // Then: creates 2 chunks due to overlap algorithm
        assertEquals(2, chunks.size());
        assertEquals(1000, chunks.get(0).length());
        assertEquals(200, chunks.get(1).length()); // The overlap tail
    }

    @Test
    void chunkText_shouldReturnSingleChunkWhenTextFitsInStep() {
        // Given: text shorter than step size (chunkSize - overlap = 800)
        String text = "Y".repeat(800);

        // When
        List<String> chunks = embeddingService.chunkText(text);

        // Then: should return single chunk
        assertEquals(1, chunks.size());
        assertEquals(800, chunks.get(0).length());
    }

    @Test
    void chunkText_shouldPreserveAllContent() {
        // Given
        String text = "The quick brown fox jumps over the lazy dog. ".repeat(50);

        // When
        List<String> chunks = embeddingService.chunkText(text);

        // Then
        assertFalse(chunks.isEmpty());

        // First chunk should start with beginning of text
        assertTrue(text.startsWith(chunks.get(0)));

        // Last chunk should contain the end of the text
        String lastChunk = chunks.get(chunks.size() - 1);
        String textEnd = text.substring(text.length() - 20); // Last 20 chars
        assertTrue(lastChunk.contains(textEnd.trim()),
                "Last chunk should contain the end of the original text");
    }
}
