package com.inkling.dto;

public class SourceReference {

    public Long documentId;
    public String documentName;
    public int chunkIndex;
    public String snippet;      // Preview of the matched content
    public double score;        // Similarity score from vector search

    public SourceReference() {}

    public static SourceReference of(Long documentId, String documentName,
                                      int chunkIndex, String snippet, double score) {
        SourceReference ref = new SourceReference();
        ref.documentId = documentId;
        ref.documentName = documentName;
        ref.chunkIndex = chunkIndex;
        ref.snippet = snippet;
        ref.score = score;
        return ref;
    }
}
