package com.nxn.sistemawebmedico.resources;

import com.nxn.sistemawebmedico.config.PersistenceLifecycle;
import com.nxn.sistemawebmedico.dto.*;
import com.nxn.sistemawebmedico.service.*;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.logging.Logger;

@Path("reportes")
public class ReportResource {
    private static final Logger AUDIT=Logger.getLogger("medico.auditoria.exportaciones");
    @Context private ServletContext context;
    @Context private HttpServletRequest request;
    private ReportService service() { return new ReportService(PersistenceLifecycle.getFactory(context)); }
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response generate(@BeanParam ReportFilter filter) { return Response.ok(service().generate(filter,false)).header("Cache-Control","no-store").build(); }
    @GET @Path("exportar")
    public Response export(@BeanParam ReportFilter filter,@QueryParam("formato") @DefaultValue("csv") String format) {
        if(!java.util.List.of("csv","xlsx","pdf").contains(format)) throw com.nxn.sistemawebmedico.error.ApplicationException.invalidData(java.util.Map.of("formato","Selecciona CSV, Excel o PDF."));
        var report=service().generate(filter,true); byte[] data=ReportExport.generate(report,format);
        String actor=request.getUserPrincipal()==null?"SIN_AUTENTICACION":request.getUserPrincipal().getName().replaceAll("[\\r\\n]","");
        AUDIT.info("EXPORTACION_GENERADA actor="+actor+" fecha="+java.time.Instant.now()+" reporte="+filter.tipo+" formato="+format+" filas="+report.total());
        String mime=switch(format) { case "pdf" -> "application/pdf"; case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"; default -> "text/csv; charset=UTF-8"; };
        return Response.ok(data,mime).header("Content-Disposition","attachment; filename=\"reporte-"+filter.tipo+"."+format+"\"").header("Cache-Control","no-store").header("X-Content-Type-Options","nosniff").build();
    }
}
