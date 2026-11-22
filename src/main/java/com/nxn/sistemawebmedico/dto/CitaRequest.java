package com.nxn.sistemawebmedico.dto;

public record CitaRequest(Integer idPaciente, Integer idMedico, String fechaHora, String motivo) { }
