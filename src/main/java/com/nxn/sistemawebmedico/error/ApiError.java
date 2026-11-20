package com.nxn.sistemawebmedico.error;

import java.util.Map;

/** Contrato público de error. No contiene SQL, causas internas ni datos clínicos. */
public record ApiError(int status, String code, String message, Map<String, String> fields) {
    public ApiError {
        fields = fields == null ? Map.of() : Map.copyOf(fields);
    }
}
