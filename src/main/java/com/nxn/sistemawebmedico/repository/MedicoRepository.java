package com.nxn.sistemawebmedico.repository;

import com.nxn.sistemawebmedico.entity.Medico;
import jakarta.persistence.EntityManager;
import java.util.List;

public class MedicoRepository {
    public java.util.List<com.nxn.sistemawebmedico.dto.PersonaOption> options(EntityManager manager, String query) {
        return manager.createQuery("select new com.nxn.sistemawebmedico.dto.PersonaOption(m.idMedico, concat(concat(m.nombre, ' '), m.apellido), m.documento) from Medico m"
                + " where lower(concat(concat(m.nombre, ' '), m.apellido)) like :q escape '!' or lower(m.documento) like :q escape '!' order by lower(m.apellido), m.idMedico", com.nxn.sistemawebmedico.dto.PersonaOption.class)
                .setParameter("q", EspecialidadRepository.pattern(query)).setMaxResults(100).getResultList();
    }
    private static final String FILTER = " where (:especialidad is null or m.especialidad.idEspecialidad = :especialidad)"
            + " and (lower(concat(concat(m.nombre, ' '), m.apellido)) like :q escape '!'"
            + " or lower(m.documento) like :q escape '!' or lower(m.registroMedico) like :q escape '!')";

    public Medico find(EntityManager manager, int id) { return manager.find(Medico.class, id); }

    public boolean exists(EntityManager manager, String field, String value, int excludedId) {
        if (!List.of("documento", "registroMedico", "email").contains(field)) throw new IllegalArgumentException();
        return manager.createQuery("select count(m) from Medico m where m." + field + " = :value and m.idMedico <> :id", Long.class)
                .setParameter("value", value).setParameter("id", excludedId).getSingleResult() > 0;
    }

    public long count(EntityManager manager, String query, Integer specialty) {
        return manager.createQuery("select count(m) from Medico m" + FILTER, Long.class)
                .setParameter("especialidad", specialty).setParameter("q", EspecialidadRepository.pattern(query)).getSingleResult();
    }

    public List<Medico> list(EntityManager manager, String query, Integer specialty, int page, int size) {
        return manager.createQuery("select m from Medico m join fetch m.especialidad" + FILTER + " order by lower(m.apellido), lower(m.nombre), m.idMedico", Medico.class)
                .setParameter("especialidad", specialty).setParameter("q", EspecialidadRepository.pattern(query))
                .setFirstResult((page - 1) * size).setMaxResults(size).getResultList();
    }
}
