package com.nxn.sistemawebmedico.entity;

import com.nxn.sistemawebmedico.enums.EstadoCita;
import com.nxn.sistemawebmedico.enums.EstadoOrden;
import com.nxn.sistemawebmedico.enums.TipoDiagnostico;
import com.nxn.sistemawebmedico.enums.TipoOrden;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/** Solo se ejecuta al indicar una base de pruebas independiente con el esquema creado. */
@EnabledIfSystemProperty(named = "medico.test.jdbcUrl", matches = ".+")
class PostgresPersistenceTest {
    private static EntityManagerFactory factory;
    private EntityManager manager;

    @BeforeAll
    static void conectar() {
        factory = Persistence.createEntityManagerFactory("my_persistence_unit", Map.of(
                "jakarta.persistence.jdbc.url", System.getProperty("medico.test.jdbcUrl"),
                "jakarta.persistence.jdbc.user", System.getProperty("medico.test.user", "postgres"),
                "jakarta.persistence.jdbc.password", System.getenv().getOrDefault("MEDICO_TEST_DB_PASSWORD", ""),
                "jakarta.persistence.jdbc.driver", "org.postgresql.Driver",
                "hibernate.hbm2ddl.auto", "validate"));
    }

    @AfterAll
    static void cerrarFabrica() {
        if (factory != null) {
            factory.close();
        }
    }

    @BeforeEach
    void comenzar() {
        manager = factory.createEntityManager();
        manager.getTransaction().begin();
    }

    @AfterEach
    void revertir() {
        if (manager != null) {
            if (manager.getTransaction().isActive()) {
                manager.getTransaction().rollback();
            }
            manager.close();
        }
    }

    @Test
    void guardaYRecuperaElRecorridoClinicoConEnumsYFechas() {
        DetalleOrden detalle = registrarRecorrido();
        Integer id = detalle.getIdDetalle();
        Integer pacienteId = detalle.getOrden().getCita().getPaciente().getIdPaciente();
        manager.flush();
        manager.clear();

        DetalleOrden recuperado = manager.find(DetalleOrden.class, id);
        Orden orden = recuperado.getOrden();
        Cita cita = orden.getCita();
        assertEquals("Hemograma", recuperado.getDescripcion());
        assertEquals(1, recuperado.getCantidad());
        assertEquals(TipoOrden.LABORATORIO, orden.getTipoOrden());
        assertEquals(EstadoOrden.PENDIENTE, orden.getEstado());
        assertEquals(LocalDate.now(ZoneId.of("America/Bogota")), orden.getFechaOrden());
        assertEquals(EstadoCita.PROGRAMADA, cita.getEstadoCita());
        assertEquals(OffsetDateTime.parse("2027-01-15T09:30:00-05:00").toInstant(), cita.getFechaHora().toInstant());
        assertTrue(cita.getMedico().getEspecialidad().getNombre().startsWith("Medicina general "));
        assertEquals(LocalDate.of(1995, 4, 10), cita.getPaciente().getFechaNacimiento());
        assertEquals(pacienteId, cita.getPaciente().getIdPaciente());

        Tratamiento tratamiento = manager.createQuery(
                "select t from Tratamiento t where t.diagnostico.cita.idCita = :cita", Tratamiento.class)
                .setParameter("cita", cita.getIdCita()).getSingleResult();
        assertEquals(TipoDiagnostico.PRESUNTIVO, tratamiento.getDiagnostico().getTipo());
        assertEquals("R51", tratamiento.getDiagnostico().getCodigoCie10());
        assertEquals("PROGRAMADA", manager.createNativeQuery(
                "select estado_cita from cita where id_cita = :id", String.class)
                .setParameter("id", cita.getIdCita()).getSingleResult());

        HistoriaClinica historia = manager.createQuery(
                "select h from HistoriaClinica h where h.paciente.idPaciente = :id", HistoriaClinica.class)
                .setParameter("id", pacienteId).getSingleResult();
        assertEquals("O+", historia.getGrupoSanguineo());
        assertEquals(LocalDate.now(ZoneId.of("America/Bogota")), historia.getFechaApertura());
    }

    @Test
    void eliminarUnaCitaNoBorraSusDatosClinicosEnCascada() {
        Cita cita = registrarRecorrido().getOrden().getCita();
        manager.flush();
        manager.clear();
        Cita cargada = manager.find(Cita.class, cita.getIdCita());
        assertThrows(PersistenceException.class, () -> {
            manager.remove(cargada);
            manager.flush();
        });
    }

    @Test
    void unPacienteNoPuedeTenerDosHistoriasClinicas() {
        Paciente paciente = registrarRecorrido().getOrden().getCita().getPaciente();
        HistoriaClinica duplicada = new HistoriaClinica();
        duplicada.setPaciente(paciente);
        duplicada.setGrupoSanguineo("O+");
        assertThrows(PersistenceException.class, () -> {
            manager.persist(duplicada);
            manager.flush();
        });
    }

    private DetalleOrden registrarRecorrido() {
        String codigo = UUID.randomUUID().toString().substring(0, 12);
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre("Medicina general " + codigo);
        manager.persist(especialidad);

        Medico medico = new Medico();
        medico.setDocumento("M" + codigo);
        medico.setNombre("Medico prueba");
        medico.setApellido("Prueba");
        medico.setRegistroMedico(codigo);
        medico.setTelefono("3000000000");
        medico.setEmail("medico" + codigo + "@example.test");
        medico.setEspecialidad(especialidad);
        manager.persist(medico);

        Paciente paciente = new Paciente();
        paciente.setTipoDocumento("CC");
        paciente.setDocumento("P" + codigo);
        paciente.setNombre("Paciente prueba");
        paciente.setApellido("Prueba");
        paciente.setFechaNacimiento(LocalDate.of(1995, 4, 10));
        paciente.setSexo("Femenino");
        paciente.setDireccion("Direccion de prueba");
        paciente.setTelefono("3000000001");
        paciente.setEmail("paciente" + codigo + "@example.test");
        manager.persist(paciente);

        HistoriaClinica historia = new HistoriaClinica();
        historia.setPaciente(paciente);
        historia.setGrupoSanguineo("O+");
        manager.persist(historia);

        Cita cita = new Cita();
        cita.setPaciente(paciente);
        cita.setMedico(medico);
        cita.setFechaHora(OffsetDateTime.parse("2027-01-15T09:30:00-05:00"));
        cita.setMotivo("Consulta de prueba");
        manager.persist(cita);

        Diagnostico diagnostico = new Diagnostico();
        diagnostico.setCita(cita);
        diagnostico.setCodigoCie10("R51");
        diagnostico.setDescripcion("Diagnostico de prueba");
        manager.persist(diagnostico);

        Tratamiento tratamiento = new Tratamiento();
        tratamiento.setDiagnostico(diagnostico);
        tratamiento.setMedicamento("Medicamento de prueba");
        tratamiento.setDosis("Dosis de prueba");
        tratamiento.setFrecuencia("Frecuencia de prueba");
        tratamiento.setDuracionDias(3);
        manager.persist(tratamiento);

        Orden orden = new Orden();
        orden.setCita(cita);
        orden.setTipoOrden(TipoOrden.LABORATORIO);
        orden.setObservaciones("Orden de prueba");
        manager.persist(orden);

        DetalleOrden detalle = new DetalleOrden();
        detalle.setOrden(orden);
        detalle.setDescripcion("Hemograma");
        manager.persist(detalle);
        return detalle;
    }
}
