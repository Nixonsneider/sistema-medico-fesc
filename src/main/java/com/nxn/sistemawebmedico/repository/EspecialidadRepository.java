package com.nxn.sistemawebmedico.repository;

import com.nxn.sistemawebmedico.entity.Especialidad;
import jakarta.persistence.EntityManager;
import java.util.List;

public class EspecialidadRepository {
    private static final String FILTER = " where (:activo is null or e.activo = :activo) and lower(e.nombre) like :q escape '!'";

    public Especialidad find(EntityManager manager, int id) { return manager.find(Especialidad.class, id); }

    public boolean nameExists(EntityManager manager, String nombre, int excludedId) {
        return manager.createQuery("select count(e) from Especialidad e where e.nombre = :nombre and e.idEspecialidad <> :id", Long.class)
                .setParameter("nombre", nombre).setParameter("id", excludedId).getSingleResult() > 0;
    }

    public long count(EntityManager manager, String query, Boolean activo) {
        return manager.createQuery("select count(e) from Especialidad e" + FILTER, Long.class)
                .setParameter("activo", activo).setParameter("q", pattern(query)).getSingleResult();
    }

    public List<Especialidad> list(EntityManager manager, String query, Boolean activo, int page, int size) {
        return manager.createQuery("select e from Especialidad e" + FILTER + " order by lower(e.nombre), e.idEspecialidad", Especialidad.class)
                .setParameter("activo", activo).setParameter("q", pattern(query))
                .setFirstResult((page - 1) * size).setMaxResults(size).getResultList();
    }

    static String pattern(String query) {
        return "%" + query.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
