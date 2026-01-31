package com.inkling.service;

import com.inkling.dto.ChatRequest;
import com.inkling.dto.ChatResponse;
import com.inkling.dto.SourceReference;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RAGService {

    private static final int MAX_RESULTS = 5;
    private static final double MIN_SCORE = 0.7;
    private static final int SNIPPET_LENGTH = 200;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Inject
    ChatLanguageModel chatModel;

    /**
     * Answer a question using RAG: retrieve relevant chunks, then generate a response.
     */
    public ChatResponse chat(ChatRequest request) {
        // Step 1: Embed the question
        Embedding questionEmbedding = embeddingModel.embed(request.question).content();

        // Step 2: Search for similar chunks in pgvector
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(questionEmbedding)
                .maxResults(MAX_RESULTS)
                .minScore(MIN_SCORE)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

        // Step 3: Build context from matched chunks
        StringBuilder contextBuilder = new StringBuilder();
        List<SourceReference> sources = new ArrayList<>();

        for (EmbeddingMatch<TextSegment> match : matches) {
            String embeddingId = match.embeddingId();
            String content = match.embedded().text();
            double score = match.score();

            // Look up our DocumentChunk to get document info
            DocumentChunk chunk = DocumentChunk.findByEmbeddingId(embeddingId);
            if (chunk != null) {
                // Add to context for the LLM
                contextBuilder.append("---\n");
                contextBuilder.append("Source: ").append(chunk.document.name);
                contextBuilder.append(" (Section ").append(chunk.chunkIndex + 1).append(")\n");
                contextBuilder.append(content).append("\n\n");

                // Build source reference for the response
                sources.add(SourceReference.of(
                        chunk.document.id,
                        chunk.document.name,
                        chunk.chunkIndex,
                        truncate(content, SNIPPET_LENGTH),
                        score
                ));
            }
        }

        // Step 4: Build the prompt
        String prompt = buildPrompt(request.question, contextBuilder.toString());

        // Step 5: Get LLM response
        String answer = chatModel.generate(prompt);

        // Step 6: Generate session ID if not provided
        String sessionId = request.sessionId != null
                ? request.sessionId
                : "sess_" + UUID.randomUUID().toString().substring(0, 8);

        return ChatResponse.of(answer, sources, sessionId);
    }

    /**
     * Build the prompt with context and question.
     */
    private String buildPrompt(String question, String context) {
        if (context.isBlank()) {
            return """
                The user asked a question, but no relevant information was found in the documents.

                Question: %s

                Please let the user know that you couldn't find relevant information in their documents \
                to answer this question. Suggest they upload relevant documents or rephrase their question.
                """.formatted(question);
        }

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
