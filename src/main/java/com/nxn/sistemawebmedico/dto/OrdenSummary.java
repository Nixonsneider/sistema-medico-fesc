package com.nxn.sistemawebmedico.dto;

import com.nxn.sistemawebmedico.entity.Orden;
import com.nxn.sistemawebmedico.enums.EstadoOrden;
import com.nxn.sistemawebmedico.enums.TipoOrden;

public record OrdenSummary(Integer idOrden, Integer idCita, TipoOrden tipoOrden, EstadoOrden estado,
        String fechaOrden, PersonaOption paciente) {
    public static OrdenSummary from(Orden order) {
        var patient=order.getCita().getPaciente();
        return new OrdenSummary(order.getIdOrden(),order.getCita().getIdCita(),order.getTipoOrden(),order.getEstado(),
                order.getFechaOrden().toString(),new PersonaOption(patient.getIdPaciente(),patient.getNombre()+" "+patient.getApellido(),patient.getDocumento()));
    }
}
