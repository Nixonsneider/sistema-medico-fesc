package com.nxn.sistemawebmedico.service;

import com.nxn.sistemawebmedico.error.ApplicationException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class DataValidation {
    private final Map<String, String> errors = new LinkedHashMap<>();

    String required(String field, String value, int max) {
        String cleaned = value == null ? "" : value.strip();
        if (cleaned.isBlank()) errors.put(field, "Completa este campo.");
        else if (cleaned.codePointCount(0, cleaned.length()) > max) errors.put(field, "Usa como máximo " + max + " caracteres.");
        return cleaned;
    }

    void email(String value) {
        if (!value.isEmpty() && !value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) errors.put("email", "Ingresa un correo electrónico válido.");
    }

    void reference(Integer value) {
        if (value == null || value < 1) errors.put("idEspecialidad", "Selecciona una especialidad.");
    }

    void check() { if (!errors.isEmpty()) throw ApplicationException.invalidData(errors); }

    void reject(String field, String message) { errors.put(field, message); }

    static void id(int id) {
        if (id < 1) throw ApplicationException.invalidData(Map.of("id", "El identificador debe ser positivo."));
    }

    static String search(String query, int page, int size) {
        Map<String, String> errors = new LinkedHashMap<>();
        String value = query == null ? "" : query.strip();
        if (value.length() > 100) errors.put("q", "Usa como máximo 100 caracteres para buscar.");
        if (page < 1 || size < 1 || size > 100 || (long) (page - 1) * size > Integer.MAX_VALUE) {
            errors.put("page", "La página debe ser positiva y el tamaño debe estar entre 1 y 100.");
        }
        if (!errors.isEmpty()) throw ApplicationException.invalidData(errors);
        return value.toLowerCase(Locale.ROOT);
    }
}
