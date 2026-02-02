package com.inkling;

import com.inkling.dto.DocumentDTO;
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
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("No file provided"))
                    .build();
        }

        try (InputStream inputStream = Files.newInputStream(file.uploadedFile())) {
            // Parse document and save entity
            DocumentParseResult result = documentService.processUpload(
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
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to read uploaded file: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to process document: " + e.getMessage()))
                    .build();
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
    public Response get(@PathParam("id") Long id) {
        Document doc = documentService.findById(id);
        if (doc == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Document not found: " + id))
                    .build();
        }
        return Response.ok(DocumentDTO.from(doc)).build();
    }

    /**
     * Delete a document and its chunks/embeddings.
     */
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        Document doc = documentService.findById(id);
        if (doc == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Document not found: " + id))
                    .build();
        }

        // Delete embeddings from pgvector first
        embeddingService.deleteEmbeddings(doc);

        // Delete document (cascades to chunks)
        documentService.delete(id);

        return Response.noContent().build();
    }

    /**
     * Simple error response wrapper.
     */
    public record ErrorResponse(String error) {}
}
