package com.nxn.sistemawebmedico.dto;

import com.nxn.sistemawebmedico.entity.HistoriaClinica;

public record HistoriaResponse(Integer idHistoria, Integer idPaciente, String fechaApertura,
        String grupoSanguineo, String antecedentes, String alergias) {
    public static HistoriaResponse from(HistoriaClinica h) {
        return new HistoriaResponse(h.getIdHistoria(), h.getPaciente().getIdPaciente(), h.getFechaApertura().toString(),
                h.getGrupoSanguineo(), h.getAntecedentes(), h.getAlergias());
    }
}
