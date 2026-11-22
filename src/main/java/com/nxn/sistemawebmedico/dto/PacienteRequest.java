package com.nxn.sistemawebmedico.dto;

public record PacienteRequest(String tipoDocumento, String documento, String nombre, String apellido,
        String fechaNacimiento, String sexo, String direccion, String telefono, String email) { }
