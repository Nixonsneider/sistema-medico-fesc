package com.nxn.sistemawebmedico.repository;

import com.nxn.sistemawebmedico.entity.DetalleOrden;
import com.nxn.sistemawebmedico.entity.Orden;
import com.nxn.sistemawebmedico.enums.EstadoOrden;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;

public class OrdenRepository {
    private String condition(Integer cita,EstadoOrden state) {
        return " where lower(concat(concat(o.cita.paciente.nombre, ' '),o.cita.paciente.apellido)) like :q escape '!'"
                +(cita==null?"":" and o.cita.idCita = :cita")+(state==null?"":" and o.estado = :estado");
    }
    private <T> TypedQuery<T> filter(TypedQuery<T> query,String q,Integer cita,EstadoOrden state) {
        query.setParameter("q",EspecialidadRepository.pattern(q));
        if(cita!=null) query.setParameter("cita",cita);
        if(state!=null) query.setParameter("estado",state);
        return query;
    }
    public List<Orden> list(EntityManager manager,String q,Integer cita,EstadoOrden state,int page,int size) {
        return filter(manager.createQuery("select o from Orden o join fetch o.cita c join fetch c.paciente"+condition(cita,state)+" order by o.idOrden desc",Orden.class),q,cita,state)
                .setFirstResult((page-1)*size).setMaxResults(size).getResultList();
    }
    public long count(EntityManager manager,String q,Integer cita,EstadoOrden state) {
        return filter(manager.createQuery("select count(o) from Orden o"+condition(cita,state),Long.class),q,cita,state).getSingleResult();
    }
    public List<DetalleOrden> details(EntityManager manager,int id) {
        return manager.createQuery("select d from DetalleOrden d where d.orden.idOrden = :id order by d.idDetalle",DetalleOrden.class).setParameter("id",id).getResultList();
    }
}
