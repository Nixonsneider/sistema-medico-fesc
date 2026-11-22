package com.nxn.sistemawebmedico.dto;

import com.nxn.sistemawebmedico.entity.Paciente;

public record PacienteResponse(Integer idPaciente, String tipoDocumento, String documento, String nombre, String apellido,
        String fechaNacimiento, String sexo, String direccion, String telefono, String email) {
    public static PacienteResponse from(Paciente p) {
        return new PacienteResponse(p.getIdPaciente(), p.getTipoDocumento(), p.getDocumento(), p.getNombre(), p.getApellido(),
                p.getFechaNacimiento().toString(), p.getSexo(), p.getDireccion(), p.getTelefono(), p.getEmail());
    }
}
