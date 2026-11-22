package com.nxn.sistemawebmedico.dto;

import com.nxn.sistemawebmedico.entity.Cita;
import com.nxn.sistemawebmedico.enums.EstadoCita;

/** El motivo clínico se consulta en el detalle, no se envía en la agenda completa. */
public record CitaSummary(Integer idCita, String fechaHora, EstadoCita estadoCita,
        PersonaOption paciente, PersonaOption medico) {
    public static CitaSummary from(Cita appointment) {
        CitaResponse detail = CitaResponse.from(appointment);
        return new CitaSummary(detail.idCita(), detail.fechaHora(), detail.estadoCita(), detail.paciente(), detail.medico());
    }
}
