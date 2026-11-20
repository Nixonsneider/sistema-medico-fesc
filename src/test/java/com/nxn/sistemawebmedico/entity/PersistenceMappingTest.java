package com.nxn.sistemawebmedico.entity;

import jakarta.persistence.Persistence;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistenceMappingTest {
    @Test
    void unidadDePersistenciaReconoceLasNueveEntidadesSinConexion() {
        try (var factory = Persistence.createEntityManagerFactory("my_persistence_unit", Map.of(
                "hibernate.boot.allow_jdbc_metadata_access", false,
                "hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect"))) {
            assertEquals(9, factory.getMetamodel().getEntities().size());
        }
    }
}
