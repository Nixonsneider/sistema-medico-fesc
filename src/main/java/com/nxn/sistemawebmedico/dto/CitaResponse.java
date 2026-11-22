package com.nxn.sistemawebmedico.dto;

import com.nxn.sistemawebmedico.entity.Cita;
import com.nxn.sistemawebmedico.enums.EstadoCita;

public record CitaResponse(Integer idCita, String fechaHora, String motivo, EstadoCita estadoCita,
        PersonaOption paciente, PersonaOption medico) {
    public static CitaResponse from(Cita c) {
        return new CitaResponse(c.getIdCita(), c.getFechaHora().toString(), c.getMotivo(), c.getEstadoCita(),
                new PersonaOption(c.getPaciente().getIdPaciente(), c.getPaciente().getNombre()+" "+c.getPaciente().getApellido(), c.getPaciente().getDocumento()),
                new PersonaOption(c.getMedico().getIdMedico(), c.getMedico().getNombre()+" "+c.getMedico().getApellido(), c.getMedico().getDocumento()));
    }
}
