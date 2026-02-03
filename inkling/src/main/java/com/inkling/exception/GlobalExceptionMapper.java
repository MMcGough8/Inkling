package com.inkling.exception;

import com.inkling.dto.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Catch-all exception mapper for unexpected errors.
 * Prevents stack traces from leaking to API clients.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Exception e) {
        // Log the full exception for debugging
        LOG.error("Unexpected error", e);

        // Return generic message to client (don't expose internal details)
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ErrorResponse.of(
                        "Internal Server Error",
                        "An unexpected error occurred. Please try again later.",
                        500
                ))
                .build();
    }
}
