package com.inkling;

import com.inkling.dto.ChatRequest;
import com.inkling.dto.ChatResponse;
import com.inkling.exception.ValidationException;
import com.inkling.service.RAGService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/chat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChatResource {

    @Inject
    RAGService ragService;

    /**
     * Ask a question and get an answer grounded in uploaded documents.
     *
     * Example:
     * curl -X POST http://localhost:8080/api/chat \
     *   -H "Content-Type: application/json" \
     *   -d '{"question": "What are the key findings?"}'
     */
    @POST
    public ChatResponse chat(ChatRequest request) {
        if (request == null || request.question == null || request.question.isBlank()) {
            throw new ValidationException("Question is required");
        }

        return ragService.chat(request);
    }
}
