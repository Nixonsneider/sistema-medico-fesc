package com.nxn.sistemawebmedico.service;

import com.nxn.sistemawebmedico.dto.*;
import com.nxn.sistemawebmedico.enums.EstadoCita;
import com.nxn.sistemawebmedico.error.ApplicationException;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="medico.test.jdbcUrl",matches=".+")
class ClinicalServiceTest {
    private static EntityManagerFactory factory;
    private final String prefix=UUID.randomUUID().toString().substring(0,10);
    private final List<Integer> patients=new ArrayList<>(),doctors=new ArrayList<>();
    private Integer specialty;
    private PacienteService people;
    private HistoriaService histories;
    private CitaService appointments;
    private static final Clock BEFORE=Clock.fixed(Instant.parse("2030-01-01T12:00:00Z"),ZoneId.of("America/Bogota"));
    private static final Clock AFTER=Clock.fixed(Instant.parse("2030-01-03T12:00:00Z"),ZoneId.of("America/Bogota"));
    private static final String SLOT="2030-01-02T09:00:00-05:00";

    @BeforeAll static void connect() {
        factory=Persistence.createEntityManagerFactory("my_persistence_unit",Map.of(
                "jakarta.persistence.jdbc.url",System.getProperty("medico.test.jdbcUrl"),
                "jakarta.persistence.jdbc.user",System.getProperty("medico.test.user","postgres"),
                "jakarta.persistence.jdbc.password",System.getenv().getOrDefault("MEDICO_TEST_DB_PASSWORD",""),
                "jakarta.persistence.jdbc.driver","org.postgresql.Driver","hibernate.hbm2ddl.auto","validate"));
    }
    @BeforeEach void setup() {
        people=new PacienteService(factory); histories=new HistoriaService(factory);appointments=new CitaService(factory,BEFORE);
        specialty=new EspecialidadService(factory).save(null,new EspecialidadRequest(prefix,null,true)).idEspecialidad();
        for(String suffix:List.of("a","b")) {
            doctors.add(new MedicoService(factory).save(null,new MedicoRequest(prefix+suffix,"Medico",suffix,"RM"+prefix+suffix,"3000000000",prefix+suffix+"@med.example.test",specialty)).idMedico());
            patients.add(people.save(null,input(suffix)).idPaciente());
        }
    }
    private PacienteRequest input(String suffix) { return new PacienteRequest("CC",prefix+suffix,"Paciente",suffix,"1990-05-10","Femenino","Direccion prueba","3000000001",prefix+suffix+"@pac.example.test"); }
    private CitaRequest slot(int patient,int doctor,String when) { return new CitaRequest(patient,doctor,when,"Consulta de prueba"); }
    @AfterEach void cleanup() {
        try(var manager=factory.createEntityManager()) {
            manager.getTransaction().begin();
            if(!patients.isEmpty()) {
                manager.createQuery("delete from Cita c where c.paciente.idPaciente in :ids").setParameter("ids",patients).executeUpdate();
                manager.createQuery("delete from HistoriaClinica h where h.paciente.idPaciente in :ids").setParameter("ids",patients).executeUpdate();
                manager.createQuery("delete from Paciente p where p.idPaciente in :ids").setParameter("ids",patients).executeUpdate();
            }
            if(!doctors.isEmpty()) manager.createQuery("delete from Medico m where m.idMedico in :ids").setParameter("ids",doctors).executeUpdate();
            if(specialty!=null) manager.createQuery("delete from Especialidad e where e.idEspecialidad=:id").setParameter("id",specialty).executeUpdate();
            manager.getTransaction().commit();
        }
    }
    @AfterAll static void close() { if(factory!=null) factory.close(); }

    @Test void registraEditaBuscaYRechazaDuplicadosDePaciente() {
        int id=patients.get(0);
        assertEquals(id,people.save(id,input("a")).idPaciente());
        assertEquals(2,people.list(prefix,1,20).total());
        assertEquals(1,people.list(prefix,2,1).items().size());
        var duplicate=assertThrows(ApplicationException.class,()->people.save(null,input("a"))).getError();
        assertEquals(409,duplicate.status()); assertEquals(2,duplicate.fields().size());
        var original=input("a");
        var bad=new PacienteRequest("CC",original.documento(),"Paciente","a","2099-01-01","NoValido","direccion","telefono","no-email");
        var invalid=assertThrows(ApplicationException.class,()->people.save(id,bad)).getError();
        assertTrue(invalid.fields().keySet().containsAll(List.of("fechaNacimiento","sexo","email")));
        assertEquals("1990-05-10",people.get(id).fechaNacimiento());
    }

    @Test void historiaUnicaEditableConFechaDeAperturaConservada() {
        int patient=patients.get(0);assertNull(histories.get(patient));
        var created=histories.save(patient,new HistoriaRequest("O+",null,"Dato de prueba"),true);
        var updated=histories.save(patient,new HistoriaRequest("A+","Antecedentes prueba",null),false);
        assertEquals(created.idHistoria(),updated.idHistoria());assertEquals(created.fechaApertura(),updated.fechaApertura());
        assertEquals("A+",histories.get(patient).grupoSanguineo());
        assertEquals(409,assertThrows(ApplicationException.class,()->histories.save(patient,new HistoriaRequest("O+",null,null),true)).getError().status());
        assertEquals(400,assertThrows(ApplicationException.class,()->histories.save(patient,new HistoriaRequest("X",null,null),false)).getError().status());
    }

