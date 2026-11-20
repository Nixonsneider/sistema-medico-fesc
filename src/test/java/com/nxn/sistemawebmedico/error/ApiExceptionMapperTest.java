package com.nxn.sistemawebmedico.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionMapperTest {
    private final ApiExceptionMapper mapper = new ApiExceptionMapper();
    private final ObjectMapper json = new ObjectMapper();

    @ParameterizedTest
    @CsvSource({
        "23505,409,DUPLICATE", "23503,409,RELATED_RECORDS", "23001,409,RELATED_RECORDS",
        "23502,400,INVALID_DATA", "23514,400,INVALID_DATA", "22001,400,INVALID_DATA",
        "08001,503,SERVICE_UNAVAILABLE", "99999,500,INTERNAL_ERROR"
    })
    void erroresSqlSeTraducenSinExponerDatos(String state, int status, String code) throws Exception {
        var failure = new PersistenceException(new SQLException("SQL y datos privados del paciente", state));
        try (var response = mapper.toResponse(failure)) {
            ApiError error = (ApiError) response.getEntity();
            assertEquals(status, response.getStatus());
            assertEquals(code, error.code());
            assertEquals("application/json", response.getMediaType().toString());
            String body = json.writeValueAsString(error);
            assertFalse(body.contains("privados"));
            assertFalse(body.contains("SQLException"));
        }
    }

    @Test
    void reconoceRestriccionesDentroDeUnLoteSql() {
        var batch = new BatchUpdateException();
        batch.setNextException(new SQLException("Detalle interno", "23505"));
        assertEquals(409, mapper.toResponse(new PersistenceException(batch)).getStatus());
    }

    @Test
    void validacionConservaMensajesDeCampoSegurosYSerializables() throws Exception {
        try (var response = mapper.toResponse(ApplicationException.invalidData(Map.of("nombre", "Completa este campo.")))) {
            var data = json.readTree(json.writeValueAsString(response.getEntity()));
            assertEquals(400, data.get("status").asInt());
            assertEquals("INVALID_DATA", data.get("code").asText());
            assertEquals("Completa este campo.", data.get("fields").get("nombre").asText());
        }
    }

    @Test
    void erroresNoEncontradosTienenElMismoContrato() {
        assertEquals(404, mapper.toResponse(new EntityNotFoundException("Dato privado")).getStatus());
        try (var response = mapper.toResponse(new NotFoundException("Ruta interna"))) {
            assertEquals(404, response.getStatus());
            assertEquals("NOT_FOUND", ((ApiError) response.getEntity()).code());
            assertFalse(((ApiError) response.getEntity()).message().contains("interna"));
        }
    }

    @Test
    void conservaLaCabeceraAllowDelErrorDeMetodo() {
        try (var response = mapper.toResponse(new NotAllowedException("GET"))) {
            assertEquals(405, response.getStatus());
            assertTrue(response.getAllowedMethods().contains("GET"));
            assertEquals("no-store", response.getHeaderString("Cache-Control"));
        }
    }

    @Test
    void falloInesperadoNoDevuelveElMensajeInterno() throws Exception {
        try (var response = mapper.toResponse(new IllegalStateException("password=secreto"))) {
            assertEquals(500, response.getStatus());
            assertFalse(json.writeValueAsString(response.getEntity()).contains("secreto"));
        }
    }

    @Test
    void jsonMalformadoNoDevuelveElContenidoDeEntrada() {
        JsonProcessingException exception = assertThrows(JsonProcessingException.class,
                () -> json.readTree("{datoPrivado: contraseña}"));
        try (var response = new InvalidJsonMapper().toResponse(exception)) {
            assertEquals(400, response.getStatus());
            assertFalse(((ApiError) response.getEntity()).message().contains("contraseña"));
        }
    }
}
