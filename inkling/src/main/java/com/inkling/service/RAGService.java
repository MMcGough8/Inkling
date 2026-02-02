package com.inkling.service;

import com.inkling.dto.ChatRequest;
import com.inkling.dto.ChatResponse;
import com.inkling.dto.SourceReference;
import com.inkling.model.ChatMessage;
import com.inkling.model.ChatSession;
import com.inkling.model.DocumentChunk;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RAGService {

    private static final int MAX_RESULTS = 5;
    private static final double MIN_SCORE = 0.7;
    private static final int SNIPPET_LENGTH = 200;
    private static final int MAX_HISTORY_MESSAGES = 10; // Last 5 exchanges (10 messages)

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Inject
    ChatLanguageModel chatModel;

    /**
     * Answer a question using RAG with conversation memory.
     */
    @Transactional
    public ChatResponse chat(ChatRequest request) {
        // Step 1: Get or create session
        String sessionId = request.sessionId != null
                ? request.sessionId
                : "sess_" + UUID.randomUUID().toString().substring(0, 8);

        ChatSession session = getOrCreateSession(sessionId);

        // Step 2: Embed the question
        Embedding questionEmbedding = embeddingModel.embed(request.question).content();

        // Step 3: Search for similar chunks in pgvector
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(questionEmbedding)
                .maxResults(MAX_RESULTS)
                .minScore(MIN_SCORE)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

        // Step 4: Build context from matched chunks
        StringBuilder contextBuilder = new StringBuilder();
        List<SourceReference> sources = new ArrayList<>();

        for (EmbeddingMatch<TextSegment> match : matches) {
            String embeddingId = match.embeddingId();
            String content = match.embedded().text();
            double score = match.score();

            DocumentChunk chunk = DocumentChunk.findByEmbeddingId(embeddingId);
            if (chunk != null) {
                contextBuilder.append("---\n");
                contextBuilder.append("Source: ").append(chunk.document.name);
                contextBuilder.append(" (Section ").append(chunk.chunkIndex + 1).append(")\n");
                contextBuilder.append(content).append("\n\n");

                sources.add(SourceReference.of(
                        chunk.document.id,
                        chunk.document.name,
                        chunk.chunkIndex,
                        truncate(content, SNIPPET_LENGTH),
                        score
                ));
            }
        }

        // Step 5: Build the prompt with conversation history
        String conversationHistory = buildConversationHistory(session);
        String prompt = buildPrompt(request.question, contextBuilder.toString(), conversationHistory);

        // Step 6: Get LLM response
        String answer = chatModel.generate(prompt);

        // Step 7: Save the conversation
        session.addMessage(ChatMessage.userMessage(request.question));
        session.addMessage(ChatMessage.assistantMessage(answer));

        return ChatResponse.of(answer, sources, sessionId);
    }

    /**
     * Get existing session or create a new one.
     */
    private ChatSession getOrCreateSession(String sessionId) {
        ChatSession session = ChatSession.findBySessionId(sessionId);
        if (session == null) {
            session = new ChatSession();
            session.sessionId = sessionId;
            session.createdAt = LocalDateTime.now();
            session.lastActivity = LocalDateTime.now();
            session.persist();
        }
        return session;
    }

    /**
     * Build conversation history string from previous messages.
     */
    private String buildConversationHistory(ChatSession session) {
        List<ChatMessage> messages = session.messages;
        if (messages.isEmpty()) {
            return "";
        }

        // Take only the last N messages to avoid context overflow
        int startIndex = Math.max(0, messages.size() - MAX_HISTORY_MESSAGES);
        List<ChatMessage> recentMessages = messages.subList(startIndex, messages.size());

        StringBuilder history = new StringBuilder();
        for (ChatMessage msg : recentMessages) {
            String role = msg.role == ChatMessage.Role.USER ? "User" : "Assistant";
            history.append(role).append(": ").append(msg.content).append("\n\n");
        }

        return history.toString();
    }

    /**
     * Build the prompt with context, history, and question.
     */
    private String buildPrompt(String question, String context, String conversationHistory) {
        if (context.isBlank()) {
            if (conversationHistory.isBlank()) {
                return """
                    The user asked a question, but no relevant information was found in the documents.

                    Question: %s

                    Please let the user know that you couldn't find relevant information in their documents \
                    to answer this question. Suggest they upload relevant documents or rephrase their question.
                    """.formatted(question);
            } else {
                return """
                    You are a helpful assistant that answers questions based on provided documents.

                    No new relevant document excerpts were found for this question, but here is the conversation history:

                    CONVERSATION HISTORY:
                    %s

                    CURRENT QUESTION: %s

                    If you can answer based on the conversation history, do so. Otherwise, let the user know \
                    you couldn't find new relevant information.

                    ANSWER:
                    """.formatted(conversationHistory, question);
            }
        }

        if (conversationHistory.isBlank()) {
            return """
                You are a helpful assistant that answers questions based on the provided documents.

                Use ONLY the information from the following document excerpts to answer the question.
                If the answer cannot be found in the excerpts, say so - do not make up information.
                When possible, cite which document the information comes from.

                DOCUMENT EXCERPTS:
                %s

                QUESTION: %s

                ANSWER:
                """.formatted(context, question);
        }

        return """
            You are a helpful assistant that answers questions based on the provided documents.

            Use ONLY the information from the document excerpts to answer the question.
            If the answer cannot be found in the excerpts, say so - do not make up information.
            When possible, cite which document the information comes from.

            Consider the conversation history for context about what the user is asking.

            CONVERSATION HISTORY:
            %s

            DOCUMENT EXCERPTS:
            %s

            CURRENT QUESTION: %s

            ANSWER:
            """.formatted(conversationHistory, context, question);
    }

    /**
     * Truncate text for snippets.
     */
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
