package com.nxn.sistemawebmedico.dto;

import com.nxn.sistemawebmedico.entity.Medico;

public record MedicoResponse(Integer idMedico, String documento, String nombre, String apellido,
        String registroMedico, String telefono, String email, EspecialidadResponse especialidad) {
    public static MedicoResponse from(Medico entity) {
        return new MedicoResponse(entity.getIdMedico(), entity.getDocumento(), entity.getNombre(), entity.getApellido(),
                entity.getRegistroMedico(), entity.getTelefono(), entity.getEmail(), EspecialidadResponse.from(entity.getEspecialidad()));
    }
}
