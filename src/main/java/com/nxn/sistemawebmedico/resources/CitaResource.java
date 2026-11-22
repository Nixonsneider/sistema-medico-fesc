package com.nxn.sistemawebmedico.resources;

import com.nxn.sistemawebmedico.config.PersistenceLifecycle;
import com.nxn.sistemawebmedico.dto.*;
import com.nxn.sistemawebmedico.enums.EstadoCita;
import com.nxn.sistemawebmedico.service.CitaService;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("citas")
@Produces(MediaType.APPLICATION_JSON)
public class CitaResource {
    @Context private ServletContext context;
    @Context private UriInfo uri;
    private CitaService service() { return new CitaService(PersistenceLifecycle.getFactory(context)); }
    @GET public Page<CitaSummary> list(@QueryParam("q") String q,@QueryParam("idPaciente") Integer patient,@QueryParam("idMedico") Integer doctor,
            @QueryParam("estado") EstadoCita state,@QueryParam("desde") String from,@QueryParam("hasta") String to,
            @QueryParam("page") @DefaultValue("1") int page,@QueryParam("pageSize") @DefaultValue("20") int size) {
        return service().list(q,patient,doctor,state,from,to,page,size);
    }
    @GET @Path("{id}") public CitaResponse get(@PathParam("id") int id) { return service().get(id); }
    @POST @Consumes(MediaType.APPLICATION_JSON) public Response create(CitaRequest data) {
        var saved=service().save(null,data);
        return Response.created(uri.getAbsolutePathBuilder().path(saved.idCita().toString()).build()).entity(saved).build();
    }
    @PUT @Path("{id}") @Consumes(MediaType.APPLICATION_JSON) public CitaResponse update(@PathParam("id") int id,CitaRequest data) { return service().save(id,data); }
    @PATCH @Path("{id}/estado") @Consumes(MediaType.APPLICATION_JSON) public CitaResponse state(@PathParam("id") int id,EstadoCitaRequest data) { return service().state(id,data); }
}
