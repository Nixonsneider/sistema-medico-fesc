package com.nxn.sistemawebmedico.service;

import com.nxn.sistemawebmedico.dto.EspecialidadRequest;
import com.nxn.sistemawebmedico.dto.EspecialidadResponse;
import com.nxn.sistemawebmedico.dto.Page;
import com.nxn.sistemawebmedico.entity.Especialidad;
import com.nxn.sistemawebmedico.error.ApplicationException;
import com.nxn.sistemawebmedico.repository.EspecialidadRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.Map;

public class EspecialidadService {
    private final JpaWork work;
    private final EspecialidadRepository repository = new EspecialidadRepository();

    public EspecialidadService(EntityManagerFactory factory) { work = new JpaWork(factory); }

    public Page<EspecialidadResponse> list(String query, Boolean active, int page, int size) {
        String cleaned = DataValidation.search(query, page, size);
        return work.read(manager -> new Page<>(repository.list(manager, cleaned, active, page, size).stream()
                .map(EspecialidadResponse::from).toList(), repository.count(manager, cleaned, active), page, size));
    }

    public EspecialidadResponse get(int id) {
        DataValidation.id(id);
        return work.read(manager -> {
            Especialidad entity = repository.find(manager, id);
            if (entity == null) throw ApplicationException.notFound();
            return EspecialidadResponse.from(entity);
        });
    }

    public EspecialidadResponse save(Integer id, EspecialidadRequest data) {
        if (id != null) DataValidation.id(id);
        if (data == null) throw ApplicationException.invalidData(Map.of());
        DataValidation validation = new DataValidation();
        String name = validation.required("nombre", data.nombre(), 100);
        validation.check();
        return work.write(manager -> {
            Especialidad entity = id == null ? new Especialidad() : repository.find(manager, id);
            if (entity == null) throw ApplicationException.notFound();
            if (repository.nameExists(manager, name, id == null ? 0 : id)) {
                throw ApplicationException.duplicate(Map.of("nombre", "Ya existe una especialidad con este nombre."));
            }
            entity.setNombre(name);
            entity.setDescripcion(data.descripcion() == null || data.descripcion().isBlank() ? null : data.descripcion().strip());
            if (data.activo() != null) entity.setActivo(data.activo());
            if (id == null) manager.persist(entity);
            return EspecialidadResponse.from(entity);
        });
    }
}
