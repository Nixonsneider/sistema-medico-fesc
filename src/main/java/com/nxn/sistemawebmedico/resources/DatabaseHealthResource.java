package com.nxn.sistemawebmedico.resources;

import com.nxn.sistemawebmedico.config.PersistenceLifecycle;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Comprobación de conexión sin exponer credenciales ni datos clínicos. */
@Path("health/db")
public class DatabaseHealthResource {
    @Context
    private ServletContext context;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response check() {
        EntityManagerFactory factory = PersistenceLifecycle.getFactory(context);
        if (factory != null && factory.isOpen()) {
            try (EntityManager manager = factory.createEntityManager()) {
                manager.createNativeQuery("SELECT 1", Integer.class).getSingleResult();
                return Response.ok("database OK").build();
            } catch (RuntimeException exception) {
                // La respuesta pública no incluye detalles de la conexión.
            }
        }
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("database unavailable").build();
    }
}
