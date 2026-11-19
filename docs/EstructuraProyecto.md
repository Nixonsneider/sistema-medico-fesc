# Requisitos y modelo de datos

La plataforma permitirá gestionar especialidades, médicos, pacientes, citas, historias clínicas, diagnósticos, tratamientos y órdenes médicas con sus detalles.

Este documento define las reglas funcionales de la primera versión. [schema.sql](../schema.sql) define sus tablas, nombres de columnas, tipos, longitudes y restricciones. Los futuros modelos Java y formularios deberán respetar ambos archivos. Los módulos aún no están implementados.

## Campos por entidad

Todas las tablas tienen una clave primaria entera generada automáticamente; no la introduce el usuario. Los nombres de la tabla siguiente corresponden a las columnas SQL.

Los campos obligatorios deben tener un valor; los textos obligatorios no deben enviarse vacíos ni contener únicamente espacios. En SQL, `NOT NULL` impide valores nulos; la validación general de textos se implementará en la aplicación. `tipo_documento` tiene además una restricción SQL que rechaza textos vacíos o formados únicamente por espacios ordinarios.

| Entidad | Campos obligatorios, además de su clave primaria | Campos opcionales | Valores predeterminados si se omiten al insertar |
| --- | --- | --- | --- |
| `especialidad` | `nombre`, `activo` | `descripcion` | `activo = TRUE` |
| `medico` | `documento`, `nombre`, `apellido`, `registro_medico`, `telefono`, `email`, `id_especialidad` | Ninguno | Ninguno |
| `paciente` | `tipo_documento`, `documento`, `nombre`, `apellido`, `fecha_nacimiento`, `sexo`, `direccion`, `telefono`, `email` | Ninguno | Ninguno |
| `cita` | `fecha_hora`, `motivo`, `estado_cita`, `id_paciente`, `id_medico` | Ninguno | `estado_cita = PROGRAMADA` |
| `historia_clinica` | `fecha_apertura`, `grupo_sanguineo`, `id_paciente` | `antecedentes`, `alergias` | `fecha_apertura = CURRENT_DATE` |
| `orden` | `tipo_orden`, `estado`, `fecha_orden`, `observaciones`, `id_cita` | Ninguno | `estado = PENDIENTE`, `fecha_orden = CURRENT_DATE` |
| `diagnostico` | `codigo_cie10`, `descripcion`, `tipo`, `id_cita` | Ninguno | `tipo = PRESUNTIVO` |
| `tratamiento` | `medicamento`, `dosis`, `frecuencia`, `duracion_dias`, `id_diagnostico` | `indicaciones` | Ninguno |
| `detalle_orden` | `descripcion`, `cantidad`, `id_orden` | `indicaciones` | `cantidad = 1` |

Los valores predeterminados no sustituyen un `NULL` explícito. `tipo_documento` debe indicarse expresamente y no tiene un catálogo cerrado definido por ahora. Se mantienen como obligatorios el contacto del paciente y del médico y las observaciones de la orden, conforme al esquema.

## Estados y valores permitidos

Estos valores se almacenarán como texto. Los estados y tipos deberán coincidir exactamente entre SQL y las futuras enumeraciones Java; sexo y grupo sanguíneo conservan los valores cerrados del esquema.

| Campo | Valores permitidos | Valor inicial |
| --- | --- | --- |
| `cita.estado_cita` | `PROGRAMADA`, `CONFIRMADA`, `ATENDIDA`, `CANCELADA`, `NO_ASISTIO` | `PROGRAMADA` |
| `orden.tipo_orden` | `LABORATORIO`, `IMAGENOLOGIA`, `MEDICAMENTO`, `REMISION` | Sin valor predeterminado |
| `orden.estado` | `PENDIENTE`, `COMPLETADA`, `CANCELADA` | `PENDIENTE` |
| `diagnostico.tipo` | `PRESUNTIVO`, `DEFINITIVO`, `PRINCIPAL`, `SECUNDARIO` | `PRESUNTIVO` |
| `paciente.sexo` | `Masculino`, `Femenino`, `Otro` | Sin valor predeterminado |
| `historia_clinica.grupo_sanguineo` | `A+`, `A-`, `B+`, `B-`, `AB+`, `AB-`, `O+`, `O-` | Sin valor predeterminado |

