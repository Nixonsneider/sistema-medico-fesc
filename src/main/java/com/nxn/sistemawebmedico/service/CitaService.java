package com.nxn.sistemawebmedico.service;

import com.nxn.sistemawebmedico.dto.*;
import com.nxn.sistemawebmedico.entity.*;
import com.nxn.sistemawebmedico.enums.EstadoCita;
import com.nxn.sistemawebmedico.error.ApplicationException;
import com.nxn.sistemawebmedico.repository.CitaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import java.time.*;
import java.util.List;
import java.util.Map;

public class CitaService {
    private static final ZoneId ZONE = ZoneId.of("America/Bogota");
    private final JpaWork work;
    private final Clock clock;
    private final CitaRepository repository = new CitaRepository();

    public CitaService(EntityManagerFactory factory) { this(factory,Clock.system(ZONE)); }
    CitaService(EntityManagerFactory factory, Clock clock) { work=new JpaWork(factory); this.clock=clock; }

    public java.util.List<ClinicalOption> options(String query) {
        String q=DataValidation.search(query,1,100);
        var format=java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZONE);
        return work.read(manager -> repository.list(manager,q,null,null,null,null,null,1,100).stream().map(c -> new ClinicalOption(c.getIdCita(),
                "Cita #"+c.getIdCita()+" · "+c.getPaciente().getNombre()+" "+c.getPaciente().getApellido()+" · "+format.format(c.getFechaHora()))).toList());
    }

    public Page<CitaSummary> list(String query,Integer patient,Integer doctor,EstadoCita state,String from,String to,int page,int size) {
        String q=DataValidation.search(query,page,size);
        if(patient!=null) DataValidation.id(patient);
        if(doctor!=null) DataValidation.id(doctor);
        LocalDate first=date(from,"desde"),last=date(to,"hasta");
        if(first!=null && last!=null && first.isAfter(last)) throw ApplicationException.invalidData(Map.of("hasta","La fecha final debe ser igual o posterior a la inicial."));
        OffsetDateTime start=first==null?null:first.atStartOfDay(ZONE).toOffsetDateTime();
        OffsetDateTime end;
        try { end=last==null?null:last.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime(); }
        catch(DateTimeException exception) { throw ApplicationException.invalidData(Map.of("hasta","Ingresa una fecha válida.")); }
        return work.read(manager -> new Page<>(repository.list(manager,q,patient,doctor,state,start,end,page,size).stream().map(CitaSummary::from).toList(),repository.count(manager,q,patient,doctor,state,start,end),page,size));
    }

    private LocalDate date(String value,String field) {
        if(value==null || value.isBlank()) return null;
        try { return LocalDate.parse(value); }
        catch(DateTimeException exception) { throw ApplicationException.invalidData(Map.of(field,"Usa una fecha válida con formato AAAA-MM-DD.")); }
    }

    public CitaResponse get(int id) {
        DataValidation.id(id);
        return work.read(manager -> CitaResponse.from(require(manager,id,false)));
    }

    public CitaResponse save(Integer id,CitaRequest data) {
        if(id!=null) DataValidation.id(id);
        if(data==null) throw ApplicationException.invalidData(Map.of());
        var validation=new DataValidation();
        String reason=validation.required("motivo",data.motivo(),10000);
        if(data.idPaciente()==null || data.idPaciente()<1) validation.reject("idPaciente","Selecciona un paciente.");
        if(data.idMedico()==null || data.idMedico()<1) validation.reject("idMedico","Selecciona un médico.");
        OffsetDateTime when=null;
        try { when=OffsetDateTime.parse(data.fechaHora()==null?"":data.fechaHora()); }
        catch(DateTimeException exception) { validation.reject("fechaHora","Ingresa fecha y hora con zona o desplazamiento."); }
        if(when!=null) {
            if(!when.toInstant().isAfter(clock.instant())) validation.reject("fechaHora","La cita debe programarse en una fecha y hora futura.");
            if(when.getSecond()!=0 || when.getNano()!=0) validation.reject("fechaHora","Selecciona la hora con precisión de minutos, sin segundos.");
        }
        validation.check();
        OffsetDateTime appointment=when;
        return work.write(manager -> {
            Cita entity=id==null?new Cita():require(manager,id,true);
            if(!active(entity.getEstadoCita())) throw ApplicationException.conflict("Una cita finalizada no puede reprogramarse.",Map.of());
            if(id!=null && (!entity.getPaciente().getIdPaciente().equals(data.idPaciente()) || !entity.getMedico().getIdMedico().equals(data.idMedico())) && (repository.hasDiagnosticos(manager,id) || repository.hasOrdenes(manager,id))) {
                throw ApplicationException.conflict("No se pueden cambiar los participantes de una cita con diagnósticos u órdenes registrados.",Map.of());
            }
            // Orden fijo: médico y luego paciente. Serializa reservas del mismo recurso.
            Medico doctor=manager.find(Medico.class,data.idMedico(),LockModeType.PESSIMISTIC_WRITE);
            Paciente patient=manager.find(Paciente.class,data.idPaciente(),LockModeType.PESSIMISTIC_WRITE);
            if(doctor==null) throw ApplicationException.invalidData(Map.of("idMedico","El médico seleccionado no existe."));
            if(patient==null) throw ApplicationException.invalidData(Map.of("idPaciente","El paciente seleccionado no existe."));
            if(repository.busy(manager,data.idPaciente(),data.idMedico(),appointment,id==null?0:id)) {
                throw ApplicationException.conflict("El médico o paciente ya tiene una cita activa a esa hora.",Map.of("fechaHora","Selecciona otra hora, médico o paciente."));
            }
            boolean changed=id!=null && (!entity.getFechaHora().toInstant().equals(appointment.toInstant())
                    || !entity.getMedico().getIdMedico().equals(data.idMedico()) || !entity.getPaciente().getIdPaciente().equals(data.idPaciente()));
            entity.setMedico(doctor);entity.setPaciente(patient);entity.setFechaHora(appointment);entity.setMotivo(reason);
            if(changed) entity.setEstadoCita(EstadoCita.PROGRAMADA);
            if(id==null) manager.persist(entity);
            return CitaResponse.from(entity);
        });
    }

    public CitaResponse state(int id,EstadoCitaRequest data) {
        DataValidation.id(id);
        if(data==null || data.estado()==null) throw ApplicationException.invalidData(Map.of("estado","Selecciona un estado válido."));
        return work.write(manager -> {
            Cita entity=require(manager,id,true);
            if(entity.getEstadoCita()==data.estado()) return CitaResponse.from(entity);
            if(!active(entity.getEstadoCita()) || data.estado()==EstadoCita.PROGRAMADA) {
                throw ApplicationException.conflict("El cambio de estado no está permitido.",Map.of("estado","Las citas finalizadas no pueden reabrirse."));
            }
            if(List.of(EstadoCita.ATENDIDA,EstadoCita.NO_ASISTIO).contains(data.estado()) && clock.instant().isBefore(entity.getFechaHora().toInstant())) {
                throw ApplicationException.invalidData(Map.of("estado","Espera hasta la hora de la cita para registrar atención o inasistencia."));
            }
            manager.lock(entity.getMedico(),LockModeType.PESSIMISTIC_WRITE);
            manager.lock(entity.getPaciente(),LockModeType.PESSIMISTIC_WRITE);
            entity.setEstadoCita(data.estado());
            return CitaResponse.from(entity);
        });
    }

    private Cita require(EntityManager manager,int id,boolean lock) {
        Cita entity=lock?manager.find(Cita.class,id,LockModeType.PESSIMISTIC_WRITE):manager.find(Cita.class,id);
        if(entity==null) throw ApplicationException.notFound();
        return entity;
    }
    private boolean active(EstadoCita state) { return state==EstadoCita.PROGRAMADA || state==EstadoCita.CONFIRMADA; }
}
