package com.nxn.sistemawebmedico.service;

import com.nxn.sistemawebmedico.dto.EspecialidadRequest;
import com.nxn.sistemawebmedico.dto.EspecialidadResponse;
import com.nxn.sistemawebmedico.dto.MedicoRequest;
import com.nxn.sistemawebmedico.dto.MedicoResponse;
import com.nxn.sistemawebmedico.error.ApplicationException;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/** Usa exclusivamente una base de pruebas. Limpia solo los registros creados por cada prueba. */
@EnabledIfSystemProperty(named = "medico.test.jdbcUrl", matches = ".+")
class DirectoryServiceTest {
    private static EntityManagerFactory factory;
    private EspecialidadService specialties;
    private MedicoService doctors;
    private final List<Integer> specialtyIds = new ArrayList<>();
    private final List<Integer> doctorIds = new ArrayList<>();
    private final String prefix = UUID.randomUUID().toString().substring(0, 10);

    @BeforeAll
    static void connect() {
        factory = Persistence.createEntityManagerFactory("my_persistence_unit", Map.of(
                "jakarta.persistence.jdbc.url", System.getProperty("medico.test.jdbcUrl"),
                "jakarta.persistence.jdbc.user", System.getProperty("medico.test.user", "postgres"),
                "jakarta.persistence.jdbc.password", System.getenv().getOrDefault("MEDICO_TEST_DB_PASSWORD", ""),
                "jakarta.persistence.jdbc.driver", "org.postgresql.Driver",
                "hibernate.hbm2ddl.auto", "validate"));
    }

    @BeforeEach
    void services() { specialties = new EspecialidadService(factory); doctors = new MedicoService(factory); }

    @AfterEach
    void cleanup() {
        try (var manager = factory.createEntityManager()) {
            manager.getTransaction().begin();
            if (!doctorIds.isEmpty()) manager.createQuery("delete from Medico m where m.idMedico in :ids").setParameter("ids", doctorIds).executeUpdate();
            if (!specialtyIds.isEmpty()) manager.createQuery("delete from Especialidad e where e.idEspecialidad in :ids").setParameter("ids", specialtyIds).executeUpdate();
            manager.getTransaction().commit();
        }
    }

    @AfterAll
    static void close() { if (factory != null) factory.close(); }

    private EspecialidadResponse specialty(String suffix, boolean active) {
        var result = specialties.save(null, new EspecialidadRequest(prefix + suffix, null, active));
        specialtyIds.add(result.idEspecialidad());
        return result;
    }

    private MedicoRequest input(int specialty, String suffix) {
        return new MedicoRequest(prefix + suffix, "Ana", "Prueba", "RM" + prefix + suffix,
                "3001234567", prefix + suffix + "@example.test", specialty);
    }

    private MedicoResponse doctor(int specialty, String suffix) {
        var result = doctors.save(null, input(specialty, suffix));
        doctorIds.add(result.idMedico());
        return result;
    }

    @Test
    void registraConsultaYEditaDatosSinDuplicarseASiMismo() {
        var specialty = specialty(" activa", true);
        var doctor = doctor(specialty.idEspecialidad(), "a");
        var edited = doctors.save(doctor.idMedico(), new MedicoRequest(doctor.documento(), "Ana Maria", doctor.apellido(),
                doctor.registroMedico(), doctor.telefono(), doctor.email(), specialty.idEspecialidad()));
        assertEquals("Ana Maria", doctors.get(edited.idMedico()).nombre());
        assertEquals(specialty.idEspecialidad(), edited.especialidad().idEspecialidad());
    }

    @Test
    void inactivarConservaAsignacionesPeroBloqueaAltasYReasignaciones() {
        var original = specialty(" original", true);
        var other = specialty(" otra", true);
        var assigned = doctor(original.idEspecialidad(), "a");
        var unassigned = doctor(other.idEspecialidad(), "b");
        specialties.save(original.idEspecialidad(), new EspecialidadRequest(original.nombre(), null, false));
        assertFalse(doctors.get(assigned.idMedico()).especialidad().activo());
        assertEquals(assigned.idMedico(), doctors.save(assigned.idMedico(), input(original.idEspecialidad(), "a")).idMedico());
        assertEquals(400, assertThrows(ApplicationException.class,
                () -> doctors.save(null, input(original.idEspecialidad(), "c"))).getError().status());
        assertEquals(400, assertThrows(ApplicationException.class,
                () -> doctors.save(unassigned.idMedico(), input(original.idEspecialidad(), "b"))).getError().status());
        assertEquals(other.idEspecialidad(), doctors.get(unassigned.idMedico()).especialidad().idEspecialidad());
    }

    @Test
    void validaDuplicadosPorCampoYNoGuardaElRegistroFallido() {
        var specialty = specialty(" activa", true);
        var first = doctor(specialty.idEspecialidad(), "a");
        var errors = assertThrows(ApplicationException.class,
                () -> doctors.save(null, input(specialty.idEspecialidad(), "a"))).getError();
        assertEquals(409, errors.status());
        assertEquals(3, errors.fields().size());
        assertTrue(errors.fields().keySet().containsAll(List.of("documento", "registroMedico", "email")));
        assertEquals(1, doctors.list(prefix, null, 1, 20).total());
        assertEquals(first.idMedico(), doctors.list(prefix, null, 1, 20).items().get(0).idMedico());
        assertEquals(409, assertThrows(ApplicationException.class,
                () -> specialties.save(null, new EspecialidadRequest(specialty.nombre(), null, true))).getError().status());
    }

    @Test
    void filtraPaginaYBuscaCaracteresLiterales() {
        var first = specialty(" %uno", true);
        specialty(" dos", false);
        var page = specialties.list(prefix, null, 1, 1);
        assertEquals(2, page.total());
        assertEquals(1, page.items().size());
        assertNotEquals(page.items().get(0).idEspecialidad(), specialties.list(prefix, null, 2, 1).items().get(0).idEspecialidad());
        assertEquals(1, specialties.list(prefix, true, 1, 20).total());
        assertEquals(first.idEspecialidad(), specialties.list(prefix + " %", null, 1, 20).items().get(0).idEspecialidad());
        doctor(first.idEspecialidad(), "a");
        assertEquals(1, doctors.list(prefix, first.idEspecialidad(), 1, 20).total());
    }

    @Test
    void rechazaDatosInvalidosReferenciasInexistentesYDevuelve404() {
        var invalid = assertThrows(ApplicationException.class,
                () -> doctors.save(null, new MedicoRequest(" ", "", "", "", "", "no-es-email", null))).getError();
        assertEquals(400, invalid.status());
        assertTrue(invalid.fields().containsKey("email"));
        assertTrue(invalid.fields().containsKey("idEspecialidad"));
        assertEquals(400, assertThrows(ApplicationException.class,
                () -> doctors.save(null, input(Integer.MAX_VALUE, "a"))).getError().status());
        assertEquals(404, assertThrows(ApplicationException.class, () -> doctors.get(Integer.MAX_VALUE)).getError().status());
        assertEquals(400, assertThrows(ApplicationException.class, () -> specialties.list("", null, 0, 20)).getError().status());
    }
}

