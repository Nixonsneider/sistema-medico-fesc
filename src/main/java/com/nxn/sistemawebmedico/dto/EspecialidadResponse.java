package com.nxn.sistemawebmedico.dto;

import com.nxn.sistemawebmedico.entity.Especialidad;

public record EspecialidadResponse(Integer idEspecialidad, String nombre, String descripcion, boolean activo) {
    public static EspecialidadResponse from(Especialidad entity) {
        return new EspecialidadResponse(entity.getIdEspecialidad(), entity.getNombre(), entity.getDescripcion(), entity.getActivo());
    }
}
