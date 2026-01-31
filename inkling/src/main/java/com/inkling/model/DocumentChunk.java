package com.inkling.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    public Document document;

    @Column(columnDefinition = "TEXT")
    public String content;

    @Column(name = "chunk_index")
    public Integer chunkIndex;

    @Column(name = "embedding_id")
    public String embeddingId;

    // Find all chunks for a document, ordered by index
    public static java.util.List<DocumentChunk> findByDocument(Document document) {
        return find("document = ?1 ORDER BY chunkIndex", document).list();
    }

    // Find chunk by its embedding ID (for RAG retrieval)
    public static DocumentChunk findByEmbeddingId(String embeddingId) {
        return find("embeddingId", embeddingId).firstResult();
    }
}
