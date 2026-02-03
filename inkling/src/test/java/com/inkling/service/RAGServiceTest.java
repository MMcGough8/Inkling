package com.inkling.service;

import com.inkling.dto.ChatRequest;
import com.inkling.dto.ChatResponse;
import com.inkling.model.ChatMessage;
import com.inkling.model.ChatSession;
import com.inkling.model.Document;
import com.inkling.model.DocumentChunk;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
class RAGServiceTest {

    @Inject
    RAGService ragService;

    @InjectMock
    EmbeddingModel embeddingModel;

    @InjectMock
    EmbeddingStore<TextSegment> embeddingStore;

    @InjectMock
    ChatLanguageModel chatModel;

    @BeforeEach
    void setupMocks() {
        // Mock embedding model to return a fake embedding
        float[] fakeVector = new float[1536];
        Embedding fakeEmbedding = new Embedding(fakeVector);
        when(embeddingModel.embed(anyString()))
                .thenReturn(Response.from(fakeEmbedding));

        // Default: empty search results (no documents)
        EmbeddingSearchResult<TextSegment> emptyResult = new EmbeddingSearchResult<>(Collections.emptyList());
        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(emptyResult);

        // Mock chat model
        when(chatModel.generate(anyString()))
                .thenReturn("This is a test response from the LLM.");
    }

    @AfterEach
    @Transactional
    void cleanup() {
        ChatMessage.deleteAll();
        ChatSession.deleteAll();
        DocumentChunk.deleteAll();
        Document.deleteAll();
    }

    // ========================================
    // Session Management Tests
    // ========================================

    @Test
    void chat_shouldGenerateSessionIdWhenNotProvided() {
        ChatRequest request = new ChatRequest();
        request.question = "What is AI?";
        request.sessionId = null;

        ChatResponse response = ragService.chat(request);

        assertNotNull(response.sessionId);
        assertTrue(response.sessionId.startsWith("sess_"));
        assertEquals(13, response.sessionId.length()); // "sess_" + 8 chars
    }

    @Test
    void chat_shouldUseProvidedSessionId() {
        ChatRequest request = new ChatRequest();
        request.question = "What is AI?";
        request.sessionId = "sess_custom123";

        ChatResponse response = ragService.chat(request);

        assertEquals("sess_custom123", response.sessionId);
    }

    @Test
    @Transactional
    void chat_shouldCreateNewSessionWhenNotExists() {
        ChatRequest request = new ChatRequest();
        request.question = "What is AI?";
        request.sessionId = "sess_newone";

        ragService.chat(request);

        ChatSession session = ChatSession.findBySessionId("sess_newone");
        assertNotNull(session);
        assertNotNull(session.createdAt);
        assertNotNull(session.lastActivity);
    }

    @Test
    @Transactional
    void chat_shouldReuseExistingSession() {
        // Create a session first
        ChatSession existingSession = new ChatSession();
        existingSession.sessionId = "sess_existing";
        existingSession.createdAt = LocalDateTime.now().minusHours(1);
        existingSession.lastActivity = LocalDateTime.now().minusHours(1);
        existingSession.persist();

        ChatRequest request = new ChatRequest();
        request.question = "Follow up question";
        request.sessionId = "sess_existing";

        ragService.chat(request);

        // Should still be just one session
        assertEquals(1, ChatSession.count("sessionId", "sess_existing"));
    }

    // ========================================
    // Message Persistence Tests
    // ========================================

    @Test
    @Transactional
    void chat_shouldSaveUserMessage() {
        ChatRequest request = new ChatRequest();
        request.question = "What is machine learning?";
        request.sessionId = "sess_msgtest";

        ragService.chat(request);

        ChatSession session = ChatSession.findBySessionId("sess_msgtest");
        assertNotNull(session);

        // Should have 2 messages: user + assistant
        assertEquals(2, session.messages.size());

        // First message should be user's question
        ChatMessage userMsg = session.messages.get(0);
        assertEquals(ChatMessage.Role.USER, userMsg.role);
        assertEquals("What is machine learning?", userMsg.content);
    }

    @Test
    @Transactional
    void chat_shouldSaveAssistantMessage() {
        ChatRequest request = new ChatRequest();
        request.question = "What is AI?";
        request.sessionId = "sess_assisttest";

        ChatResponse response = ragService.chat(request);

        ChatSession session = ChatSession.findBySessionId("sess_assisttest");

        // Second message should be assistant's response
        ChatMessage assistantMsg = session.messages.get(1);
        assertEquals(ChatMessage.Role.ASSISTANT, assistantMsg.role);
        assertEquals(response.answer, assistantMsg.content);
    }

    @Test
    @Transactional
    void chat_shouldAccumulateMessagesInSession() {
        String sessionId = "sess_accumulate";

        // First question
        ChatRequest request1 = new ChatRequest();
        request1.question = "Question 1";
        request1.sessionId = sessionId;
        ragService.chat(request1);

        // Second question
        ChatRequest request2 = new ChatRequest();
        request2.question = "Question 2";
        request2.sessionId = sessionId;
        ragService.chat(request2);

        ChatSession session = ChatSession.findBySessionId(sessionId);

        // Should have 4 messages: 2 user + 2 assistant
        assertEquals(4, session.messages.size());
        assertEquals("Question 1", session.messages.get(0).content);
        assertEquals("Question 2", session.messages.get(2).content);
    }

