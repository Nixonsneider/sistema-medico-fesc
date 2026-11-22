package com.nxn.sistemawebmedico.repository;

import com.nxn.sistemawebmedico.entity.Diagnostico;
import jakarta.persistence.EntityManager;
import java.util.List;

public class DiagnosticoRepository {
    private String condition(Integer cita) {
        return " where (lower(d.codigoCie10) like :q escape '!' or lower(concat(concat(d.cita.paciente.nombre, ' '), d.cita.paciente.apellido)) like :q escape '!')"
                + (cita==null?"":" and d.cita.idCita = :cita");
    }
    public List<Diagnostico> list(EntityManager manager,String q,Integer cita,int page,int size) {
        var query=manager.createQuery("select d from Diagnostico d join fetch d.cita c join fetch c.paciente"+condition(cita)+" order by d.idDiagnostico desc",Diagnostico.class)
                .setParameter("q",EspecialidadRepository.pattern(q)).setFirstResult((page-1)*size).setMaxResults(size);
        if(cita!=null) query.setParameter("cita",cita);
        return query.getResultList();
    }
    public long count(EntityManager manager,String q,Integer cita) {
        var query=manager.createQuery("select count(d) from Diagnostico d"+condition(cita),Long.class).setParameter("q",EspecialidadRepository.pattern(q));
        if(cita!=null) query.setParameter("cita",cita);
        return query.getSingleResult();
    }
}
