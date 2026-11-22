package com.nxn.sistemawebmedico.repository;

import com.nxn.sistemawebmedico.entity.HistoriaClinica;
import jakarta.persistence.EntityManager;

public class HistoriaRepository {
    public HistoriaClinica byPatient(EntityManager manager, int id) {
        return manager.createQuery("select h from HistoriaClinica h where h.paciente.idPaciente = :id", HistoriaClinica.class)
                .setParameter("id", id).getResultStream().findFirst().orElse(null);
    }
}
