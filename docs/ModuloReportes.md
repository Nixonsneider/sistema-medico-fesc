# Módulo de Reportes - Sistema Médico

## 1. Estructura de la Interfaz (UX)
* **Filtros globales:** Rango de fechas, médico, especialidad y estado de cita/orden.
* **KPIs superiores:** Tarjetas resumen dinámicas (Total citas, % Ausentismo, Órdenes emitidas, Pacientes nuevos).
* **Vista:** Gráfico resumen + tabla paginada.
* **Exportación:** **Excel/CSV** (auditoría/datos) y **PDF** (gerencia e historias clínicas).

---

## 2. Reportes Clave por Área

### Operativos (Recepción / Gerencia)
* **Cumplimiento de agenda:** Citas atendidas vs. canceladas vs. ausentismo (*No-Show*).
* **Productividad médica:** Cantidad de pacientes atendidos por doctor y especialidad.
* **Demanda horaria:** Días y horas pico de atención.

### Clínicos (Dirección Médica)
* **Morbilidad (Top diagnósticos):** Enfermedades más frecuentes (basado en CIE-10).
* **Demografía:** Distribución de pacientes por edad, sexo y zona.
* **Resumen de Historia Clínica:** Consolidado individual en PDF (consultas, diagnósticos y órdenes de un paciente).

### Órdenes y Seguimiento
* **Órdenes por tipo y médico:** Volumen de laboratorios, imágenes, terapias e interconsultas generadas.
* **Tasa de cumplimiento:** Órdenes emitidas vs. realizadas vs. pendientes/vencidas.
* **Top servicios/medicamentos:** Exámenes y fármacos más solicitados.

---

## 3. Reglas Técnicas
* **Carga bajo demanda:** Ejecutar consultas solo al presionar *"Generar"* para cuidar el rendimiento.
* **Auditoría:** Registrar en un `log` quién, cuándo y qué reporte exportó (seguridad de datos clínicos).