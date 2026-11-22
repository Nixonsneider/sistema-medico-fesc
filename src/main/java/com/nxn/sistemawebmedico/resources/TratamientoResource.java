package com.nxn.sistemawebmedico.resources;

import com.nxn.sistemawebmedico.config.PersistenceLifecycle;
import com.nxn.sistemawebmedico.dto.*;
import com.nxn.sistemawebmedico.service.TratamientoService;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("tratamientos")
@Produces(MediaType.APPLICATION_JSON)
public class TratamientoResource {
    @Context private ServletContext context;
    @Context private UriInfo uri;
    private TratamientoService service() { return new TratamientoService(PersistenceLifecycle.getFactory(context)); }
    @GET public Page<TratamientoResponse> list(@QueryParam("q") String q,@QueryParam("idDiagnostico") Integer parent,
            @QueryParam("page") @DefaultValue("1") int page,@QueryParam("pageSize") @DefaultValue("20") int size) {
        return service().list(q,parent,page,size);
    }
    
    @GET @Path("{id}") public TratamientoResponse get(@PathParam("id") int id) { return service().get(id); }
    @POST @Consumes(MediaType.APPLICATION_JSON) public Response create(TratamientoRequest data) {
        var saved=service().save(null,data);
        return Response.created(uri.getAbsolutePathBuilder().path(saved.idTratamiento().toString()).build()).entity(saved).build();
    }
    @PUT @Path("{id}") @Consumes(MediaType.APPLICATION_JSON) public TratamientoResponse update(@PathParam("id") int id,TratamientoRequest data) { return service().save(id,data); }
}
