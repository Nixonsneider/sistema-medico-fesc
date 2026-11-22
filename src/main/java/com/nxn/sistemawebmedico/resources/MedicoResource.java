package com.nxn.sistemawebmedico.resources;

import com.nxn.sistemawebmedico.config.PersistenceLifecycle;
import com.nxn.sistemawebmedico.dto.MedicoRequest;
import com.nxn.sistemawebmedico.dto.MedicoResponse;
import com.nxn.sistemawebmedico.dto.Page;
import com.nxn.sistemawebmedico.service.MedicoService;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("medicos")
@Produces(MediaType.APPLICATION_JSON)
public class MedicoResource {
    @Context
    private ServletContext context;
    @Context
    private UriInfo uri;

    private MedicoService service() {
        return new MedicoService(PersistenceLifecycle.getFactory(context));
    }

    @GET
    public Page<MedicoResponse> list(@QueryParam("q") String query,
            @QueryParam("idEspecialidad") Integer filter,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("20") int size) {
        return service().list(query, filter, page, size);
    }

    @GET
    @Path("opciones")
    public java.util.List<com.nxn.sistemawebmedico.dto.PersonaOption> options(@QueryParam("q") String query) { return service().options(query); }

    @GET
    @Path("{id}")
    public MedicoResponse get(@PathParam("id") int id) { return service().get(id); }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(MedicoRequest data) {
        MedicoResponse saved = service().save(null, data);
        return Response.created(uri.getAbsolutePathBuilder().path(saved.idMedico().toString()).build()).entity(saved).build();
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public MedicoResponse update(@PathParam("id") int id, MedicoRequest data) {
        return service().save(id, data);
    }
}
