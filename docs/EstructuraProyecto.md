Archivo para tener en cuenta lo que debe tener el sistema:

Este proyecto va consistir en una plataforma web médica, en donde se van a poder cumplir diferentes tareas, como: 

- Registrar pacientes (Con: Tipo de Documento, Numero de Documento, Nombres, Apellidos, Fecha de Nacimiento, Sexo, Dirección, Telefono y Email).

- Registrar Medicos (Con: Numero documento, Nombres, Apellidos, Registro Medico, Telefono, Email, Especialidad (Las especialidades se tienen que crear por aparte, porque esta sección es para que se elija en base a las que están creadas))

- Registrar Orden (Con: Tipo de Orden (Que va ser un enum con: Laboratorio, Imagenologia, Medicamento, Remision), Estado, Observación, Cita (Aquí también debe haber ya citas creadas para registrar la orden en base a la cita), Fecha de Orden)

- Registrar Citas (Con: fechaHora, motivo, estadoCita (con enum de: PROGRAMADA, CONFIRMADA, ATENDIDA, CANCELADA, NO_ASISTIO), Paciente registrado a la cita, Medico registrado a la cita).

- Registrar historia clinica (Con: Paciente al que esta registrada esa Historia Clinica, Fecha apertura de la historia, Grupo Sanguinero, Antecedentes, Alergias)

- Registrar Diagnostico (Con: codigoCie10, Descripcion, Tipo de Diagnostico, Cita a la que esta vinculado ese diagnostico)

- Registrar Tratamiento (Con: Medicamento, Dosis, Frecuencia, DuracionDias, Indicaciones, El diagnostico al que esta vinculado).

- Registrar Detalle de Orden (Con: Descripción, Cantidad, Indicaciones y Orden a la que ese detalle esta vinculado)

- Generar reportes va ser otro módulo, pero ese vas a tener que ir a moduloReportes.md (Este módulo se va trabajar cuando yo te diga)


RELACIONES DE CADA UNO DE LOS MÓDULOS. RECUERDA QUE ESOS MÓDULOS SON ENTIDADES EN LA BASE DE DATOS: PACIENTES, MEDICOS, ESPECIALIDAD, CITAS, HISTORIACLINICA, DIAGNOSTICO, ORDEN, TRATAMIENTO, DETALLEORDEN.

Las relaciones son: 

- UNA especialidad pertenece a MUCHOS médicos.
- UN medico atiende MUCHAS citas
- UN paciente agenda MUCHAS citas
- UN paciente tiene UNA historia clinica
- UNA cita registra MUCHOS diagnosticos
- UNA cita genera MUCHAS ordenes
- UN diagnostico preescribe MUCHOS tratamientos
- UNA orden puede tener UNO O MUCHOS detalles de orden.

(En el archivo schema.sql están las entidades que se crearan en la base de datos).

