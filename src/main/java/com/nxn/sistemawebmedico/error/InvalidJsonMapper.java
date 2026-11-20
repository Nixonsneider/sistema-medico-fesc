package com.nxn.sistemawebmedico.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

/** Evita devolver el contenido de entrada o mensajes internos del parser JSON. */
@Provider
public class InvalidJsonMapper implements ExceptionMapper<JsonProcessingException> {
    @Override
    public Response toResponse(JsonProcessingException exception) {
        return Response.status(400).type(MediaType.APPLICATION_JSON_TYPE)
                .header("Cache-Control", "no-store")
                .entity(new ApiError(400, "INVALID_DATA", "Revisa el formato y los tipos de los datos enviados.", Map.of()))
                .build();
    }
}
