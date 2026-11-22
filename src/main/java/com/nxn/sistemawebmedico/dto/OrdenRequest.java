package com.nxn.sistemawebmedico.dto;

import com.nxn.sistemawebmedico.enums.TipoOrden;
import java.math.BigDecimal;
import java.util.List;

public record OrdenRequest(Integer idCita, TipoOrden tipoOrden, String observaciones, List<Detalle> detalles) {
    public record Detalle(String descripcion, BigDecimal cantidad, String indicaciones) {}
}
