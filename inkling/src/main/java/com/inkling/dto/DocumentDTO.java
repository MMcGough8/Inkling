package com.inkling.dto;

import com.inkling.model.Document;
import java.time.LocalDateTime;

public class DocumentDTO {

    public Long id;
    public String name;
    public String contentType;
    public Long size;
    public LocalDateTime uploadedAt;
    public String status;
    public int chunkCount;

    // Default constructor needed for JSON deserialization
    public DocumentDTO() {}

    // Factory method to convert entity to DTO
    public static DocumentDTO from(Document doc) {
        DocumentDTO dto = new DocumentDTO();
        dto.id = doc.id;
        dto.name = doc.name;
        dto.contentType = doc.contentType;
        dto.size = doc.size;
        dto.uploadedAt = doc.uploadedAt;
        dto.status = doc.status != null ? doc.status.name() : null;
        dto.chunkCount = doc.chunks != null ? doc.chunks.size() : 0;
        return dto;
    }
}
