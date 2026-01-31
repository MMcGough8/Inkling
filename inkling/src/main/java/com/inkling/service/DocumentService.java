package com.inkling.service;

import com.inkling.model.Document;
import com.inkling.model.Document.Status;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class DocumentService {

    private final Tika tika = new Tika();

    /**
     * Process an uploaded file: extract text and create a Document entity.
     * Returns the parsed text content for further processing (chunking/embedding).
     */
    @Transactional
    public DocumentParseResult processUpload(String fileName, String contentType,
                                              long size, InputStream fileStream)
            throws IOException, TikaException {

        // Extract text using Apache Tika
        String extractedText = tika.parseToString(fileStream);

        // Create and persist the document
        Document doc = new Document();
        doc.name = fileName;
        doc.contentType = contentType;
        doc.size = size;
        doc.uploadedAt = LocalDateTime.now();
        doc.status = Status.PENDING;
        doc.persist();

        return new DocumentParseResult(doc, extractedText);
    }

    /**
     * Update document status (called after chunking/embedding completes or fails).
     */
    @Transactional
    public void updateStatus(Long documentId, Status status) {
        Document doc = Document.findById(documentId);
        if (doc != null) {
            doc.status = status;
        }
    }

    /**
     * Get all documents, ordered by upload date (newest first).
     */
    public List<Document> listAll() {
        return Document.find("ORDER BY uploadedAt DESC").list();
    }

    /**
     * Get a document by ID.
     */
    public Document findById(Long id) {
        return Document.findById(id);
    }

    /**
     * Delete a document and all its chunks (cascade handles chunks).
     */
    @Transactional
    public boolean delete(Long id) {
        Document doc = Document.findById(id);
        if (doc != null) {
            doc.delete();
            return true;
        }
        return false;
    }

    /**
     * Result of parsing a document - contains both the entity and extracted text.
     */
    public record DocumentParseResult(Document document, String extractedText) {}
}
