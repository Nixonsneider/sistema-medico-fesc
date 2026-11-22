package com.nxn.sistemawebmedico.dto;

import com.nxn.sistemawebmedico.enums.TipoDiagnostico;
public record DiagnosticoRequest(Integer idCita, String codigoCie10, String descripcion, TipoDiagnostico tipo) { }
