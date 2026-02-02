package com.inkling.service;

import com.inkling.model.Document;
import com.inkling.model.Document.Status;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class DocumentServiceTest {

    @Inject
    DocumentService documentService;

    @AfterEach
    @Transactional
    void cleanup() {
        Document.deleteAll();
    }

    @Test
    void processUpload_shouldExtractTextFromPlainText() throws Exception {
        // Given
        String content = "This is a test document with some content.";
        InputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        // When
        DocumentService.DocumentParseResult result = documentService.processUpload(
                "test.txt",
                "text/plain",
                content.length(),
                stream
        );

        // Then
        assertNotNull(result.document());
        assertNotNull(result.document().id);
        assertEquals("test.txt", result.document().name);
        assertEquals("text/plain", result.document().contentType);
        assertEquals(Status.PENDING, result.document().status);
        assertNotNull(result.document().uploadedAt);

        // Extracted text should contain original content
        assertTrue(result.extractedText().contains("This is a test document"));
    }

    @Test
    void processUpload_shouldPersistDocument() throws Exception {
        // Given
        String content = "Persisted document content";
        InputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        // When
        DocumentService.DocumentParseResult result = documentService.processUpload(
                "persisted.txt",
                "text/plain",
                content.length(),
                stream
        );

        // Then: should be findable by ID
        Document found = documentService.findById(result.document().id);
        assertNotNull(found);
        assertEquals("persisted.txt", found.name);
    }

    @Test
    void listAll_shouldReturnAllDocuments() throws Exception {
        // Given: create multiple documents
        for (int i = 0; i < 3; i++) {
            String content = "Document " + i;
            InputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            documentService.processUpload("doc" + i + ".txt", "text/plain", content.length(), stream);
        }

        // When
        List<Document> documents = documentService.listAll();

        // Then
        assertEquals(3, documents.size());
    }

    @Test
    void findById_shouldReturnNullForNonExistent() {
        // When
        Document found = documentService.findById(99999L);

        // Then
        assertNull(found);
    }

    @Test
    void delete_shouldRemoveDocument() throws Exception {
        // Given
        String content = "To be deleted";
        InputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        DocumentService.DocumentParseResult result = documentService.processUpload(
                "delete-me.txt", "text/plain", content.length(), stream
        );
        Long id = result.document().id;

        // When
        boolean deleted = documentService.delete(id);

        // Then
        assertTrue(deleted);
        assertNull(documentService.findById(id));
    }

    @Test
    void delete_shouldReturnFalseForNonExistent() {
        // When
        boolean deleted = documentService.delete(99999L);

        // Then
        assertFalse(deleted);
    }

    @Test
    void updateStatus_shouldChangeStatus() throws Exception {
        // Given
        String content = "Status test";
        InputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        DocumentService.DocumentParseResult result = documentService.processUpload(
                "status.txt", "text/plain", content.length(), stream
        );
        assertEquals(Status.PENDING, result.document().status);

        // When
        documentService.updateStatus(result.document().id, Status.READY);

        // Then
        Document updated = documentService.findById(result.document().id);
        assertEquals(Status.READY, updated.status);
    }
}
