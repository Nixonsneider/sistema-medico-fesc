package com.nxn.sistemawebmedico.dto;

import com.nxn.sistemawebmedico.entity.Diagnostico;
import com.nxn.sistemawebmedico.enums.TipoDiagnostico;
public record DiagnosticoResponse(Integer idDiagnostico, Integer idCita, String codigoCie10, String descripcion,
        TipoDiagnostico tipo, PersonaOption paciente, String fechaCita) {
    public static DiagnosticoResponse from(Diagnostico d) {
        var summary=DiagnosticoSummary.from(d);
        return new DiagnosticoResponse(summary.idDiagnostico(),summary.idCita(),summary.codigoCie10(),d.getDescripcion(),
                summary.tipo(),summary.paciente(),summary.fechaCita());
    }
}