Para citas se adoptan los estados de los requisitos originales. `PENDIENTE` y `COMPLETADA` no son estados de cita; siguen siendo válidos para órdenes. La agenda y el futuro cálculo de ausentismo usarán `NO_ASISTIO`.

Las transiciones permitidas entre estados y las reglas de disponibilidad se definirán en la etapa de citas. El esquema solo restringe los valores admitidos; no controla esas transiciones ni evita cruces de agenda.

## Fechas y horas

- `paciente.fecha_nacimiento`, `historia_clinica.fecha_apertura` y `orden.fecha_orden` son `DATE`: representan fechas de calendario, sin hora.
- La fecha de apertura y la fecha de orden se completan con `CURRENT_DATE` cuando se omiten. La fecha de nacimiento debe suministrarse.
- `cita.fecha_hora` es `TIMESTAMPTZ`: representa un instante de la cita. Las futuras entradas deberán incluir zona o desplazamiento para evitar interpretaciones ambiguas; PostgreSQL no conserva el nombre de la zona original.
- Se utilizará `America/Bogota` como zona de referencia de la aplicación. La conexión y la presentación de fechas deberán configurarse con esa referencia en las etapas técnicas correspondientes. `CURRENT_DATE` depende de la zona de la sesión de base de datos.

## Relaciones

| Relación | Regla |
| --- | --- |
| Especialidad → médicos | Una especialidad puede tener muchos médicos; cada médico debe tener una especialidad existente. |
| Médico → citas | Un médico puede atender muchas citas; cada cita debe tener un médico existente. |
| Paciente → citas | Un paciente puede tener muchas citas; cada cita debe tener un paciente existente. |
| Paciente → historia clínica | Cada historia pertenece a un paciente existente. Un paciente puede tener como máximo una historia clínica. |
| Cita → diagnósticos | Una cita puede registrar muchos diagnósticos; cada diagnóstico pertenece a una cita existente. |
| Cita → órdenes | Una cita puede generar muchas órdenes; cada orden pertenece a una cita existente. |
| Diagnóstico → tratamientos | Un diagnóstico puede tener muchos tratamientos; cada tratamiento pertenece a un diagnóstico existente. |
| Orden → detalles | Cada detalle pertenece a una orden existente. Una orden debe guardarse con al menos un detalle. |

El esquema permite registrar un paciente antes de abrir su historia clínica. La restricción `UNIQUE` sobre `historia_clinica.id_paciente` impide una segunda historia, pero no obliga a crear la primera automáticamente.

El mínimo de un detalle por orden se implementará en el servicio, guardando la orden y sus detalles en una misma transacción. La clave foránea por sí sola no impide una orden sin detalles; esta regla queda pendiente de implementación en la etapa de órdenes.

## Unicidad, cantidades y eliminación

- `especialidad.nombre` es único.
- En médicos son únicos `documento`, `registro_medico` y `email`.
- En pacientes son únicos `documento` y `email`. Se conserva la unicidad de documento por sí solo, sin combinarlo con `tipo_documento`.
- La unicidad de médicos y pacientes se aplica dentro de cada tabla; no existe una restricción compartida entre ambas.
- `tratamiento.duracion_dias` y `detalle_orden.cantidad` deben ser enteros mayores que cero.
- Todas las claves foráneas usan `ON DELETE RESTRICT`: no se puede eliminar un registro mientras otro lo referencie. No se borrará información relacionada en cascada.
- Las claves foráneas conservan `ON UPDATE CASCADE`. Las claves primarias autogeneradas no serán campos editables en la aplicación.
- `especialidad.activo` conserva su valor inicial `TRUE`. El comportamiento de las especialidades inactivas se definirá al implementar ese módulo.

## Alcance y pendientes

La creación del esquema se realiza sobre una base PostgreSQL vacía. `schema.sql` no migra bases existentes ni convierte datos con los estados anteriores de citas.

El módulo de reportes se desarrollará únicamente cuando el usuario lo indique. Su propuesta está en [ModuloReportes.md](ModuloReportes.md). Esa propuesta contempla datos todavía no modelados, como fecha de registro del paciente, zona demográfica, vencimiento de órdenes e identificación del usuario que exporta. También menciona terapias e interconsultas, que no son valores adicionales de `tipo_orden` en esta versión. Estas diferencias se resolverán al definir el alcance de reportes, sin ampliar ahora las nueve entidades.

Usuarios, roles y autenticación no están definidos en estos requisitos.
