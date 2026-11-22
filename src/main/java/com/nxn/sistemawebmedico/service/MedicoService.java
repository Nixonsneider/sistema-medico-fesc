package com.nxn.sistemawebmedico.service;

import com.nxn.sistemawebmedico.dto.MedicoRequest;
import com.nxn.sistemawebmedico.dto.MedicoResponse;
import com.nxn.sistemawebmedico.dto.Page;
import com.nxn.sistemawebmedico.entity.Especialidad;
import com.nxn.sistemawebmedico.entity.Medico;
import com.nxn.sistemawebmedico.error.ApplicationException;
import com.nxn.sistemawebmedico.repository.MedicoRepository;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import java.util.LinkedHashMap;
import java.util.Map;

public class MedicoService {
    private final JpaWork work;
    private final MedicoRepository repository = new MedicoRepository();

    public MedicoService(EntityManagerFactory factory) { work = new JpaWork(factory); }

    public java.util.List<com.nxn.sistemawebmedico.dto.PersonaOption> options(String query) {
        String cleaned = DataValidation.search(query, 1, 100);
        return work.read(manager -> repository.options(manager, cleaned));
    }

    public Page<MedicoResponse> list(String query, Integer specialty, int page, int size) {
        String cleaned = DataValidation.search(query, page, size);
        if (specialty != null) DataValidation.id(specialty);
        return work.read(manager -> new Page<>(repository.list(manager, cleaned, specialty, page, size).stream()
                .map(MedicoResponse::from).toList(), repository.count(manager, cleaned, specialty), page, size));
    }

    public MedicoResponse get(int id) {
        DataValidation.id(id);
        return work.read(manager -> {
            Medico entity = repository.find(manager, id);
            if (entity == null) throw ApplicationException.notFound();
            return MedicoResponse.from(entity);
        });
    }

    public MedicoResponse save(Integer id, MedicoRequest data) {
        if (id != null) DataValidation.id(id);
        if (data == null) throw ApplicationException.invalidData(Map.of());
        DataValidation validation = new DataValidation();
        String document = validation.required("documento", data.documento(), 20);
        String name = validation.required("nombre", data.nombre(), 100);
        String surname = validation.required("apellido", data.apellido(), 100);
        String registration = validation.required("registroMedico", data.registroMedico(), 50);
        String phone = validation.required("telefono", data.telefono(), 20);
        String email = validation.required("email", data.email(), 100);
        validation.email(email);
        validation.reference(data.idEspecialidad());
        validation.check();
        return work.write(manager -> {
            Medico entity = id == null ? new Medico() : repository.find(manager, id);
            if (entity == null) throw ApplicationException.notFound();
            Especialidad specialty = manager.find(Especialidad.class, data.idEspecialidad(), LockModeType.PESSIMISTIC_READ);
            if (specialty == null) throw ApplicationException.invalidData(Map.of("idEspecialidad", "La especialidad seleccionada ya no existe."));
            boolean sameSpecialty = id != null && entity.getEspecialidad().getIdEspecialidad().equals(data.idEspecialidad());
            if (!specialty.getActivo() && !sameSpecialty) {
                throw ApplicationException.invalidData(Map.of("idEspecialidad", "Selecciona una especialidad activa."));
            }
            Map<String, String> duplicates = new LinkedHashMap<>();
            int excludedId = id == null ? 0 : id;
            if (repository.exists(manager, "documento", document, excludedId)) duplicates.put("documento", "Ya existe un médico con este documento.");
            if (repository.exists(manager, "registroMedico", registration, excludedId)) duplicates.put("registroMedico", "Este registro médico ya está en uso.");
            if (repository.exists(manager, "email", email, excludedId)) duplicates.put("email", "Este correo ya está en uso.");
            if (!duplicates.isEmpty()) throw ApplicationException.duplicate(duplicates);
            entity.setDocumento(document);
            entity.setNombre(name);
            entity.setApellido(surname);
            entity.setRegistroMedico(registration);
            entity.setTelefono(phone);
            entity.setEmail(email);
            entity.setEspecialidad(specialty);
            if (id == null) manager.persist(entity);
            return MedicoResponse.from(entity);
        });
    }
}
