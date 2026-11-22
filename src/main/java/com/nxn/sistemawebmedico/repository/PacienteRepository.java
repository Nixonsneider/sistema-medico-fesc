package com.nxn.sistemawebmedico.repository;

import com.nxn.sistemawebmedico.entity.Paciente;
import com.nxn.sistemawebmedico.dto.PersonaOption;
import jakarta.persistence.EntityManager;
import java.util.List;

public class PacienteRepository {
    private static final String FILTER = " where lower(concat(concat(p.nombre, ' '), p.apellido)) like :q escape '!' or lower(p.documento) like :q escape '!'";
    public Paciente find(EntityManager manager, int id) { return manager.find(Paciente.class, id); }
    public long count(EntityManager manager, String q) {
        return manager.createQuery("select count(p) from Paciente p" + FILTER, Long.class)
                .setParameter("q", EspecialidadRepository.pattern(q)).getSingleResult();
    }
    public List<Paciente> list(EntityManager manager, String q, int page, int size) {
        return manager.createQuery("select p from Paciente p" + FILTER + " order by lower(p.apellido), lower(p.nombre), p.idPaciente", Paciente.class)
                .setParameter("q", EspecialidadRepository.pattern(q)).setFirstResult((page-1)*size).setMaxResults(size).getResultList();
    }
    public boolean exists(EntityManager manager, String field, String value, int excluded) {
        if (!List.of("documento", "email").contains(field)) throw new IllegalArgumentException();
        return manager.createQuery("select count(p) from Paciente p where p." + field + " = :value and p.idPaciente <> :id", Long.class)
                .setParameter("value", value).setParameter("id", excluded).getSingleResult() > 0;
    }
    public List<PersonaOption> options(EntityManager manager, String q) {
        return manager.createQuery("select new com.nxn.sistemawebmedico.dto.PersonaOption(p.idPaciente, concat(concat(p.nombre, ' '), p.apellido), p.documento) from Paciente p" + FILTER + " order by lower(p.apellido), p.idPaciente", PersonaOption.class)
                .setParameter("q", EspecialidadRepository.pattern(q)).setMaxResults(100).getResultList();
    }
}
