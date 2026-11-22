package com.nxn.sistemawebmedico.service;

import com.nxn.sistemawebmedico.dto.*;
import com.nxn.sistemawebmedico.enums.*;
import com.nxn.sistemawebmedico.error.ApplicationException;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Consultas de lectura parametrizadas; nunca se interpola entrada del usuario en SQL. */
public class ReportService {
    private final JpaWork work;
    private static final ZoneId ZONE=ZoneId.of("America/Bogota");
    public ReportService(EntityManagerFactory factory) { work=new JpaWork(factory); }
    private record Definition(String title,List<String> columns,String sql) {}
    private static final String JOIN=" from cita c join paciente p on p.id_paciente=c.id_paciente join medico m on m.id_medico=c.id_medico join especialidad e on e.id_especialidad=m.id_especialidad ";

    public ReportResponse generate(ReportFilter f,boolean export) {
        DataValidation.search(null,f.page,f.pageSize);
        Map<String,Object> params=new LinkedHashMap<>();
        LocalDate from=date(f.desde,"desde"),to=date(f.hasta,"hasta");
        if(from!=null && to!=null && from.isAfter(to)) throw ApplicationException.invalidData(Map.of("hasta","La fecha final debe ser igual o posterior a la inicial."));
        String where=" where 1=1";
        if(from!=null) { params.put("desde",from.atStartOfDay(ZONE).toOffsetDateTime()); where+=" and c.fecha_hora >= :desde"; }
        if(to!=null) { if(to.getYear()>9998) throw ApplicationException.invalidData(Map.of("hasta","Selecciona una fecha válida.")); params.put("hasta",to.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime()); where+=" and c.fecha_hora < :hasta"; }
        if(f.idMedico!=null) { DataValidation.id(f.idMedico); params.put("medico",f.idMedico); where+=" and m.id_medico=:medico"; }
        if(f.idEspecialidad!=null) { DataValidation.id(f.idEspecialidad); params.put("especialidad",f.idEspecialidad); where+=" and e.id_especialidad=:especialidad"; }
        if(f.idPaciente!=null) { DataValidation.id(f.idPaciente); params.put("paciente",f.idPaciente); where+=" and p.id_paciente=:paciente"; }
        if(f.estadoCita!=null && !f.estadoCita.isBlank()) { enumValue(EstadoCita.class,f.estadoCita,"estadoCita"); params.put("estadoCita",f.estadoCita); where+=" and c.estado_cita=:estadoCita"; }
        if(f.estadoOrden!=null && !f.estadoOrden.isBlank()) { enumValue(EstadoOrden.class,f.estadoOrden,"estadoOrden"); params.put("estadoOrden",f.estadoOrden); where+=" and exists (select 1 from orden ox where ox.id_cita=c.id_cita and ox.estado=:estadoOrden)"; }
        String orderFilter=params.containsKey("estadoOrden")?" and o.estado=:estadoOrden":"";
        Definition d=definition(f,where,orderFilter);
        String filter=where;
        return work.read(manager -> {
            if(f.idPaciente!=null && manager.find(com.nxn.sistemawebmedico.entity.Paciente.class,f.idPaciente)==null) throw ApplicationException.notFound();
            long total=((Number)query(manager,"select count(*) from ("+d.sql+") reporte",params).getSingleResult()).longValue();
            if(export && total>10000) throw ApplicationException.invalidData(Map.of("desde","La exportación admite hasta 10000 filas. Reduce el rango o añade filtros."));
            Query rows=query(manager,d.sql,params).setFirstResult(export?0:(f.page-1)*f.pageSize).setMaxResults(export?10000:f.pageSize);
            List<List<String>> values=new ArrayList<>();
            for(Object value:rows.getResultList()) {
                Object[] cells=value instanceof Object[] array?array:new Object[]{value};
                values.add(Arrays.stream(cells).map(cell -> cell==null?"Sin dato":cell.toString()).toList());
            }
            var indicators=new LinkedHashMap<String,String>();
            Object[] counts=(Object[])query(manager,"select count(*),count(*) filter (where c.estado_cita='NO_ASISTIO'),count(*) filter (where c.estado_cita in ('ATENDIDA','NO_ASISTIO')),count(distinct p.id_paciente)"+JOIN+filter,params).getSingleResult();
            indicators.put("Total citas",counts[0].toString());
            long denominator=((Number)counts[2]).longValue();
            indicators.put("Ausentismo",denominator==0?"Sin datos":String.format(Locale.ROOT,"%.1f %%",100.0*((Number)counts[1]).longValue()/denominator));
            indicators.put("Órdenes emitidas",query(manager,"select count(*) from orden o join cita c on c.id_cita=o.id_cita join paciente p on p.id_paciente=c.id_paciente join medico m on m.id_medico=c.id_medico join especialidad e on e.id_especialidad=m.id_especialidad"+filter+orderFilter,params).getSingleResult().toString());
            indicators.put("Pacientes con citas",counts[3].toString());
            List<ReportResponse.Bar> bars=new ArrayList<>();
            for(Object row:query(manager,"select c.estado_cita,count(*)"+JOIN+filter+" group by c.estado_cita order by c.estado_cita",params).getResultList()) {
                Object[] cells=(Object[])row; bars.add(new ReportResponse.Bar(cells[0].toString(),((Number)cells[1]).longValue()));
            }
            return new ReportResponse(d.title,d.columns,values,total,export?1:f.page,export?values.size():f.pageSize,indicators,bars,
                    "Fechas de las citas en Bogotá. Ausentismo = NO_ASISTIO / (ATENDIDA + NO_ASISTIO). Demografía: pacientes con citas en la selección; edad actual. No se calculan altas, zonas ni vencimientos. Desde: "+(from==null?"sin límite":from)+" · Hasta: "+(to==null?"sin límite":to)+" · Médico: "+f.idMedico+" · Especialidad: "+f.idEspecialidad+" · Estado cita: "+f.estadoCita+" · Estado orden: "+f.estadoOrden+" · Paciente: "+f.idPaciente);
        });
    }
    private Definition definition(ReportFilter f,String w,String of) {
        return switch(f.tipo==null?"":f.tipo) {
            case "agenda" -> new Definition("Cumplimiento de agenda",List.of("Estado","Citas"),"select c.estado_cita,count(*)"+JOIN+w+" group by c.estado_cita order by c.estado_cita");
            case "productividad" -> new Definition("Productividad médica",List.of("Médico","Especialidad","Citas atendidas","Pacientes distintos"),"select concat(m.nombre,' ',m.apellido),e.nombre,count(*),count(distinct p.id_paciente)"+JOIN+w+" and c.estado_cita='ATENDIDA' group by m.id_medico,m.nombre,m.apellido,e.nombre order by count(*) desc,m.id_medico");
            case "demanda" -> new Definition("Demanda por día y hora",List.of("Día ISO (1=lunes)","Hora Bogotá","Citas"),"select extract(isodow from c.fecha_hora at time zone 'America/Bogota')::int,extract(hour from c.fecha_hora at time zone 'America/Bogota')::int,count(*)"+JOIN+w+" group by 1,2 order by 3 desc,1,2");
            case "morbilidad" -> new Definition("Diagnósticos más frecuentes",List.of("Código CIE-10","Registros","Pacientes distintos"),"select d.codigo_cie10,count(*),count(distinct p.id_paciente) from diagnostico d join cita c on c.id_cita=d.id_cita join paciente p on p.id_paciente=c.id_paciente join medico m on m.id_medico=c.id_medico join especialidad e on e.id_especialidad=m.id_especialidad"+w+" group by d.codigo_cie10 order by count(*) desc,d.codigo_cie10");
            case "demografia" -> new Definition("Demografía de pacientes con citas",List.of("Grupo de edad actual","Sexo","Pacientes"),"select case when edad<18 then '01 · Menores de 18' when edad<40 then '02 · 18 a 39' when edad<60 then '03 · 40 a 59' else '04 · 60 o más' end,sexo,count(*) from (select distinct p.id_paciente,p.sexo,extract(year from age((current_timestamp at time zone 'America/Bogota')::date,p.fecha_nacimiento)) as edad"+JOIN+w+") pacientes group by 1,2 order by 1,2");
            case "ordenes" -> new Definition("Órdenes por tipo, médico y estado",List.of("Médico","Tipo","Estado","Órdenes"),"select concat(m.nombre,' ',m.apellido),o.tipo_orden,o.estado,count(*) from orden o join cita c on c.id_cita=o.id_cita join paciente p on p.id_paciente=c.id_paciente join medico m on m.id_medico=c.id_medico join especialidad e on e.id_especialidad=m.id_especialidad"+w+of+" group by m.id_medico,m.nombre,m.apellido,o.tipo_orden,o.estado order by count(*) desc,m.id_medico,o.tipo_orden,o.estado");
            case "cumplimiento" -> new Definition("Cumplimiento de órdenes",List.of("Estado","Órdenes","Porcentaje"),"select o.estado,count(*),round(100.0*count(*)/sum(count(*)) over (),1) from orden o join cita c on c.id_cita=o.id_cita join paciente p on p.id_paciente=c.id_paciente join medico m on m.id_medico=c.id_medico join especialidad e on e.id_especialidad=m.id_especialidad"+w+of+" group by o.estado order by o.estado");
            case "servicios" -> new Definition("Servicios y medicamentos solicitados",List.of("Origen","Servicio o medicamento","Cantidad"),"select origen,nombre,sum(cantidad) from (select 'Orden' as origen,dt.descripcion as nombre,dt.cantidad as cantidad from detalle_orden dt join orden o on o.id_orden=dt.id_orden join cita c on c.id_cita=o.id_cita join paciente p on p.id_paciente=c.id_paciente join medico m on m.id_medico=c.id_medico join especialidad e on e.id_especialidad=m.id_especialidad"+w+of+" union all select 'Tratamiento',t.medicamento,1 from tratamiento t join diagnostico d on d.id_diagnostico=t.id_diagnostico join cita c on c.id_cita=d.id_cita join paciente p on p.id_paciente=c.id_paciente join medico m on m.id_medico=c.id_medico join especialidad e on e.id_especialidad=m.id_especialidad"+w+") servicios group by origen,nombre order by sum(cantidad) desc,origen,nombre");
            case "historia" -> {
                if(f.idPaciente==null) throw ApplicationException.invalidData(Map.of("idPaciente","Selecciona un paciente para su resumen clínico."));
                String context=" join cita c on c.id_cita=d.id_cita join paciente p on p.id_paciente=c.id_paciente join medico m on m.id_medico=c.id_medico join especialidad e on e.id_especialidad=m.id_especialidad";
                // La historia base no tiene médico ni cita; se incluye una vez, sin filtrar su fecha de apertura.
                yield new Definition("Resumen individual de historia clínica",List.of("Fecha","Sección","Información"),
                    "select fecha,seccion,informacion from (select h.fecha_apertura::text as fecha,'Historia base' as seccion,concat(p.nombre,' ',p.apellido,' · Grupo: ',h.grupo_sanguineo,' · Antecedentes: ',coalesce(h.antecedentes,'Sin registrar'),' · Alergias: ',coalesce(h.alergias,'Sin registrar')) as informacion from historia_clinica h join paciente p on p.id_paciente=h.id_paciente where p.id_paciente=:paciente"
                    +" union all select to_char(c.fecha_hora at time zone 'America/Bogota','YYYY-MM-DD HH24:MI'),'Consulta',concat(m.nombre,' ',m.apellido,' · ',c.estado_cita,' · ',c.motivo)"+JOIN+w
                    +" union all select to_char(c.fecha_hora at time zone 'America/Bogota','YYYY-MM-DD HH24:MI'),'Diagnóstico',concat(d.codigo_cie10,' · ',d.tipo,' · ',d.descripcion) from diagnostico d"+context+w
                    +" union all select to_char(c.fecha_hora at time zone 'America/Bogota','YYYY-MM-DD HH24:MI'),'Tratamiento',concat(t.medicamento,' · Dosis: ',t.dosis,' · Frecuencia: ',t.frecuencia,' · Días: ',t.duracion_dias,' · ',t.indicaciones) from tratamiento t join diagnostico d on d.id_diagnostico=t.id_diagnostico"+context+w
                    +" union all select o.fecha_orden::text,'Orden',concat('#',o.id_orden,' · ',o.tipo_orden,' · ',o.estado,' · ',o.observaciones,' · Detalle: ',dt.descripcion,' · Cantidad: ',dt.cantidad,' · ',dt.indicaciones) from orden o join detalle_orden dt on dt.id_orden=o.id_orden join cita c on c.id_cita=o.id_cita join paciente p on p.id_paciente=c.id_paciente join medico m on m.id_medico=c.id_medico join especialidad e on e.id_especialidad=m.id_especialidad"+w+of+") resumen order by fecha,seccion,informacion");
            }
            default -> throw ApplicationException.invalidData(Map.of("tipo","Selecciona un reporte válido."));
        };
    }
    private Query query(EntityManager manager,String sql,Map<String,Object> params) { var query=manager.createNativeQuery(sql); params.forEach(query::setParameter); return query; }
    private LocalDate date(String value,String field) { if(value==null || value.isBlank()) return null; try { return LocalDate.parse(value); } catch(DateTimeException e) { throw ApplicationException.invalidData(Map.of(field,"Ingresa una fecha válida.")); } }
    private <E extends Enum<E>> void enumValue(Class<E> type,String value,String field) { try { Enum.valueOf(type,value); } catch(IllegalArgumentException e) { throw ApplicationException.invalidData(Map.of(field,"Selecciona un estado válido.")); } }
}