    @Test void confirmaReprogramaCancelaYFiltraConLimitesDeDiaBogota() {
        var created=appointments.save(null,slot(patients.get(0),doctors.get(0),SLOT));
        assertEquals(EstadoCita.PROGRAMADA,created.estadoCita());
        appointments.state(created.idCita(),new EstadoCitaRequest(EstadoCita.CONFIRMADA));
        var moved=appointments.save(created.idCita(),slot(patients.get(0),doctors.get(0),"2030-01-02T10:00:00-05:00"));
        assertEquals(EstadoCita.PROGRAMADA,moved.estadoCita());
        assertEquals(1,appointments.list("",patients.get(0),doctors.get(0),null,"2030-01-02","2030-01-02",1,20).total());
        assertEquals(0,appointments.list("",patients.get(0),null,null,"2030-01-03",null,1,20).total());
        appointments.state(created.idCita(),new EstadoCitaRequest(EstadoCita.CANCELADA));
        assertEquals(409,assertThrows(ApplicationException.class,()->appointments.state(created.idCita(),new EstadoCitaRequest(EstadoCita.CONFIRMADA))).getError().status());
        assertEquals(409,assertThrows(ApplicationException.class,()->appointments.save(created.idCita(),slot(patients.get(0),doctors.get(0),SLOT))).getError().status());
    }

    @Test void bloqueaCrucesDePacienteYMedicoYLiberaAlCancelar() {
        var first=appointments.save(null,slot(patients.get(0),doctors.get(0),SLOT));
        assertEquals(409,assertThrows(ApplicationException.class,()->appointments.save(null,slot(patients.get(1),doctors.get(0),SLOT))).getError().status());
        assertEquals(409,assertThrows(ApplicationException.class,()->appointments.save(null,slot(patients.get(0),doctors.get(1),"2030-01-02T14:00:00Z"))).getError().status());
        appointments.state(first.idCita(),new EstadoCitaRequest(EstadoCita.CANCELADA));
        assertNotEquals(first.idCita(),appointments.save(null,slot(patients.get(0),doctors.get(0),SLOT)).idCita());
    }

    @Test void atencionEInasistenciaNoSeAdelantanYNoPuedenReabrirse() {
        var first=appointments.save(null,slot(patients.get(0),doctors.get(0),SLOT));
        assertEquals(400,assertThrows(ApplicationException.class,()->appointments.state(first.idCita(),new EstadoCitaRequest(EstadoCita.ATENDIDA))).getError().status());
        var after=new CitaService(factory,AFTER);
        assertEquals(EstadoCita.ATENDIDA,after.state(first.idCita(),new EstadoCitaRequest(EstadoCita.ATENDIDA)).estadoCita());
        assertEquals(409,assertThrows(ApplicationException.class,()->after.state(first.idCita(),new EstadoCitaRequest(EstadoCita.CANCELADA))).getError().status());
        var second=appointments.save(null,slot(patients.get(1),doctors.get(1),SLOT));
        assertEquals(400,assertThrows(ApplicationException.class,()->appointments.state(second.idCita(),new EstadoCitaRequest(EstadoCita.NO_ASISTIO))).getError().status());
        assertEquals(EstadoCita.NO_ASISTIO,after.state(second.idCita(),new EstadoCitaRequest(EstadoCita.NO_ASISTIO)).estadoCita());
    }

    @Test void rechazaFechasReferenciasYFiltrosInvalidos() {
        assertEquals(400,assertThrows(ApplicationException.class,()->appointments.save(null,slot(patients.get(0),doctors.get(0),"2020-01-01T09:00:00-05:00"))).getError().status());
        assertEquals(400,assertThrows(ApplicationException.class,()->appointments.save(null,slot(patients.get(0),doctors.get(0),"2030-01-02T09:00:00"))).getError().status());
        assertEquals(400,assertThrows(ApplicationException.class,()->appointments.save(null,slot(Integer.MAX_VALUE,doctors.get(0),SLOT))).getError().status());
        assertEquals(400,assertThrows(ApplicationException.class,()->appointments.list("",null,null,null,"2030-02-01","2030-01-01",1,20)).getError().status());
        assertEquals(404,assertThrows(ApplicationException.class,()->appointments.get(Integer.MAX_VALUE)).getError().status());
    }

    @Test void dosReservasSimultaneasNoDuplicanLaAgenda() throws Exception {
        var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(1);
        try {
            List<Future<Integer>> results=new ArrayList<>();
            for(int i=0;i<2;i++) { final int patient=patients.get(i);results.add(pool.submit(()->{ready.await();try { appointments.save(null,slot(patient,doctors.get(0),SLOT));return 201; } catch(ApplicationException e) {return e.getError().status();}})); }
            ready.countDown();
            var statuses=new ArrayList<Integer>();for(var result:results) statuses.add(result.get(15,TimeUnit.SECONDS));
            Collections.sort(statuses);assertEquals(List.of(201,409),statuses);
        } finally { pool.shutdownNow(); }
    }
}
