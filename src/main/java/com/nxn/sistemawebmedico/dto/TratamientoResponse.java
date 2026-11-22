package com.nxn.sistemawebmedico.dto;

import com.nxn.sistemawebmedico.entity.Tratamiento;
public record TratamientoResponse(Integer idTratamiento, DiagnosticoSummary diagnostico, String medicamento,
        String dosis, String frecuencia, Integer duracionDias, String indicaciones) {
    public static TratamientoResponse from(Tratamiento t) {
        return new TratamientoResponse(t.getIdTratamiento(),DiagnosticoSummary.from(t.getDiagnostico()),t.getMedicamento(),
                t.getDosis(),t.getFrecuencia(),t.getDuracionDias(),t.getIndicaciones());
    }
}
