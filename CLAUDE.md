# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Inkling is a RAG (Retrieval-Augmented Generation) document assistant built with Quarkus. Users upload documents (PDF, TXT, MD), which are parsed, chunked, and embedded into vectors. Users can then ask questions and receive AI-generated answers grounded in their documents.

**Current Status:** Scaffolding phase with placeholder code. Core RAG features not yet implemented.

## Build and Run Commands

All commands run from the `inkling/` directory.

```bash
# Development mode (live reload, auto-starts PostgreSQL+pgvector container)
./mvnw quarkus:dev

# Run tests
./mvnw test                                        # All unit tests
./mvnw test -Dtest=DocumentServiceTest             # Single test class
./mvnw test -Dtest=DocumentServiceTest#testParse   # Single test method
./mvnw verify                                      # Integration tests (runs *IT.java)

# Package
./mvnw package                 # Creates target/quarkus-app/quarkus-run.jar
./mvnw package -Dnative        # Native executable (requires GraalVM)
```

**Prerequisites:** Docker Desktop must be running for dev mode (Quarkus Dev Services auto-starts pgvector container).

**URLs in dev mode:**
- Application: http://localhost:8080
- Dev UI: http://localhost:8080/q/dev

## Tech Stack

- **Framework:** Quarkus 3.30.8
- **Language:** Java 17+
- **AI/LLM:** LangChain4j 0.23.0 with OpenAI (GPT-4o for chat, text-embedding-3-small for embeddings)
- **Database:** PostgreSQL with pgvector extension (1536-dimensional vectors)
- **Document Parsing:** Apache Tika 2.9.2
- **ORM:** Hibernate with Panache (active record pattern)
- **Testing:** JUnit 5, REST-Assured

## Architecture

```
com.inkling/
├── *Resource.java           # REST endpoints (JAX-RS with @Path, @GET, @POST)
├── service/                 # Business logic
├── model/                   # JPA entities (extend PanacheEntity)
└── ai/                      # LangChain4j integration
```

**Data flow:** Document upload → Tika parsing → Text chunking → OpenAI embedding → pgvector storage → Similarity search → LLM response

## Configuration

All configuration in `inkling/src/main/resources/application.properties`.

**Profile prefixes:** `%dev.`, `%test.`, `%prod.` for environment-specific config.

**Required environment variables for production:**
- `OPENAI_API_KEY` - OpenAI API key
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` - Database connection

**Custom app properties:**
- `inkling.chunk.size=1000` - Characters per chunk
- `inkling.chunk.overlap=200` - Overlap between chunks
- `inkling.upload.max-size=10M` - Max upload size

## Patterns

**Entities:** Extend `PanacheEntity` for automatic ID management
```java
@Entity
public class Document extends PanacheEntity {
    public String name;
}
```

**REST Resources:** Use JAX-RS annotations
```java
@Path("/api/documents")
public class DocumentResource {
    @GET
    public List<Document> list() { ... }
}
```

**Tests:** Use `@QuarkusTest` for unit tests, `@QuarkusIntegrationTest` for integration tests

## Development Roadmap

### Phase 1: Cleanup
- Remove placeholder code (GreetingResource, MyEntity, and their tests)

### Phase 2: Data Model
- Create Document entity (name, contentType, size, uploadedAt, status)
- Create DocumentChunk entity (document ManyToOne, content, chunkIndex, embeddingId)
- Create DTOs (DocumentDTO, ChatRequest, ChatResponse, SourceReference)

### Phase 3: Services
- DocumentService: Parse documents with Apache Tika, save to database
- EmbeddingService: Chunk text, generate embeddings via OpenAI, store in pgvector
- RAGService: Query similar chunks, build prompt, get LLM response

### Phase 4: REST Endpoints
- DocumentResource: POST/GET/DELETE /api/documents
- ChatResource: POST /api/chat for Q&A

### Phase 5: Testing
- Unit tests for services (Tika parsing, entity creation)
- Integration tests for REST endpoints

### Phase 6: Enhancements
- Conversation memory (multi-turn chat with session history)
- Streaming responses (SSE for real-time token streaming)
- Global exception handling
- Frontend UI
