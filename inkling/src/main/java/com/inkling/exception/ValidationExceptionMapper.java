package com.inkling.exception;

import com.inkling.dto.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps ValidationException to HTTP 400 responses.
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

    @Override
    public Response toResponse(ValidationException e) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponse.of(
                        "Bad Request",
                        e.getMessage(),
                        400
                ))
                .build();
    }
}
