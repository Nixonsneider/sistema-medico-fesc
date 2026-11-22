package com.nxn.sistemawebmedico.dto;

import com.nxn.sistemawebmedico.entity.Paciente;

/** Datos necesarios para el listado; el contacto se consulta en la ficha individual. */
public record PacienteSummary(Integer idPaciente, String tipoDocumento, String documento, String nombre,
        String apellido, String fechaNacimiento) {
    public static PacienteSummary from(Paciente patient) {
        return new PacienteSummary(patient.getIdPaciente(), patient.getTipoDocumento(), patient.getDocumento(),
                patient.getNombre(), patient.getApellido(), patient.getFechaNacimiento().toString());
    }
}
