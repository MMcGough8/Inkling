# Inkling

**Turn your documents into conversations.**

Inkling is an AI-powered document assistant that lets you upload files and ask questions in plain English, getting instant answers grounded in your own content.

---

## What It Does

1. **Upload** documents (PDF, TXT, MD)
2. **Process** - Inkling extracts text, chunks it, and generates vector embeddings
3. **Ask** any question about your documents
4. **Get answers** with source citations drawn directly from your content

No more digging through pages of documentation. Just ask.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Framework | Quarkus 3.30.8 |
| Language | Java 17+ |
| AI/LLM | LangChain4j + OpenAI (GPT-4o, text-embedding-3-small) |
| Vector Store | PostgreSQL + pgvector |
| Document Parsing | Apache Tika 2.9.2 |
| ORM | Hibernate with Panache |
| Testing | JUnit 5, REST-Assured, Mockito |
| Build | Maven |

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker Desktop (Quarkus auto-starts PostgreSQL)
- OpenAI API key

### Setup

1. Clone the repo
   ```bash
   git clone https://github.com/yourusername/inkling.git
   cd inkling/inkling
   ```

2. Create a `.env` file with your API key
   ```
   OPENAI_API_KEY=your-key-here
   ```

3. Run the application
   ```bash
   ./mvnw quarkus:dev
   ```

   > Quarkus Dev Services automatically starts a PostgreSQL container with pgvector - no manual Docker setup needed.

4. Open http://localhost:8080

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/documents` | Upload a document (multipart form) |
| `GET` | `/api/documents` | List all documents |
| `GET` | `/api/documents/{id}` | Get a specific document |
| `DELETE` | `/api/documents/{id}` | Delete a document |
| `POST` | `/api/chat` | Ask a question |

### Example: Upload a Document
```bash
curl -F "file=@report.pdf" http://localhost:8080/api/documents
```

### Example: Ask a Question
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "What are the key findings?"}'
```

---

## Project Structure

```
inkling/
├── src/main/java/com/inkling/
│   ├── DocumentResource.java    # Document CRUD endpoints
│   ├── ChatResource.java        # Chat/Q&A endpoint
│   ├── PageResource.java        # Frontend HTML serving
│   ├── dto/                     # Request/response objects
│   ├── exception/               # Custom exceptions & mappers
│   ├── model/                   # JPA entities
│   │   ├── Document.java
│   │   ├── DocumentChunk.java
│   │   ├── ChatSession.java
│   │   └── ChatMessage.java
│   └── service/
│       ├── DocumentService.java   # Tika parsing
│       ├── EmbeddingService.java  # Chunking & vectors
│       └── RAGService.java        # Search & LLM
├── src/main/resources/
│   ├── application.properties
│   ├── templates/index.html       # Qute template
│   └── META-INF/resources/        # CSS & JS
└── pom.xml
```

---

## Features

- **Document Management** - Upload, list, and delete documents via REST API or web UI
- **Smart Chunking** - Configurable chunk size with overlap for better context retrieval
- **Vector Search** - Semantic similarity search using pgvector (1536-dimensional embeddings)
- **Conversation Memory** - Multi-turn chat that remembers context within sessions
- **Source Attribution** - Every answer includes references to source documents
- **Global Error Handling** - Consistent JSON error responses across all endpoints
- **Modern UI** - Clean interface with drag-and-drop upload and real-time chat

---

## Roadmap

- [x] Project setup
- [x] Document upload and parsing
- [x] Text chunking and embedding
- [x] Vector similarity search
- [x] RAG pipeline with LangChain4j
- [x] Conversation memory
- [x] Frontend UI
- [x] Global exception handling
- [ ] Streaming responses (SSE)
- [ ] Multi-user support

---

## Why I Built This

I built Inkling to learn Quarkus and LangChain4j while creating something practical. RAG applications are increasingly common in enterprise settings, and this project demonstrates the full pipeline: document processing, vector search, and LLM orchestration.

---

## License

MIT

---

## Contact

**Marc McGough**
[LinkedIn](https://www.linkedin.com/in/marc-mcgough/) | [GitHub](https://github.com/MMcGough8/)
