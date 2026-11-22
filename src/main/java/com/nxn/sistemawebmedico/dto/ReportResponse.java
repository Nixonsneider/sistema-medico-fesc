package com.nxn.sistemawebmedico.dto;

import java.util.List;
import java.util.Map;

public record ReportResponse(String titulo, List<String> columnas, List<List<String>> filas,
        long total, int page, int pageSize, Map<String,String> indicadores, List<Bar> grafico, String alcance) {
    public record Bar(String etiqueta,long cantidad) {}
}
