package com.nxn.sistemawebmedico.resources;

import com.nxn.sistemawebmedico.config.PersistenceLifecycle;
import com.nxn.sistemawebmedico.dto.*;
import com.nxn.sistemawebmedico.service.DiagnosticoService;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("diagnosticos")
@Produces(MediaType.APPLICATION_JSON)
public class DiagnosticoResource {
    @Context private ServletContext context;
    @Context private UriInfo uri;
    private DiagnosticoService service() { return new DiagnosticoService(PersistenceLifecycle.getFactory(context)); }
    @GET public Page<DiagnosticoSummary> list(@QueryParam("q") String q,@QueryParam("idCita") Integer parent,
            @QueryParam("page") @DefaultValue("1") int page,@QueryParam("pageSize") @DefaultValue("20") int size) {
        return service().list(q,parent,page,size);
    }
    @GET @Path("opciones") public java.util.List<ClinicalOption> options(@QueryParam("q") String q) { return service().options(q); }
    @GET @Path("{id}") public DiagnosticoResponse get(@PathParam("id") int id) { return service().get(id); }
    @POST @Consumes(MediaType.APPLICATION_JSON) public Response create(DiagnosticoRequest data) {
        var saved=service().save(null,data);
        return Response.created(uri.getAbsolutePathBuilder().path(saved.idDiagnostico().toString()).build()).entity(saved).build();
    }
    @PUT @Path("{id}") @Consumes(MediaType.APPLICATION_JSON) public DiagnosticoResponse update(@PathParam("id") int id,DiagnosticoRequest data) { return service().save(id,data); }
}
