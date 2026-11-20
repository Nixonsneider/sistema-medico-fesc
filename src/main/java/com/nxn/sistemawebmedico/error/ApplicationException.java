package com.nxn.sistemawebmedico.error;

import java.util.Map;

/** Errores funcionales seguros que los futuros servicios pueden comunicar a la API. */
public final class ApplicationException extends RuntimeException {
    private final ApiError error;

    private ApplicationException(int status, String code, String message, Map<String, String> fields) {
        super(message);
        error = new ApiError(status, code, message, fields);
    }

    public static ApplicationException invalidData(Map<String, String> fields) {
        return new ApplicationException(400, "INVALID_DATA", "Revisa los campos indicados.", fields);
    }

    public static ApplicationException notFound() {
        return new ApplicationException(404, "NOT_FOUND", "No se encontró el registro solicitado.", Map.of());
    }

    public static ApplicationException duplicate() {
        return new ApplicationException(409, "DUPLICATE", "Ya existe un registro con esos datos únicos.", Map.of());
    }

    public static ApplicationException relatedRecords() {
        return new ApplicationException(409, "RELATED_RECORDS", "No se puede eliminar el registro porque tiene información relacionada.", Map.of());
    }

    public ApiError getError() {
        return error;
    }
}
