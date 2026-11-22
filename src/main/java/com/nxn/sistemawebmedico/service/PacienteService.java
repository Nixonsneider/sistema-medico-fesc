package com.nxn.sistemawebmedico.service;

import com.nxn.sistemawebmedico.dto.*;
import com.nxn.sistemawebmedico.entity.Paciente;
import com.nxn.sistemawebmedico.error.ApplicationException;
import com.nxn.sistemawebmedico.repository.PacienteRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PacienteService {
    private final JpaWork work;
    private final PacienteRepository repository = new PacienteRepository();
    public PacienteService(EntityManagerFactory factory) { work = new JpaWork(factory); }
    public Page<PacienteSummary> list(String query, int page, int size) {
        String q = DataValidation.search(query, page, size);
        return work.read(manager -> new Page<>(repository.list(manager,q,page,size).stream().map(PacienteSummary::from).toList(), repository.count(manager,q),page,size));
    }
    public List<PersonaOption> options(String query) {
        String q = DataValidation.search(query,1,100);
        return work.read(manager -> repository.options(manager,q));
    }
    public PacienteResponse get(int id) {
        DataValidation.id(id);
        return work.read(manager -> { var p=repository.find(manager,id); if(p==null) throw ApplicationException.notFound(); return PacienteResponse.from(p); });
    }
    public PacienteResponse save(Integer id, PacienteRequest data) {
        if(id!=null) DataValidation.id(id);
        if(data==null) throw ApplicationException.invalidData(Map.of());
        var validation=new DataValidation();
        String type=validation.required("tipoDocumento",data.tipoDocumento(),20);
        String document=validation.required("documento",data.documento(),20);
        String name=validation.required("nombre",data.nombre(),100);
        String surname=validation.required("apellido",data.apellido(),100);
        String address=validation.required("direccion",data.direccion(),150);
        String phone=validation.required("telefono",data.telefono(),20);
        String email=validation.required("email",data.email(),100); validation.email(email);
        if(data.sexo()==null || !List.of("Masculino","Femenino","Otro").contains(data.sexo())) validation.reject("sexo","Selecciona un sexo válido.");
        LocalDate birth=null;
        try { birth=LocalDate.parse(data.fechaNacimiento()==null?"":data.fechaNacimiento()); }
        catch(DateTimeParseException e) { validation.reject("fechaNacimiento","Ingresa una fecha de nacimiento válida."); }
        if(birth!=null && birth.isAfter(LocalDate.now(ZoneId.of("America/Bogota")))) validation.reject("fechaNacimiento","La fecha de nacimiento no puede estar en el futuro.");
        validation.check();
        LocalDate date=birth;
        return work.write(manager -> {
            Paciente entity=id==null?new Paciente():repository.find(manager,id);
            if(entity==null) throw ApplicationException.notFound();
            Map<String,String> duplicates=new LinkedHashMap<>();
            if(repository.exists(manager,"documento",document,id==null?0:id)) duplicates.put("documento","Ya existe un paciente con este documento.");
            if(repository.exists(manager,"email",email,id==null?0:id)) duplicates.put("email","Este correo ya está en uso por otro paciente.");
            if(!duplicates.isEmpty()) throw ApplicationException.duplicate(duplicates);
            entity.setTipoDocumento(type); entity.setDocumento(document); entity.setNombre(name); entity.setApellido(surname);
            entity.setFechaNacimiento(date); entity.setSexo(data.sexo()); entity.setDireccion(address); entity.setTelefono(phone); entity.setEmail(email);
            if(id==null) manager.persist(entity);
            return PacienteResponse.from(entity);
        });
    }
}
