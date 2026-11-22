package com.nxn.sistemawebmedico.service;

import com.nxn.sistemawebmedico.dto.HistoriaRequest;
import com.nxn.sistemawebmedico.dto.HistoriaResponse;
import com.nxn.sistemawebmedico.entity.HistoriaClinica;
import com.nxn.sistemawebmedico.entity.Paciente;
import com.nxn.sistemawebmedico.error.ApplicationException;
import com.nxn.sistemawebmedico.repository.HistoriaRepository;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Map;

public class HistoriaService {
    private final JpaWork work;
    private final HistoriaRepository repository=new HistoriaRepository();
    public HistoriaService(EntityManagerFactory factory) { work=new JpaWork(factory); }
    public HistoriaResponse get(int patient) {
        DataValidation.id(patient);
        return work.read(manager -> {
            if(manager.find(Paciente.class,patient)==null) throw ApplicationException.notFound();
            var history=repository.byPatient(manager,patient);
            return history==null?null:HistoriaResponse.from(history);
        });
    }
    public HistoriaResponse save(int patient, HistoriaRequest data, boolean create) {
        DataValidation.id(patient);
        if(data==null || data.grupoSanguineo()==null || !List.of("A+","A-","B+","B-","AB+","AB-","O+","O-").contains(data.grupoSanguineo())) {
            throw ApplicationException.invalidData(Map.of("grupoSanguineo","Selecciona un grupo sanguíneo válido."));
        }
        return work.write(manager -> {
            Paciente owner=manager.find(Paciente.class,patient,LockModeType.PESSIMISTIC_WRITE);
            if(owner==null) throw ApplicationException.notFound();
            HistoriaClinica history=repository.byPatient(manager,patient);
            if(create && history!=null) throw ApplicationException.duplicate(Map.of("idPaciente","El paciente ya tiene una historia clínica."));
            if(!create && history==null) throw ApplicationException.notFound();
            if(create) { history=new HistoriaClinica(); history.setPaciente(owner); }
            history.setGrupoSanguineo(data.grupoSanguineo()); history.setAntecedentes(optional(data.antecedentes())); history.setAlergias(optional(data.alergias()));
            if(create) manager.persist(history);
            return HistoriaResponse.from(history);
        });
    }
    private String optional(String text) { return text==null || text.isBlank()?null:text.strip(); }
}