    // ========================================
    // Response Tests
    // ========================================

    @Test
    void chat_shouldReturnAnswerFromLLM() {
        when(chatModel.generate(anyString()))
                .thenReturn("Machine learning is a subset of AI.");

        ChatRequest request = new ChatRequest();
        request.question = "What is ML?";

        ChatResponse response = ragService.chat(request);

        assertEquals("Machine learning is a subset of AI.", response.answer);
    }

    @Test
    void chat_shouldReturnEmptySourcesWhenNoMatches() {
        // embeddingStore already returns empty results by default

        ChatRequest request = new ChatRequest();
        request.question = "Random question";

        ChatResponse response = ragService.chat(request);

        assertNotNull(response.sources);
        assertTrue(response.sources.isEmpty());
    }

    @Test
    @Transactional
    void chat_shouldReturnSourcesWhenMatchesFound() {
        // Create a document and chunk
        Document doc = new Document();
        doc.name = "test-doc.pdf";
        doc.contentType = "application/pdf";
        doc.size = 1000L;
        doc.uploadedAt = LocalDateTime.now();
        doc.status = Document.Status.READY;
        doc.persist();

        DocumentChunk chunk = new DocumentChunk();
        chunk.document = doc;
        chunk.content = "This is the chunk content about AI and machine learning.";
        chunk.chunkIndex = 0;
        chunk.embeddingId = "emb_123";
        chunk.persist();

        // Mock search to return this chunk
        TextSegment segment = TextSegment.from(chunk.content);
        float[] vector = new float[1536];
        Embedding embedding = new Embedding(vector);
        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.85, "emb_123", embedding, segment);
        EmbeddingSearchResult<TextSegment> result = new EmbeddingSearchResult<>(List.of(match));
        when(embeddingStore.search(any(EmbeddingSearchRequest.class))).thenReturn(result);

        ChatRequest request = new ChatRequest();
        request.question = "What is AI?";

        ChatResponse response = ragService.chat(request);

        assertEquals(1, response.sources.size());
        assertEquals("test-doc.pdf", response.sources.get(0).documentName);
        assertEquals(0, response.sources.get(0).chunkIndex);
        assertEquals(0.85, response.sources.get(0).score, 0.01);
    }

    // ========================================
    // Prompt Building Tests (via behavior)
    // ========================================

    @Test
    void chat_shouldHandleNoContextNoHistory() {
        // Empty search results, new session
        when(chatModel.generate(anyString())).thenAnswer(invocation -> {
            String prompt = invocation.getArgument(0);
            // Verify prompt mentions no relevant information found
            assertTrue(prompt.contains("no relevant information"));
            return "I couldn't find relevant information.";
        });

        ChatRequest request = new ChatRequest();
        request.question = "Unknown topic";

        ChatResponse response = ragService.chat(request);

        assertNotNull(response.answer);
    }

    @Test
    @Transactional
    void chat_shouldIncludeHistoryInPrompt() {
        String sessionId = "sess_history";

        // First exchange
        ChatRequest request1 = new ChatRequest();
        request1.question = "What is AI?";
        request1.sessionId = sessionId;
        ragService.chat(request1);

        // Second question - should include history
        when(chatModel.generate(anyString())).thenAnswer(invocation -> {
            String prompt = invocation.getArgument(0);
            // Verify conversation history is included
            assertTrue(prompt.contains("CONVERSATION HISTORY") || prompt.contains("What is AI?"));
            return "Following up on AI...";
        });

        ChatRequest request2 = new ChatRequest();
        request2.question = "Tell me more";
        request2.sessionId = sessionId;

        ragService.chat(request2);
    }

    // ========================================
    // Edge Cases
    // ========================================

    @Test
    void chat_shouldHandleLongQuestions() {
        String longQuestion = "A".repeat(5000);

        ChatRequest request = new ChatRequest();
        request.question = longQuestion;

        ChatResponse response = ragService.chat(request);

        assertNotNull(response);
        assertNotNull(response.answer);
    }

    @Test
    void chat_shouldHandleSpecialCharactersInQuestion() {
        ChatRequest request = new ChatRequest();
        request.question = "What about <script>alert('xss')</script> and SQL' OR '1'='1?";

        ChatResponse response = ragService.chat(request);

        assertNotNull(response);
        assertNotNull(response.answer);
    }

    @Test
    @Transactional
    void chat_shouldUpdateLastActivity() throws InterruptedException {
        // Create session
        ChatSession session = new ChatSession();
        session.sessionId = "sess_activity";
        session.createdAt = LocalDateTime.now().minusHours(1);
        session.lastActivity = LocalDateTime.now().minusHours(1);
        session.persist();

        LocalDateTime beforeChat = LocalDateTime.now();

        // Small delay to ensure time difference
        Thread.sleep(10);

        ChatRequest request = new ChatRequest();
        request.question = "New question";
        request.sessionId = "sess_activity";

        ragService.chat(request);

        ChatSession updated = ChatSession.findBySessionId("sess_activity");
        assertTrue(updated.lastActivity.isAfter(beforeChat));
    }
}
