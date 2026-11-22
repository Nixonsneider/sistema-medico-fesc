package com.nxn.sistemawebmedico.resources;

import com.nxn.sistemawebmedico.config.PersistenceLifecycle;
import com.nxn.sistemawebmedico.dto.*;
import com.nxn.sistemawebmedico.service.PacienteService;
import com.nxn.sistemawebmedico.service.HistoriaService;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

@Path("pacientes")
@Produces(MediaType.APPLICATION_JSON)
public class PacienteResource {
    @Context private ServletContext context;
    @Context private UriInfo uri;
    private PacienteService service() { return new PacienteService(PersistenceLifecycle.getFactory(context)); }
    private HistoriaService histories() { return new HistoriaService(PersistenceLifecycle.getFactory(context)); }
    @GET public Page<PacienteSummary> list(@QueryParam("q") String q,@QueryParam("page") @DefaultValue("1") int page,@QueryParam("pageSize") @DefaultValue("20") int size) { return service().list(q,page,size); }
    @GET @Path("opciones") public List<PersonaOption> options(@QueryParam("q") String q) { return service().options(q); }
    @GET @Path("{id}") public PacienteResponse get(@PathParam("id") int id) { return service().get(id); }
    @POST @Consumes(MediaType.APPLICATION_JSON) public Response create(PacienteRequest data) {
        var saved=service().save(null,data);
        return Response.created(uri.getAbsolutePathBuilder().path(saved.idPaciente().toString()).build()).entity(saved).build();
    }
    @PUT @Path("{id}") @Consumes(MediaType.APPLICATION_JSON) public PacienteResponse update(@PathParam("id") int id,PacienteRequest data) { return service().save(id,data); }
    @GET @Path("{id}/historia") public Response history(@PathParam("id") int id) {
        var history=histories().get(id);
        return history==null?Response.noContent().build():Response.ok(history).build();
    }
    @POST @Path("{id}/historia") @Consumes(MediaType.APPLICATION_JSON) public Response createHistory(@PathParam("id") int id,HistoriaRequest data) {
        var saved=histories().save(id,data,true);
        return Response.created(uri.getAbsolutePath()).entity(saved).build();
    }
    @PUT @Path("{id}/historia") @Consumes(MediaType.APPLICATION_JSON) public HistoriaResponse updateHistory(@PathParam("id") int id,HistoriaRequest data) { return histories().save(id,data,false); }
}
