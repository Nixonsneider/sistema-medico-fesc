package com.nxn.sistemawebmedico.service;

import com.nxn.sistemawebmedico.dto.*;
import com.nxn.sistemawebmedico.entity.*;
import com.nxn.sistemawebmedico.enums.EstadoOrden;
import com.nxn.sistemawebmedico.error.ApplicationException;
import com.nxn.sistemawebmedico.repository.OrdenRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import java.util.ArrayList;
import java.util.Map;

public class OrdenService {
    private final JpaWork work;
    private final OrdenRepository repository=new OrdenRepository();
    public OrdenService(EntityManagerFactory factory) { work=new JpaWork(factory); }

    public Page<OrdenSummary> list(String query,Integer cita,EstadoOrden state,int page,int size) {
        String q=DataValidation.search(query,page,size);
        if(cita!=null) DataValidation.id(cita);
        return work.read(manager -> new Page<>(repository.list(manager,q,cita,state,page,size).stream().map(OrdenSummary::from).toList(),repository.count(manager,q,cita,state),page,size));
    }
    public OrdenResponse get(int id) {
        DataValidation.id(id);
        return work.read(manager -> response(manager,require(manager,id,false)));
    }
    public OrdenResponse create(OrdenRequest data) {
        if(data==null) throw ApplicationException.invalidData(Map.of());
        var validation=new DataValidation();
        if(data.idCita()==null || data.idCita()<1) validation.reject("idCita","Selecciona una cita existente.");
        if(data.tipoOrden()==null) validation.reject("tipoOrden","Selecciona un tipo de orden.");
        String notes=validation.required("observaciones",data.observaciones(),10000);
        if(data.detalles()==null || data.detalles().isEmpty()) validation.reject("detalles","Añade al menos un detalle.");
        var details=new ArrayList<DetalleOrden>();
        if(data.detalles()!=null) for(int index=0;index<data.detalles().size();index++) {
            var input=data.detalles().get(index);
            String key="detalles["+index+"]";
            if(input==null) { validation.reject(key+".descripcion","Completa el detalle."); continue; }
            var detail=new DetalleOrden();
            detail.setDescripcion(validation.required(key+".descripcion",input.descripcion(),10000));
            Integer quantity=null;
            try { if(input.cantidad()!=null) quantity=input.cantidad().intValueExact(); }
            catch(ArithmeticException exception) { /* No truncar cantidades fraccionarias. */ }
            if(quantity==null || quantity<1) validation.reject(key+".cantidad","La cantidad debe ser un entero mayor que cero.");
            detail.setCantidad(quantity);
            String instructions=input.indicaciones()==null?null:input.indicaciones().strip();
            if(instructions!=null && instructions.length()>10000) validation.reject(key+".indicaciones","Usa como máximo 10000 caracteres.");
            detail.setIndicaciones(instructions==null || instructions.isBlank()?null:instructions);
            details.add(detail);
        }
        validation.check();
        return work.write(manager -> {
            var cita=manager.find(Cita.class,data.idCita(),LockModeType.PESSIMISTIC_WRITE);
            if(cita==null) throw ApplicationException.invalidData(Map.of("idCita","La cita seleccionada no existe."));
            var order=new Orden(); order.setCita(cita); order.setTipoOrden(data.tipoOrden()); order.setObservaciones(notes);
            manager.persist(order);
            for(var detail:details) { detail.setOrden(order); manager.persist(detail); }
            manager.flush();
            return response(manager,order);
        });
    }
    public OrdenResponse state(int id,EstadoOrdenRequest data) {
        DataValidation.id(id);
        if(data==null || data.estado()==null) throw ApplicationException.invalidData(Map.of("estado","Selecciona un estado válido."));
        return work.write(manager -> {
            var order=require(manager,id,true);
            if(order.getEstado()!=data.estado()) {
                if(order.getEstado()!=EstadoOrden.PENDIENTE || data.estado()==EstadoOrden.PENDIENTE)
                    throw ApplicationException.conflict("La orden finalizada no puede cambiar de estado.",Map.of("estado","Crea una nueva orden si es necesario."));
                order.setEstado(data.estado());
            }
            return response(manager,order);
        });
    }
    private Orden require(EntityManager manager,int id,boolean lock) {
        var order=lock?manager.find(Orden.class,id,LockModeType.PESSIMISTIC_WRITE):manager.find(Orden.class,id);
        if(order==null) throw ApplicationException.notFound();
        return order;
    }
    private OrdenResponse response(EntityManager manager,Orden order) {
        return new OrdenResponse(OrdenSummary.from(order),order.getObservaciones(),repository.details(manager,order.getIdOrden()).stream().map(OrdenResponse.Detalle::from).toList());
    }
}
