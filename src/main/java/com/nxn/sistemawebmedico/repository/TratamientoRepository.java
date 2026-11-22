package com.nxn.sistemawebmedico.repository;

import com.nxn.sistemawebmedico.entity.Tratamiento;
import jakarta.persistence.EntityManager;
import java.util.List;

public class TratamientoRepository {
    private String condition(Integer diagnostico) {
        return " where (lower(t.medicamento) like :q escape '!' or lower(t.diagnostico.codigoCie10) like :q escape '!')"
                +(diagnostico==null?"":" and t.diagnostico.idDiagnostico = :diagnostico");
    }
    public List<Tratamiento> list(EntityManager manager,String q,Integer diagnostico,int page,int size) {
        var query=manager.createQuery("select t from Tratamiento t join fetch t.diagnostico d join fetch d.cita c join fetch c.paciente"+condition(diagnostico)+" order by t.idTratamiento desc",Tratamiento.class)
                .setParameter("q",EspecialidadRepository.pattern(q)).setFirstResult((page-1)*size).setMaxResults(size);
        if(diagnostico!=null) query.setParameter("diagnostico",diagnostico);
        return query.getResultList();
    }
    public long count(EntityManager manager,String q,Integer diagnostico) {
        var query=manager.createQuery("select count(t) from Tratamiento t"+condition(diagnostico),Long.class).setParameter("q",EspecialidadRepository.pattern(q));
        if(diagnostico!=null) query.setParameter("diagnostico",diagnostico);
        return query.getSingleResult();
    }
}
