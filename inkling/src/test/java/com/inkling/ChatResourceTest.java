package com.inkling;

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
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
class ChatResourceTest {

    @InjectMock
    EmbeddingModel embeddingModel;

    @InjectMock
    EmbeddingStore<TextSegment> embeddingStore;

    @InjectMock
    ChatLanguageModel chatModel;

    @BeforeEach
    void setupMocks() {
        // Mock embedding model
        float[] fakeVector = new float[1536];
        Embedding fakeEmbedding = new Embedding(fakeVector);
        when(embeddingModel.embed(anyString()))
                .thenReturn(Response.from(fakeEmbedding));

        // Mock embedding store to return empty results (no documents uploaded)
        EmbeddingSearchResult<TextSegment> emptyResult = new EmbeddingSearchResult<>(Collections.emptyList());
        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(emptyResult);

        // Mock chat model
        when(chatModel.generate(anyString()))
                .thenReturn("I couldn't find any relevant information in your documents.");
    }

    @Test
    void chat_shouldRejectEmptyQuestion() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"question\": \"\"}")
            .when()
                .post("/api/chat")
            .then()
                .statusCode(400)
                .body("error", containsString("required"));
    }

    @Test
    void chat_shouldRejectNullQuestion() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
            .when()
                .post("/api/chat")
            .then()
                .statusCode(400)
                .body("error", containsString("required"));
    }

    @Test
    void chat_shouldRejectBlankQuestion() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"question\": \"   \"}")
            .when()
                .post("/api/chat")
            .then()
                .statusCode(400)
                .body("error", containsString("required"));
    }

    @Test
    void chat_shouldReturnAnswerForValidQuestion() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"question\": \"What is in the documents?\"}")
            .when()
                .post("/api/chat")
            .then()
                .statusCode(200)
                .body("answer", notNullValue())
                .body("sessionId", notNullValue())
                .body("sources", notNullValue());
    }

    @Test
    void chat_shouldAcceptOptionalSessionId() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"question\": \"Follow up question\", \"sessionId\": \"sess_12345\"}")
            .when()
                .post("/api/chat")
            .then()
                .statusCode(200)
                .body("sessionId", is("sess_12345"));
    }

    @Test
    void chat_shouldAcceptOptionalDocumentIds() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"question\": \"Search specific docs\", \"documentIds\": [1, 2, 3]}")
            .when()
                .post("/api/chat")
            .then()
                .statusCode(200)
                .body("answer", notNullValue());
    }
}
