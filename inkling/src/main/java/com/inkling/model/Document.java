package com.inkling.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document extends PanacheEntity {

    @Column(nullable = false)
    public String name;

    @Column(name = "content_type")
    public String contentType;

    public Long size;

    @Column(name = "uploaded_at")
    public LocalDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    public Status status;

    public enum Status {
        PENDING,    // Just uploaded, not yet processed
        PROCESSING, // Currently being chunked/embedded
        READY,      // Fully processed, available for queries
        FAILED      // Processing failed
    }

    // Panache uses public fields - no getters/setters needed!
    // But we can add helper methods:

    public static Document findByName(String name) {
        return find("name", name).firstResult();
    }
}
