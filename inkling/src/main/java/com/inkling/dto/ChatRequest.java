package com.inkling.dto;

import java.util.List;

public class ChatRequest {

    public String question;

    // Optional: limit search to specific documents
    public List<Long> documentIds;

    // Optional: for conversation continuity
    public String sessionId;

    public ChatRequest() {}
}
