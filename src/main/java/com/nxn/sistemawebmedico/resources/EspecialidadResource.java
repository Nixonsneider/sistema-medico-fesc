package com.nxn.sistemawebmedico.resources;

import com.nxn.sistemawebmedico.config.PersistenceLifecycle;
import com.nxn.sistemawebmedico.dto.EspecialidadRequest;
import com.nxn.sistemawebmedico.dto.EspecialidadResponse;
import com.nxn.sistemawebmedico.dto.Page;
import com.nxn.sistemawebmedico.service.EspecialidadService;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("especialidades")
@Produces(MediaType.APPLICATION_JSON)
public class EspecialidadResource {
    @Context
    private ServletContext context;
    @Context
    private UriInfo uri;

    private EspecialidadService service() {
        return new EspecialidadService(PersistenceLifecycle.getFactory(context));
    }

    @GET
    public Page<EspecialidadResponse> list(@QueryParam("q") String query,
            @QueryParam("activo") Boolean filter,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int size) {
        return service().list(query, filter, page, size);
    }

    @GET
    @Path("{id}")
    public EspecialidadResponse get(@PathParam("id") int id) { return service().get(id); }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(EspecialidadRequest data) {
        EspecialidadResponse saved = service().save(null, data);
        return Response.created(uri.getAbsolutePathBuilder().path(saved.idEspecialidad().toString()).build()).entity(saved).build();
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public EspecialidadResponse update(@PathParam("id") int id, EspecialidadRequest data) {
        return service().save(id, data);
    }
}
