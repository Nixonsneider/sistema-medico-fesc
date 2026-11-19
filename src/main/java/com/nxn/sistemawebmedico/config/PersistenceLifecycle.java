package com.nxn.sistemawebmedico.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/** Inicializa una única fábrica JPA y cierra sus recursos al detener la aplicación. */
@WebListener
public class PersistenceLifecycle implements ServletContextListener {
    public static final String ENTITY_MANAGER_FACTORY = EntityManagerFactory.class.getName();
    private EntityManagerFactory factory;
    private HikariDataSource dataSource;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        String location = System.getProperty("medico.db.config");
        if (location == null || location.isBlank()) {
            location = System.getenv("MEDICO_DB_CONFIG");
        }
        if (location == null || location.isBlank()) {
            throw new IllegalStateException("Defina MEDICO_DB_CONFIG o -Dmedico.db.config con la ruta del archivo de conexión.");
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(Path.of(location))) {
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el archivo externo de conexión.");
        }
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(required(properties, "db.url"));
        config.setUsername(required(properties, "db.user"));
        config.setPassword(required(properties, "db.password"));
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setConnectionInitSql("SET TIME ZONE 'America/Bogota'");
        config.setPoolName("sistemaWebMedico");
        try {
            dataSource = new HikariDataSource(config);
            factory = Persistence.createEntityManagerFactory("my_persistence_unit",
                    Map.of("jakarta.persistence.nonJtaDataSource", dataSource));
            event.getServletContext().setAttribute(ENTITY_MANAGER_FACTORY, factory);
        } catch (RuntimeException exception) {
            close();
            event.getServletContext().log("Fallo de inicialización de persistencia: " + exception.getClass().getName());
            // No incluir mensajes del controlador que puedan contener datos de conexión.
            throw new IllegalStateException("No se pudo inicializar PostgreSQL/JPA. Revise el archivo externo y el servicio de base de datos.");
        }
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank() || "CAMBIAR_LOCALMENTE".equals(value)) {
            throw new IllegalStateException("Configure la propiedad " + key + " en el archivo externo.");
        }
        return value;
    }

    public static EntityManagerFactory getFactory(ServletContext context) {
        return (EntityManagerFactory) context.getAttribute(ENTITY_MANAGER_FACTORY);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        event.getServletContext().removeAttribute(ENTITY_MANAGER_FACTORY);
        close();
    }

    private void close() {
        if (factory != null && factory.isOpen()) {
            factory.close();
        }
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
