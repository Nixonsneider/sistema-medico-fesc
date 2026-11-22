package com.nxn.sistemawebmedico.dto;

import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.DefaultValue;

public class ReportFilter {
    @QueryParam("tipo") @DefaultValue("agenda") public String tipo="agenda";
    @QueryParam("desde") public String desde;
    @QueryParam("hasta") public String hasta;
    @QueryParam("idMedico") public Integer idMedico;
    @QueryParam("idEspecialidad") public Integer idEspecialidad;
    @QueryParam("estadoCita") public String estadoCita;
    @QueryParam("estadoOrden") public String estadoOrden;
    @QueryParam("idPaciente") public Integer idPaciente;
    @QueryParam("page") @DefaultValue("1") public int page=1;
    @QueryParam("pageSize") @DefaultValue("20") public int pageSize=20;
}
