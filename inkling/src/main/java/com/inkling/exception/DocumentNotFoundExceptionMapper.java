package com.inkling.exception;

import com.inkling.dto.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps DocumentNotFoundException to HTTP 404 responses.
 */
@Provider
public class DocumentNotFoundExceptionMapper implements ExceptionMapper<DocumentNotFoundException> {

    @Override
    public Response toResponse(DocumentNotFoundException e) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(ErrorResponse.of(
                        "Not Found",
                        e.getMessage(),
                        404
                ))
                .build();
    }
}
