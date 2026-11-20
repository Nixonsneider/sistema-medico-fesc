package com.nxn.sistemawebmedico.error;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;

/** Traduce errores de negocio, HTTP y persistencia sin exponer detalles internos. */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger LOG = Logger.getLogger(ApiExceptionMapper.class.getName());

    @Override
    public Response toResponse(Exception exception) {
        ApiError error = classify(exception);
        if (error.status() >= 500) {
            LOG.warning("Operación REST fallida: " + exception.getClass().getName());
        }
        Response.ResponseBuilder response = exception instanceof WebApplicationException web
                ? Response.fromResponse(web.getResponse()) : Response.status(error.status());
        return response.status(error.status()).type(MediaType.APPLICATION_JSON_TYPE)
                .header("Cache-Control", "no-store").entity(error).build();
    }

    private ApiError classify(Exception exception) {
        Throwable cause = exception;
        for (int depth = 0; cause != null && depth < 20; depth++, cause = cause.getCause()) {
            if (cause instanceof ApplicationException application) return application.getError();
            if (cause instanceof EntityNotFoundException || cause instanceof NoResultException) {
                return error(404, "NOT_FOUND", "No se encontró el registro solicitado.");
            }
            if (cause instanceof OptimisticLockException) {
                return error(409, "CONFLICT", "El registro cambió. Actualiza la información e inténtalo de nuevo.");
            }
            if (cause instanceof SQLException sql) {
                for (int next = 0; sql != null && next < 20; next++, sql = sql.getNextException()) {
                    String state = sql.getSQLState();
                    if ("23505".equals(state)) return error(409, "DUPLICATE", "Ya existe un registro con esos datos únicos.");
                    if ("23503".equals(state) || "23001".equals(state)) {
                        return error(409, "RELATED_RECORDS", "La operación no es posible porque hay registros relacionados o una referencia no válida.");
                    }
                    if ("23502".equals(state) || "23514".equals(state) || (state != null && state.startsWith("22"))) {
                        return error(400, "INVALID_DATA", "Los datos no cumplen las condiciones del registro.");
                    }
                    if (state != null && state.startsWith("08")) {
                        return error(503, "SERVICE_UNAVAILABLE", "El servicio no está disponible temporalmente. Inténtalo de nuevo.");
                    }
                }
            }
        }
        if (exception instanceof WebApplicationException web) {
            int status = web.getResponse().getStatus();
            return switch (status) {
                case 400 -> error(400, "INVALID_DATA", "La solicitud contiene datos no válidos.");
                case 401 -> error(401, "UNAUTHORIZED", "Necesitas identificarte para realizar esta operación.");
                case 403 -> error(403, "FORBIDDEN", "No tienes permiso para realizar esta operación.");
                case 404 -> error(404, "NOT_FOUND", "No se encontró el recurso solicitado.");
                case 405 -> error(405, "METHOD_NOT_ALLOWED", "Esta operación no está disponible para el recurso.");
                case 406 -> error(406, "NOT_ACCEPTABLE", "El formato de respuesta solicitado no está disponible.");
                case 409 -> error(409, "CONFLICT", "La operación entra en conflicto con el estado actual del registro.");
                case 415 -> error(415, "UNSUPPORTED_MEDIA_TYPE", "El formato de los datos enviados no es compatible.");
                case 503 -> error(503, "SERVICE_UNAVAILABLE", "El servicio no está disponible temporalmente. Inténtalo de nuevo.");
                default -> status >= 400 && status < 500
                        ? error(status, "REQUEST_ERROR", "No se pudo completar la solicitud.")
                        : error(500, "INTERNAL_ERROR", "No se pudo completar la operación. Inténtalo de nuevo.");
            };
        }
        if (exception instanceof IllegalArgumentException) return error(400, "INVALID_DATA", "Revisa los datos enviados.");
        return error(500, "INTERNAL_ERROR", "No se pudo completar la operación. Inténtalo de nuevo.");
    }

    private ApiError error(int status, String code, String message) {
        return new ApiError(status, code, message, Map.of());
    }
}
