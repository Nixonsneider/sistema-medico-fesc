package com.nxn.sistemawebmedico.dto;

import com.nxn.sistemawebmedico.entity.Diagnostico;
import com.nxn.sistemawebmedico.enums.TipoDiagnostico;
public record DiagnosticoSummary(Integer idDiagnostico, Integer idCita, String codigoCie10,
        TipoDiagnostico tipo, PersonaOption paciente, String fechaCita) {
    public static DiagnosticoSummary from(Diagnostico d) {
        var p=d.getCita().getPaciente();
        return new DiagnosticoSummary(d.getIdDiagnostico(),d.getCita().getIdCita(),d.getCodigoCie10(),d.getTipo(),
                new PersonaOption(p.getIdPaciente(),p.getNombre()+" "+p.getApellido(),p.getDocumento()),d.getCita().getFechaHora().toString());
    }
}
