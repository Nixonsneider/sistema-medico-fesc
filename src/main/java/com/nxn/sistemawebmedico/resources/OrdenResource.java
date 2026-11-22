package com.nxn.sistemawebmedico.resources;

import com.nxn.sistemawebmedico.config.PersistenceLifecycle;
import com.nxn.sistemawebmedico.dto.*;
import com.nxn.sistemawebmedico.enums.EstadoOrden;
import com.nxn.sistemawebmedico.service.OrdenService;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("ordenes")
@Produces(MediaType.APPLICATION_JSON)
public class OrdenResource {
    @Context private ServletContext context;
    @Context private UriInfo uri;
    private OrdenService service() { return new OrdenService(PersistenceLifecycle.getFactory(context)); }
    @GET public Page<OrdenSummary> list(@QueryParam("q") String q,@QueryParam("idCita") Integer cita,@QueryParam("estado") EstadoOrden state,
            @QueryParam("page") @DefaultValue("1") int page,@QueryParam("pageSize") @DefaultValue("20") int size) { return service().list(q,cita,state,page,size); }
    @GET @Path("{id}") public OrdenResponse get(@PathParam("id") int id) { return service().get(id); }
    @POST @Consumes(MediaType.APPLICATION_JSON) public Response create(OrdenRequest data) {
        var saved=service().create(data);
        return Response.created(uri.getAbsolutePathBuilder().path(saved.orden().idOrden().toString()).build()).entity(saved).build();
    }
    @PATCH @Path("{id}/estado") @Consumes(MediaType.APPLICATION_JSON)
    public OrdenResponse state(@PathParam("id") int id,EstadoOrdenRequest data) { return service().state(id,data); }
}
