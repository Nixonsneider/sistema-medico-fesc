package com.nxn.sistemawebmedico.dto;

import com.nxn.sistemawebmedico.entity.DetalleOrden;
import java.util.List;

public record OrdenResponse(OrdenSummary orden, String observaciones, List<Detalle> detalles) {
    public record Detalle(Integer idDetalle, String descripcion, Integer cantidad, String indicaciones) {
        public static Detalle from(DetalleOrden detail) {
            return new Detalle(detail.getIdDetalle(),detail.getDescripcion(),detail.getCantidad(),detail.getIndicaciones());
        }
    }
}
