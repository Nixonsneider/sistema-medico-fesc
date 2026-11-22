package com.nxn.sistemawebmedico.dto;

import java.math.BigDecimal;

public record TratamientoRequest(Integer idDiagnostico, String medicamento, String dosis,
        String frecuencia, BigDecimal duracionDias, String indicaciones) { }
