package com.nxn.sistemawebmedico.dto;

public record MedicoRequest(String documento, String nombre, String apellido, String registroMedico,
        String telefono, String email, Integer idEspecialidad) { }
