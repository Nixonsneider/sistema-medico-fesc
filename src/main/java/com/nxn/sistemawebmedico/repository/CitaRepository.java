package com.nxn.sistemawebmedico.repository;

import com.nxn.sistemawebmedico.entity.Cita;
import com.nxn.sistemawebmedico.enums.EstadoCita;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.OffsetDateTime;
import java.util.List;

public class CitaRepository {
    public boolean hasDiagnosticos(EntityManager manager,int id) {
        return manager.createQuery("select count(d) from Diagnostico d where d.cita.idCita = :id",Long.class).setParameter("id",id).getSingleResult()>0;
    }
    public boolean hasOrdenes(EntityManager manager,int id) {
        return manager.createQuery("select count(o) from Orden o where o.cita.idCita = :id",Long.class).setParameter("id",id).getSingleResult()>0;
    }
    private static final String SEARCH = " where (lower(concat(concat(c.paciente.nombre, ' '), c.paciente.apellido)) like :q escape '!'"
            + " or lower(concat(concat(c.medico.nombre, ' '), c.medico.apellido)) like :q escape '!'"
            + " or lower(c.paciente.documento) like :q escape '!')";
    private <T> TypedQuery<T> filter(TypedQuery<T> query, String q, Integer patient, Integer doctor, EstadoCita state, OffsetDateTime from, OffsetDateTime to) {
        query.setParameter("q", EspecialidadRepository.pattern(q));
        if(patient!=null) query.setParameter("paciente",patient);
        if(doctor!=null) query.setParameter("medico",doctor);
        if(state!=null) query.setParameter("estado",state);
        if(from!=null) query.setParameter("desde",from);
        if(to!=null) query.setParameter("hasta",to);
        return query;
    }
    private String conditions(Integer patient,Integer doctor,EstadoCita state,OffsetDateTime from,OffsetDateTime to) {
        return SEARCH + (patient==null?"":" and c.paciente.idPaciente = :paciente")
                + (doctor==null?"":" and c.medico.idMedico = :medico")
                + (state==null?"":" and c.estadoCita = :estado")
                + (from==null?"":" and c.fechaHora >= :desde") + (to==null?"":" and c.fechaHora < :hasta");
    }
    public List<Cita> list(EntityManager manager, String q, Integer patient, Integer doctor, EstadoCita state, OffsetDateTime from, OffsetDateTime to, int page, int size) {
        return filter(manager.createQuery("select c from Cita c join fetch c.paciente join fetch c.medico" + conditions(patient,doctor,state,from,to) + " order by c.fechaHora desc, c.idCita desc", Cita.class), q, patient, doctor, state, from, to)
                .setFirstResult((page-1)*size).setMaxResults(size).getResultList();
    }
    public long count(EntityManager manager, String q, Integer patient, Integer doctor, EstadoCita state, OffsetDateTime from, OffsetDateTime to) {
        return filter(manager.createQuery("select count(c) from Cita c" + conditions(patient,doctor,state,from,to), Long.class), q, patient, doctor, state, from, to).getSingleResult();
    }
    public boolean busy(EntityManager manager, int patient, int doctor, OffsetDateTime when, int excluded) {
        return manager.createQuery("select count(c) from Cita c where c.idCita <> :id and c.fechaHora = :fecha"
                + " and c.estadoCita in :estados and (c.paciente.idPaciente = :paciente or c.medico.idMedico = :medico)", Long.class)
                .setParameter("id", excluded).setParameter("fecha", when)
                .setParameter("estados", List.of(EstadoCita.PROGRAMADA, EstadoCita.CONFIRMADA))
                .setParameter("paciente", patient).setParameter("medico", doctor).getSingleResult() > 0;
    }
}
