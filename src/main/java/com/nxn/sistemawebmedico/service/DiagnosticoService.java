package com.nxn.sistemawebmedico.service;

import com.nxn.sistemawebmedico.dto.*;
import com.nxn.sistemawebmedico.entity.Cita;
import com.nxn.sistemawebmedico.entity.Diagnostico;
import com.nxn.sistemawebmedico.error.ApplicationException;
import com.nxn.sistemawebmedico.repository.DiagnosticoRepository;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Map;

public class DiagnosticoService {
    private final JpaWork work;
    private final DiagnosticoRepository repository=new DiagnosticoRepository();
    public DiagnosticoService(EntityManagerFactory factory) { work=new JpaWork(factory); }
    public Page<DiagnosticoSummary> list(String query,Integer cita,int page,int size) {
        String q=DataValidation.search(query,page,size);
        if(cita!=null) DataValidation.id(cita);
        return work.read(manager -> new Page<>(repository.list(manager,q,cita,page,size).stream().map(DiagnosticoSummary::from).toList(),repository.count(manager,q,cita),page,size));
    }
    public List<ClinicalOption> options(String query) {
        String q=DataValidation.search(query,1,100);
        return work.read(manager -> repository.list(manager,q,null,1,100).stream().map(d -> new ClinicalOption(d.getIdDiagnostico(),
                "Diagnóstico #"+d.getIdDiagnostico()+" · "+d.getCodigoCie10()+" · "+d.getCita().getPaciente().getNombre()+" "+d.getCita().getPaciente().getApellido()+" · Cita #"+d.getCita().getIdCita())).toList());
    }
    public DiagnosticoResponse get(int id) {
        DataValidation.id(id);
        return work.read(manager -> { var entity=manager.find(Diagnostico.class,id);if(entity==null) throw ApplicationException.notFound();return DiagnosticoResponse.from(entity); });
    }
    public DiagnosticoResponse save(Integer id,DiagnosticoRequest data) {
        if(id!=null) DataValidation.id(id);
        if(data==null) throw ApplicationException.invalidData(Map.of());
        var validation=new DataValidation();
        String code=validation.required("codigoCie10",data.codigoCie10(),20);
        String description=validation.required("descripcion",data.descripcion(),10000);
        if(data.tipo()==null) validation.reject("tipo","Selecciona un tipo de diagnóstico.");
        if(data.idCita()==null || data.idCita()<1) validation.reject("idCita","Selecciona una cita existente.");
        validation.check();
        return work.write(manager -> {
            Diagnostico entity=id==null?new Diagnostico():manager.find(Diagnostico.class,id,LockModeType.PESSIMISTIC_WRITE);
            if(entity==null) throw ApplicationException.notFound();
            if(id!=null && !entity.getCita().getIdCita().equals(data.idCita())) throw ApplicationException.invalidData(Map.of("idCita","No se puede trasladar el diagnóstico a otra cita."));
            Cita cita=manager.find(Cita.class,data.idCita(),LockModeType.PESSIMISTIC_WRITE);
            if(cita==null) throw ApplicationException.invalidData(Map.of("idCita","La cita seleccionada no existe."));
            entity.setCita(cita);entity.setCodigoCie10(code);entity.setDescripcion(description);entity.setTipo(data.tipo());
            if(id==null) manager.persist(entity);
            return DiagnosticoResponse.from(entity);
        });
    }
}
