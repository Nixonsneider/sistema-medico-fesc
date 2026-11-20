package com.nxn.sistemawebmedico;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Configura los endpoints y los proveedores comunes de la API.
 */
@ApplicationPath("resources")
public class JakartaRestConfiguration extends ResourceConfig {
    public JakartaRestConfiguration() {
        packages("com.nxn.sistemawebmedico.resources", "com.nxn.sistemawebmedico.error");
        register(JacksonFeature.withoutExceptionMappers());
    }
}
