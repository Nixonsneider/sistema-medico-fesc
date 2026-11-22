package com.nxn.sistemawebmedico.service;

import com.nxn.sistemawebmedico.dto.*;
import com.nxn.sistemawebmedico.entity.Diagnostico;
import com.nxn.sistemawebmedico.entity.Tratamiento;
import com.nxn.sistemawebmedico.error.ApplicationException;
import com.nxn.sistemawebmedico.repository.TratamientoRepository;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import java.util.Map;

public class TratamientoService {
    private final JpaWork work;
    private final TratamientoRepository repository=new TratamientoRepository();
    public TratamientoService(EntityManagerFactory factory) { work=new JpaWork(factory); }
    public Page<TratamientoResponse> list(String query,Integer diagnostic,int page,int size) {
        String q=DataValidation.search(query,page,size);
        if(diagnostic!=null) DataValidation.id(diagnostic);
        return work.read(manager -> new Page<>(repository.list(manager,q,diagnostic,page,size).stream().map(TratamientoResponse::from).toList(),repository.count(manager,q,diagnostic),page,size));
    }
    public TratamientoResponse get(int id) {
        DataValidation.id(id);
        return work.read(manager -> { var entity=manager.find(Tratamiento.class,id);if(entity==null) throw ApplicationException.notFound();return TratamientoResponse.from(entity); });
    }
    public TratamientoResponse save(Integer id,TratamientoRequest data) {
        if(id!=null) DataValidation.id(id);
        if(data==null) throw ApplicationException.invalidData(Map.of());
        var validation=new DataValidation();
        String drug=validation.required("medicamento",data.medicamento(),150);
        String dose=validation.required("dosis",data.dosis(),100);
        String frequency=validation.required("frecuencia",data.frecuencia(),100);
        Integer days=null;
        try { if(data.duracionDias()!=null) days=data.duracionDias().intValueExact(); }
        catch(ArithmeticException exception) { /* La duración no puede redondearse ni exceder un entero. */ }
        if(days==null || days<1) validation.reject("duracionDias","La duración debe ser un número entero mayor que cero.");
        if(data.idDiagnostico()==null || data.idDiagnostico()<1) validation.reject("idDiagnostico","Selecciona un diagnóstico existente.");
        validation.check();
        int duration=days;
        return work.write(manager -> {
            Tratamiento entity=id==null?new Tratamiento():manager.find(Tratamiento.class,id,LockModeType.PESSIMISTIC_WRITE);
            if(entity==null) throw ApplicationException.notFound();
            if(id!=null && !entity.getDiagnostico().getIdDiagnostico().equals(data.idDiagnostico())) throw ApplicationException.invalidData(Map.of("idDiagnostico","No se puede trasladar el tratamiento a otro diagnóstico."));
            Diagnostico diagnostic=manager.find(Diagnostico.class,data.idDiagnostico(),LockModeType.PESSIMISTIC_READ);
            if(diagnostic==null) throw ApplicationException.invalidData(Map.of("idDiagnostico","El diagnóstico seleccionado no existe."));
            entity.setDiagnostico(diagnostic);entity.setMedicamento(drug);entity.setDosis(dose);entity.setFrecuencia(frequency);entity.setDuracionDias(duration);
            entity.setIndicaciones(data.indicaciones()==null || data.indicaciones().isBlank()?null:data.indicaciones().strip());
            if(id==null) manager.persist(entity);
            return TratamientoResponse.from(entity);
        });
    }
}
