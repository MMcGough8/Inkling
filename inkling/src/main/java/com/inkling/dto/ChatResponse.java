package com.inkling.dto;

import java.util.List;

public class ChatResponse {

    public String answer;
    public List<SourceReference> sources;
    public String sessionId;

    public ChatResponse() {}

    public static ChatResponse of(String answer, List<SourceReference> sources, String sessionId) {
        ChatResponse response = new ChatResponse();
        response.answer = answer;
        response.sources = sources;
        response.sessionId = sessionId;
        return response;
    }
}
