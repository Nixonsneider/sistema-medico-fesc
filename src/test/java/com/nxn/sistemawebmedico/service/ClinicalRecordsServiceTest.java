package com.nxn.sistemawebmedico.service;

import com.nxn.sistemawebmedico.dto.*;
import com.nxn.sistemawebmedico.enums.TipoDiagnostico;
import com.nxn.sistemawebmedico.enums.TipoOrden;
import com.nxn.sistemawebmedico.enums.EstadoOrden;
import com.nxn.sistemawebmedico.error.ApplicationException;
import jakarta.persistence.Persistence;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="medico.test.jdbcUrl",matches=".+")
class ClinicalRecordsServiceTest {
    @Test
    void recorridoYValidacionesEsenciales() {
        try(var factory=Persistence.createEntityManagerFactory("my_persistence_unit",Map.of(
                "jakarta.persistence.jdbc.url",System.getProperty("medico.test.jdbcUrl"),
                "jakarta.persistence.jdbc.user",System.getProperty("medico.test.user","postgres"),
                "jakarta.persistence.jdbc.password",System.getenv().getOrDefault("MEDICO_TEST_DB_PASSWORD",""),
                "jakarta.persistence.jdbc.driver","org.postgresql.Driver","hibernate.hbm2ddl.auto","validate"))) {
            String code=UUID.randomUUID().toString().substring(0,10);
            Integer specialty=null,doctor=null;
            List<Integer> patients=new ArrayList<>(),appointments=new ArrayList<>();
            try {
                specialty=new EspecialidadService(factory).save(null,new EspecialidadRequest(code,null,true)).idEspecialidad();
                doctor=new MedicoService(factory).save(null,new MedicoRequest(code,"Medico","Prueba","RM"+code,"3000000000",code+"@med.example.test",specialty)).idMedico();
                for(String suffix:List.of("a","b")) patients.add(new PacienteService(factory).save(null,new PacienteRequest("CC",code+suffix,"Paciente","Prueba","1990-01-01","Otro","Direccion prueba","3000000001",code+suffix+"@pac.example.test")).idPaciente());
                var histories=new HistoriaService(factory);
                histories.save(patients.get(0),new HistoriaRequest("O+","Antecedentes ficticios",null),true);
                assertEquals("O+",histories.get(patients.get(0)).grupoSanguineo());
                assertEquals(409,assertThrows(ApplicationException.class,()->histories.save(patients.get(0),new HistoriaRequest("O+",null,null),true)).getError().status());
                var slots=new CitaService(factory);
                var when=ZonedDateTime.now(ZoneId.of("America/Bogota")).plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0).toOffsetDateTime();
                for(int hour=0;hour<2;hour++) appointments.add(slots.save(null,new CitaRequest(patients.get(0),doctor,when.plusHours(hour).toString(),"Consulta ficticia")).idCita());
                int cita=appointments.get(0),otherCita=appointments.get(1),medico=doctor;
                var diagnostics=new DiagnosticoService(factory);
                var treatments=new TratamientoService(factory);
                var diagnostic=diagnostics.save(null,new DiagnosticoRequest(cita,"PRUEBA","Diagnóstico ficticio",TipoDiagnostico.PRESUNTIVO));
                assertEquals(cita,diagnostic.idCita());
                assertEquals(1,diagnostics.list("PRUEBA",cita,1,20).total());
                assertEquals(TipoDiagnostico.DEFINITIVO,diagnostics.save(diagnostic.idDiagnostico(),new DiagnosticoRequest(cita,"PRUEBA","Descripción editada",TipoDiagnostico.DEFINITIVO)).tipo());
                assertEquals("Descripción editada",diagnostics.get(diagnostic.idDiagnostico()).descripcion());
                var treatment=treatments.save(null,new TratamientoRequest(diagnostic.idDiagnostico(),"Medicamento de prueba","Dosis de prueba","Frecuencia de prueba",BigDecimal.valueOf(3),"Indicaciones ficticias"));
                assertEquals(diagnostic.idDiagnostico(),treatment.diagnostico().idDiagnostico());
                assertEquals(5,treatments.save(treatment.idTratamiento(),new TratamientoRequest(diagnostic.idDiagnostico(),"Medicamento de prueba","Dosis editada","Frecuencia de prueba",BigDecimal.valueOf(5),null)).duracionDias());
                assertEquals(1,treatments.list("",diagnostic.idDiagnostico(),1,20).total());
                assertEquals("Dosis editada",treatments.get(treatment.idTratamiento()).dosis());
                assertEquals(400,assertThrows(ApplicationException.class,()->diagnostics.save(null,new DiagnosticoRequest(cita," ","Texto",TipoDiagnostico.PRESUNTIVO))).getError().status());
                assertEquals(400,assertThrows(ApplicationException.class,()->diagnostics.save(null,new DiagnosticoRequest(Integer.MAX_VALUE,"PRUEBA","Texto",TipoDiagnostico.PRESUNTIVO))).getError().status());
                for(String days:List.of("0","1.5")) assertEquals(400,assertThrows(ApplicationException.class,()->treatments.save(null,new TratamientoRequest(diagnostic.idDiagnostico(),"Prueba","Prueba","Prueba",new BigDecimal(days),null))).getError().status());
                assertEquals(400,assertThrows(ApplicationException.class,()->diagnostics.save(diagnostic.idDiagnostico(),new DiagnosticoRequest(otherCita,"PRUEBA","Texto",TipoDiagnostico.PRESUNTIVO))).getError().status());
                assertEquals(409,assertThrows(ApplicationException.class,()->slots.save(cita,new CitaRequest(patients.get(1),medico,when.toString(),"Consulta ficticia"))).getError().status());
                var orders=new OrdenService(factory);
                var detail=new OrdenRequest.Detalle("Procedimiento ficticio",BigDecimal.ONE,"Indicaciones de prueba");
                var order=orders.create(new OrdenRequest(otherCita,TipoOrden.LABORATORIO,"Observaciones de prueba",List.of(detail,detail)));
                assertEquals(EstadoOrden.PENDIENTE,order.orden().estado());
                assertEquals(2,orders.get(order.orden().idOrden()).detalles().size());
                assertEquals(1,orders.list("Paciente",otherCita,EstadoOrden.PENDIENTE,1,20).total());
                assertEquals(400,assertThrows(ApplicationException.class,()->orders.create(new OrdenRequest(otherCita,TipoOrden.LABORATORIO,"Texto",List.of()))).getError().status());
                assertEquals(400,assertThrows(ApplicationException.class,()->orders.create(new OrdenRequest(Integer.MAX_VALUE,TipoOrden.LABORATORIO,"Texto",List.of(detail)))).getError().status());
                for(String quantity:List.of("0","1.5")) {
                    var invalid=new OrdenRequest.Detalle("Inválido",new BigDecimal(quantity),null);
                    assertEquals(400,assertThrows(ApplicationException.class,()->orders.create(new OrdenRequest(otherCita,TipoOrden.LABORATORIO,"Texto",List.of(detail,invalid)))).getError().status());
                }
                assertEquals(1,orders.list("",otherCita,null,1,20).total());
                assertEquals(409,assertThrows(ApplicationException.class,()->slots.save(otherCita,new CitaRequest(patients.get(1),medico,when.plusHours(1).toString(),"Prueba"))).getError().status());
                assertEquals(EstadoOrden.COMPLETADA,orders.state(order.orden().idOrden(),new EstadoOrdenRequest(EstadoOrden.COMPLETADA)).orden().estado());
                assertEquals(409,assertThrows(ApplicationException.class,()->orders.state(order.orden().idOrden(),new EstadoOrdenRequest(EstadoOrden.PENDIENTE))).getError().status());
                var canceled=orders.create(new OrdenRequest(cita,TipoOrden.REMISION,"Prueba",List.of(detail)));
                assertEquals(EstadoOrden.CANCELADA,orders.state(canceled.orden().idOrden(),new EstadoOrdenRequest(EstadoOrden.CANCELADA)).orden().estado());
                assertEquals(404,assertThrows(ApplicationException.class,()->orders.get(Integer.MAX_VALUE)).getError().status());
                // La restricción de la base protege las órdenes ante un borrado directo de su cita.
                assertThrows(RuntimeException.class,()->new JpaWork(factory).write(manager -> manager.createQuery("delete from Cita c where c.idCita=:id").setParameter("id",otherCita).executeUpdate()));
                assertEquals(2,orders.get(order.orden().idOrden()).detalles().size());
            } finally {
                try(var manager=factory.createEntityManager()) {
                    manager.getTransaction().begin();
                    if(!appointments.isEmpty()) {
                        manager.createQuery("delete from DetalleOrden d where d.orden.cita.idCita in :ids").setParameter("ids",appointments).executeUpdate();
                        manager.createQuery("delete from Orden o where o.cita.idCita in :ids").setParameter("ids",appointments).executeUpdate();
                        manager.createQuery("delete from Tratamiento t where t.diagnostico.cita.idCita in :ids").setParameter("ids",appointments).executeUpdate();
                        manager.createQuery("delete from Diagnostico d where d.cita.idCita in :ids").setParameter("ids",appointments).executeUpdate();
                        manager.createQuery("delete from Cita c where c.idCita in :ids").setParameter("ids",appointments).executeUpdate();
                    }
                    if(!patients.isEmpty()) {
                        manager.createQuery("delete from HistoriaClinica h where h.paciente.idPaciente in :ids").setParameter("ids",patients).executeUpdate();
                        manager.createQuery("delete from Paciente p where p.idPaciente in :ids").setParameter("ids",patients).executeUpdate();
                    }
                    if(doctor!=null) manager.createQuery("delete from Medico m where m.idMedico=:id").setParameter("id",doctor).executeUpdate();
                    if(specialty!=null) manager.createQuery("delete from Especialidad e where e.idEspecialidad=:id").setParameter("id",specialty).executeUpdate();
                    manager.getTransaction().commit();
                }
            }
        }
    }
}
