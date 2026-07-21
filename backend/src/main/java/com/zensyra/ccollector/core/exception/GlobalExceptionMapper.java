package com.zensyra.ccollector.core.exception;

import com.zensyra.ccollector.core.dto.response.ResponseDTO;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Traduce cualquier excepción no controlada a un {@link ResponseDTO} de error,
 * preservando el status HTTP de las {@link WebApplicationException}.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        int status = exception instanceof WebApplicationException wae
                ? wae.getResponse().getStatus()
                : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

        if (status >= 500) {
            LOG.error("Unhandled error", exception);
        }

        String message = exception.getMessage() != null
                ? exception.getMessage()
                : exception.getClass().getSimpleName();

        return Response.status(status)
                .entity(ResponseDTO.fail(message))
                .build();
    }
}
