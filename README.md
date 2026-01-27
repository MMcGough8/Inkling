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
| Framework | Quarkus 3.x |
| AI/LLM | LangChain4j |
| LLM Provider | OpenAI (swappable) |
| Vector Store | PostgreSQL + pgvector |
| Document Parsing | Apache Tika |
| Build | Maven |

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker (for PostgreSQL with pgvector)
- OpenAI API key

### Setup

1. Clone the repo
```bash
   git clone https://github.com/yourusername/inkling.git
   cd inkling
```

2. Create a `.env` file with your API key
```
   OPENAI_API_KEY=your-key-here
```

3. Start the database
```bash
   docker-compose up -d
```

4. Run the application
```bash
   ./mvnw quarkus:dev
```

5. Open http://localhost:8080

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/documents` | Upload a document |
| GET | `/api/documents` | List all documents |
| DELETE | `/api/documents/{id}` | Delete a document |
| POST | `/api/chat` | Ask a question |
| GET | `/api/chat/sessions/{id}` | Get conversation history |

---

## Project Structure
```
inkling/
├── src/main/java/com/inkling/
│   ├── DocumentResource.java
│   ├── ChatResource.java
│   ├── service/
│   │   ├── DocumentService.java
│   │   ├── EmbeddingService.java
│   │   └── RAGService.java
│   ├── model/
│   │   ├── Document.java
│   │   └── DocumentChunk.java
│   └── ai/
│       └── InklingAssistant.java
├── src/main/resources/
│   └── application.properties
└── pom.xml
```

---

## Roadmap

- [x] Project setup
- [ ] Document upload and parsing
- [ ] Text chunking and embedding
- [ ] Vector similarity search
- [ ] RAG pipeline with LangChain4j
- [ ] Conversation memory
- [ ] Frontend UI
- [ ] Streaming responses
- [ ] Multi-user support

---

## Why I Built This

I'm builing Inkling to learn Quarkus and LangChain4j while creating something practical. RAG applications are increasingly common in enterprise settings, and this project demonstrates the full pipeline: document processing, vector search, and LLM orchestration.

---

## License

MIT

---

## Contact

**Marc McGough**  
[LinkedIn](https://www.linkedin.com/in/marc-mcgough/) | [GitHub](https://github.com/MMcGough8/)
