package com.inkling.exception;

/**
 * Thrown when a requested document does not exist.
 */
public class DocumentNotFoundException extends RuntimeException {

    private final Long documentId;

    public DocumentNotFoundException(Long documentId) {
        super("Document not found: " + documentId);
        this.documentId = documentId;
    }

    public Long getDocumentId() {
        return documentId;
    }
}
