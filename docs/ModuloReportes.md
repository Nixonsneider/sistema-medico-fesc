# Módulo de reportes

Implementado sobre las nueve tablas actuales. Por decisión del usuario, no se amplía el esquema para esta fase. La revisión manual final se hará posteriormente.

## Uso e interfaz

Seleccionar reporte y filtros; pulsar **Generar**. No se consultan reportes al abrir la pantalla ni al cambiar filtros. Médico, especialidad y paciente se buscan mediante **Buscar opciones**, hasta 100 coincidencias. El resumen de historia exige paciente.

Se muestran indicadores, gráfico de citas por estado y tabla paginada de 20 filas. Al cambiar filtros se oculta el resultado hasta generar de nuevo. Las páginas conservan la selección.

Se exporta la selección completa, no solo la página, como Excel `.xlsx`, CSV UTF-8 o PDF paginado con título, filtros e indicadores. Límite de 10000 filas; reducir la selección si se supera. Excel usa texto explícito, sin fórmulas; para celdas de más de 32767 caracteres usar CSV o PDF.

## Fechas y filtros

El rango se aplica a **fecha/hora de la cita**, por días completos de Bogotá, incluyendo ambos extremos. Esto también rige para diagnósticos, tratamientos y órdenes ligados a esa cita: no se filtran órdenes por su fecha de emisión. Los extremos son opcionales.

Los filtros se combinan con AND. El estado de orden selecciona citas con al menos una orden en ese estado; las tablas de órdenes además limitan las órdenes y sus detalles. Médico y especialidad corresponden a la asociación actual del médico. El paciente opcional limita todos los reportes.

## Indicadores

| Indicador | Cálculo |
| --- | --- |
| Total citas | Citas que cumplen los filtros. |
| Ausentismo | `NO_ASISTIO / (ATENDIDA + NO_ASISTIO) × 100`; sin denominador: «Sin datos». No incluye canceladas en el denominador. |
| Órdenes emitidas | Órdenes de las citas seleccionadas, respetando estado si se filtra. Incluye canceladas cuando no se filtran. |
| Pacientes con citas | Pacientes distintos de la selección. Sustituye «Pacientes nuevos», pues no existe fecha de registro. |

Los indicadores y el gráfico resumen describen las citas seleccionadas independientemente de la tabla. Productividad puede estar vacía si no hay citas atendidas aunque existan programadas.

## Reportes disponibles

| Reporte (`tipo` de API) | Información |
| --- | --- |
| Agenda (`agenda`) | Número de citas por estado. |
| Productividad (`productividad`) | Citas atendidas y pacientes distintos por médico y especialidad. |
| Demanda horaria (`demanda`) | Cantidad por día ISO (1=lunes, 7=domingo) y hora de Bogotá, en orden de demanda. Todos los estados si no se filtran. |
| Morbilidad (`morbilidad`) | Frecuencia de códigos CIE-10 registrados y pacientes distintos. No interpreta ni valida el catálogo. |
| Demografía (`demografia`) | Pacientes distintos con citas seleccionadas, por sexo y edad actual: menores de 18, 18–39, 40–59 y 60 o más. No representa toda la base ni edad histórica. |
| Órdenes (`ordenes`) | Volumen por médico, tipo y estado. Tipos actuales: laboratorio, imagenología, medicamento y remisión. |
| Cumplimiento (`cumplimiento`) | Conteo y porcentaje por estado (pendiente, completada, cancelada) respecto al total de órdenes seleccionadas. |
| Servicios y medicamentos (`servicios`) | Suma de cantidades de detalles de órdenes y número de tratamientos por medicamento, separados por origen. No suma dosis ni infiere unidades dispensadas. Textos iguales se agrupan exactamente. |
| Historia individual (`historia`) | Historia base, consultas, diagnósticos, tratamientos y órdenes con detalles. Exportable a PDF, Excel y CSV. La historia base aparece una vez sin filtrar fecha de apertura; los eventos respetan los filtros. Si no hay historia, se muestran los eventos disponibles sin inventarla. |

No se calculan zonas, pacientes nuevos ni vencimientos porque no existen esos campos. No se añaden terapias o interconsultas como tipos nuevos. «Completada» refleja el estado registrado por el usuario.

## API

- `GET /resources/reportes`: título, columnas, filas, total, página, indicadores, gráfico y alcance.
- `GET /resources/reportes/exportar?formato=csv|xlsx|pdf`: archivo con la selección hasta el límite.
- Parámetros comunes: `tipo`, `desde`, `hasta`, `idMedico`, `idEspecialidad`, `estadoCita`, `estadoOrden`, `idPaciente`. Fechas `AAAA-MM-DD`. Paginación: `page`, `pageSize` (1–100).
- Consultas parametrizadas de lectura; respuestas sin caché. No se modifica el esquema.
- Los reportes reflejan registros actuales. Consultar o exportar después puede reflejar cambios posteriores; no son instantáneas históricas inmutables.

## Auditoría

Cada archivo generado registra un evento en el logger Java `medico.auditoria.exportaciones`: fecha UTC, reporte, formato, cantidad de filas y principal autenticado si el servidor lo proporciona. Actualmente se registra `SIN_AUTENTICACION`, pues no existe módulo de usuarios; no se solicita ni inventa identidad.

Se consulta en los logs de Tomcat según su configuración de logging. No se registran nombres de pacientes ni contenido clínico. Se audita generación, no la finalización de descarga del navegador. Una auditoría por usuario verificable requiere autenticación en una fase posterior.