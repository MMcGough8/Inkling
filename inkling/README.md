# Inkling

A RAG (Retrieval-Augmented Generation) document assistant built with Quarkus. Upload documents, ask questions, and get AI-generated answers grounded in your content.

## Features

- **Document Upload** - Support for PDF, TXT, and Markdown files
- **Intelligent Chunking** - Documents are split into overlapping chunks for better context retrieval
- **Vector Search** - Semantic similarity search using pgvector
- **Conversational AI** - Multi-turn chat with conversation memory
- **Source Attribution** - Responses include references to source documents
- **Modern UI** - Clean, responsive interface with drag-and-drop upload

## Tech Stack

- **Framework:** Quarkus 3.30.8
- **Language:** Java 17+
- **AI/LLM:** LangChain4j with OpenAI (GPT-4o for chat, text-embedding-3-small for embeddings)
- **Database:** PostgreSQL with pgvector extension
- **Document Parsing:** Apache Tika 2.9.2
- **ORM:** Hibernate with Panache
- **Testing:** JUnit 5, REST-Assured, Mockito

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- Docker Desktop (for dev mode - auto-starts PostgreSQL)
- OpenAI API key

## Quick Start

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd inkling
   ```

2. **Configure OpenAI API key**

   Create a `.env` file in the `inkling/` directory:
   ```
   OPENAI_API_KEY=your-api-key-here
   ```

3. **Start the application**
   ```bash
   ./mvnw quarkus:dev
   ```

   Quarkus Dev Services will automatically start a PostgreSQL container with pgvector.

4. **Open the UI**

   Navigate to http://localhost:8080

## Usage

### Web Interface

1. **Upload Documents** - Drag and drop files onto the upload area, or click to browse
2. **Ask Questions** - Type your question in the chat input
3. **View Sources** - Expand source references to see which documents informed the answer
4. **Continue Conversations** - The assistant remembers context within a session

### REST API

#### Documents

```bash
# Upload a document
curl -F "file=@document.pdf" http://localhost:8080/api/documents

# List all documents
curl http://localhost:8080/api/documents

# Get a specific document
curl http://localhost:8080/api/documents/{id}

# Delete a document
curl -X DELETE http://localhost:8080/api/documents/{id}
```

#### Chat

```bash
# Ask a question (new session)
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "What is this document about?"}'

# Continue a conversation
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "Tell me more", "sessionId": "sess_abc123"}'
```

## Project Structure

```
com.inkling/
├── DocumentResource.java      # Document upload/management endpoints
├── ChatResource.java          # Chat/Q&A endpoint
├── PageResource.java          # Frontend HTML serving
├── dto/                       # Data transfer objects
│   ├── ChatRequest.java
│   ├── ChatResponse.java
│   ├── DocumentDTO.java
│   └── ErrorResponse.java
├── exception/                 # Custom exceptions and mappers
├── model/                     # JPA entities
│   ├── Document.java
│   ├── DocumentChunk.java
│   ├── ChatSession.java
│   └── ChatMessage.java
└── service/                   # Business logic
    ├── DocumentService.java   # Document parsing with Tika
    ├── EmbeddingService.java  # Chunking and vector storage
    └── RAGService.java        # Query and LLM integration
```

## Configuration

Configuration is in `src/main/resources/application.properties`.

| Property | Default | Description |
|----------|---------|-------------|
| `inkling.chunk.size` | 1000 | Characters per text chunk |
| `inkling.chunk.overlap` | 200 | Overlap between chunks |
| `quarkus.http.body.uploads.max-size` | 10M | Maximum upload file size |

### Environment Variables (Production)

| Variable | Description |
|----------|-------------|
| `OPENAI_API_KEY` | OpenAI API key (required) |
| `DB_HOST` | PostgreSQL host |
| `DB_PORT` | PostgreSQL port |
| `DB_NAME` | Database name |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |

## Development

### Running Tests

```bash
# All tests
./mvnw test

# Specific test class
./mvnw test -Dtest=RAGServiceTest

# Integration tests
./mvnw verify
```

### Dev Mode Features

- **Live Reload** - Code changes apply automatically
- **Dev UI** - Available at http://localhost:8080/q/dev
- **Dev Services** - PostgreSQL container starts automatically

### Building for Production

```bash
# Standard JAR
./mvnw package

# Native executable (requires GraalVM)
./mvnw package -Dnative
```

## Architecture

```
┌─────────────┐     ┌─────────────────┐     ┌──────────────┐
│   Upload    │────▶│ DocumentService │────▶│   Document   │
│  (PDF/TXT)  │     │  (Tika Parse)   │     │   (Entity)   │
└─────────────┘     └─────────────────┘     └──────────────┘
                            │
                            ▼
                    ┌─────────────────┐     ┌──────────────┐
                    │EmbeddingService │────▶│DocumentChunk │
                    │ (Chunk + Embed) │     │   (Entity)   │
                    └─────────────────┘     └──────────────┘
                            │
                            ▼
                    ┌─────────────────┐
                    │    pgvector     │
                    │ (Vector Store)  │
                    └─────────────────┘

┌─────────────┐     ┌─────────────────┐     ┌──────────────┐
│  Question   │────▶│   RAGService    │────▶│   Answer +   │
│             │     │ (Search + LLM)  │     │   Sources    │
└─────────────┘     └─────────────────┘     └──────────────┘
```

## License

[Add your license here]

## Acknowledgments

- Built with [Quarkus](https://quarkus.io/)
- AI powered by [LangChain4j](https://docs.langchain4j.dev/) and [OpenAI](https://openai.com/)
- Vector storage with [pgvector](https://github.com/pgvector/pgvector)
