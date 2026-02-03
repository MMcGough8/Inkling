package com.inkling;

import com.inkling.dto.DocumentDTO;
import com.inkling.exception.DocumentNotFoundException;
import com.inkling.exception.DocumentProcessingException;
import com.inkling.exception.ValidationException;
import com.inkling.model.Document;
import com.inkling.service.DocumentService;
import com.inkling.service.DocumentService.DocumentParseResult;
import com.inkling.service.EmbeddingService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.RestForm;

import org.apache.tika.exception.TikaException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

@Path("/api/documents")
@Produces(MediaType.APPLICATION_JSON)
public class DocumentResource {

    @Inject
    DocumentService documentService;

    @Inject
    EmbeddingService embeddingService;

    /**
     * Upload a document for processing.
     *
     * Example: curl -F "file=@report.pdf" http://localhost:8080/api/documents
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response upload(@RestForm("file") FileUpload file) {
        if (file == null) {
            throw new ValidationException("No file provided");
        }

        DocumentParseResult result = null;

        try (InputStream inputStream = Files.newInputStream(file.uploadedFile())) {
            // Parse document and save entity
            result = documentService.processUpload(
                    file.fileName(),
                    file.contentType(),
                    Files.size(file.uploadedFile()),
                    inputStream
            );

            // Process embeddings (chunking + vector storage)
            embeddingService.processDocument(result.document(), result.extractedText());

            return Response.status(Response.Status.CREATED)
                    .entity(DocumentDTO.from(result.document()))
                    .build();

        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to read uploaded file", e);
        } catch (TikaException e) {
            throw new DocumentProcessingException("Failed to parse document", e);
        } catch (Exception e) {
            // Mark document as failed in a separate transaction
            if (result != null && result.document() != null) {
                embeddingService.markDocumentFailed(result.document().id);
            }
            throw new DocumentProcessingException("Failed to process document", e);
        }
    }

    /**
     * List all documents.
     */
    @GET
    public List<DocumentDTO> list() {
        return documentService.listAll()
                .stream()
                .map(DocumentDTO::from)
                .toList();
    }

    /**
     * Get a single document by ID.
     */
    @GET
    @Path("/{id}")
    public DocumentDTO get(@PathParam("id") Long id) {
        Document doc = documentService.findById(id);
        if (doc == null) {
            throw new DocumentNotFoundException(id);
        }
        return DocumentDTO.from(doc);
    }

    /**
     * Delete a document and its chunks/embeddings.
     */
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        Document doc = documentService.findById(id);
        if (doc == null) {
            throw new DocumentNotFoundException(id);
        }

        // Delete embeddings from pgvector first
        embeddingService.deleteEmbeddings(doc);

        // Delete document (cascades to chunks)
        documentService.delete(id);

        return Response.noContent().build();
    }
}
