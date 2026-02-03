package com.inkling.exception;

import com.inkling.dto.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Maps DocumentProcessingException to HTTP 500 responses.
 */
@Provider
public class DocumentProcessingExceptionMapper implements ExceptionMapper<DocumentProcessingException> {

    private static final Logger LOG = Logger.getLogger(DocumentProcessingExceptionMapper.class);

    @Override
    public Response toResponse(DocumentProcessingException e) {
        // Log the full exception for debugging
        LOG.error("Document processing failed", e);

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ErrorResponse.of(
                        "Processing Error",
                        e.getMessage(),
                        500
                ))
                .build();
    }
}
