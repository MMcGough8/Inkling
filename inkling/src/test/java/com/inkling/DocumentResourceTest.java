package com.inkling;

import com.inkling.model.Document;
import com.inkling.model.DocumentChunk;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
class DocumentResourceTest {

    @InjectMock
    EmbeddingModel embeddingModel;

    @InjectMock
    EmbeddingStore<TextSegment> embeddingStore;

    @BeforeEach
    void setupMocks() {
        // Mock embedding model to return a fake embedding
        float[] fakeVector = new float[1536];
        Embedding fakeEmbedding = new Embedding(fakeVector);
        when(embeddingModel.embed(anyString()))
                .thenReturn(Response.from(fakeEmbedding));

        // Mock embedding store to return a fake ID
        when(embeddingStore.add(any(Embedding.class), any(TextSegment.class)))
                .thenAnswer(inv -> UUID.randomUUID().toString());
    }

    @AfterEach
    @Transactional
    void cleanup() {
        DocumentChunk.deleteAll();
        Document.deleteAll();
    }

    @Test
    void list_shouldReturnEmptyArrayWhenNoDocuments() {
        given()
            .when()
                .get("/api/documents")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$.size()", is(0));
    }

    @Test
    void upload_shouldRejectMissingFile() {
        given()
            .contentType("multipart/form-data")
            .when()
                .post("/api/documents")
            .then()
                .statusCode(400);
    }

    @Test
    void upload_shouldAcceptTextFile() throws IOException {
        // Given: a temporary text file
        Path tempFile = Files.createTempFile("test", ".txt");
        Files.writeString(tempFile, "This is test content for the document.");

        try {
            given()
                .multiPart("file", tempFile.toFile(), "text/plain")
                .when()
                    .post("/api/documents")
                .then()
                    .statusCode(201)
                    .contentType(ContentType.JSON)
                    .body("id", notNullValue())
                    .body("name", endsWith(".txt"))
                    .body("status", anyOf(is("READY"), is("PROCESSING")))
                    .body("chunkCount", greaterThanOrEqualTo(0));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void get_shouldReturn404ForNonExistent() {
        given()
            .when()
                .get("/api/documents/99999")
            .then()
                .statusCode(404)
                .body("error", is("Not Found"))
                .body("message", containsString("not found"));
    }

    @Test
    void delete_shouldReturn404ForNonExistent() {
        given()
            .when()
                .delete("/api/documents/99999")
            .then()
                .statusCode(404)
                .body("error", is("Not Found"))
                .body("message", containsString("not found"));
    }

    @Test
    void fullCrudFlow() throws IOException {
        // Create
        Path tempFile = Files.createTempFile("crud-test", ".txt");
        Files.writeString(tempFile, "CRUD test content for full flow test.");

        try {
            // Upload
            int id = given()
                .multiPart("file", tempFile.toFile(), "text/plain")
                .when()
                    .post("/api/documents")
                .then()
                    .statusCode(201)
                    .extract()
                    .path("id");

            // Read
            given()
                .when()
                    .get("/api/documents/" + id)
                .then()
                    .statusCode(200)
                    .body("id", is(id));

            // List
            given()
                .when()
                    .get("/api/documents")
                .then()
                    .statusCode(200)
                    .body("$.size()", is(1));

            // Delete
            given()
                .when()
                    .delete("/api/documents/" + id)
                .then()
                    .statusCode(204);

            // Verify deleted
            given()
                .when()
                    .get("/api/documents/" + id)
                .then()
                    .statusCode(404);

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
