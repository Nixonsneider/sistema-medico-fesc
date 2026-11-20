# Plan de desarrollo

Estado: etapas 1, 2 y 3 completadas. El usuario confirmó la conexión a su base mediante la respuesta `database OK` al ejecutar el proyecto desde NetBeans. Las etapas 4 a 11 siguen pendientes.

El plan se basa en los requisitos de `docs/EstructuraProyecto.md`, el esquema `schema.sql` y la configuración actual del proyecto. Se avanzará módulo por módulo, dejando cada uno funcional y verificable antes de continuar.

## 1. Unificar los requisitos y el esquema de datos

- [x] Resolver las diferencias entre documentación y SQL para que Java, la base de datos y los formularios manejen los mismos valores.
- [x] Adoptar los estados de cita de la documentación: `PROGRAMADA`, `CONFIRMADA`, `ATENDIDA`, `CANCELADA` y `NO_ASISTIO`, con `PROGRAMADA` como valor inicial.
- [x] Revisar campos obligatorios, valores predeterminados, fechas y reglas de eliminación.
- [x] Actualizar la documentación y el esquema con las reglas unificadas.

**Resultado esperado:** documentación y SQL coherentes.

**Resultado de la etapa:** reglas documentadas en `docs/EstructuraProyecto.md` y resumen actualizado en `README.md`. En `schema.sql` se unificaron los estados de cita, se eliminó el valor vacío predeterminado de `tipo_documento` y se rechazaron valores vacíos o formados solo por espacios ordinarios; además, se ajustó `fecha_orden` a `DEFAULT CURRENT_DATE`. Se conservaron las restricciones de unicidad y las reglas de eliminación existentes. No se modificó ninguna base de datos en ejecución.

## 2. Dejar funcionando la base técnica

- [x] Verificar las versiones de Java, Jakarta EE y el servidor configurado en NetBeans: JDK 21.0.7, compilación con `release 17`, Jakarta EE API 11 estable y Tomcat 11.0.29.
- [x] Resolver las implementaciones que necesita Tomcat: Jersey, Weld y Hibernate incluidos en el WAR.
- [x] Configurar el controlador JDBC, el pool HikariCP y la unidad de persistencia con transacciones locales, sin generación automática del esquema.
- [x] Mantener las credenciales fuera del repositorio mediante archivo externo y exclusión en `.gitignore`.
- [x] Comprobar compilación, despliegue y conexión en una base temporal aislada: WAR generado, CDI iniciado y endpoints REST y JPA con respuesta HTTP 200.
- [x] Completar localmente `db.password` en `config/database.properties` y verificar la conexión a la base del usuario `sistema-web-medico`, con usuario `postgres`.

**Resultado esperado:** aplicación compilable, desplegable y conectada a la base de datos.

**Verificación realizada:** PostgreSQL 18 temporal en el puerto 55432 y Tomcat 11 temporal en el puerto 18080, dentro de `target/verification`. Se ejecutó `schema.sql` y se comprobaron `GET /resources/jakartaee11` y `GET /resources/health/db`. No se modificó la base existente del usuario. La configuración de NetBeans y la conexión local se explican en [Configuración local](docs/ConfiguracionLocal.md).

## 3. Organizar el código con una estructura sencilla

- [x] Mantener una sola aplicación Maven.
- [x] Organizar las responsabilidades en los siguientes paquetes, dentro de `com.nxn.sistemawebmedico`:

| Paquete | Responsabilidad |
| --- | --- |
| `config` | Configuración técnica y ciclo de vida de persistencia. |
| `entity` | Entidades y relaciones de persistencia. |
| `enums` | Estados y tipos del dominio. |
| `repository` | Consultas y acceso a datos. |
| `service` | Validaciones, reglas y transacciones. |
| `resources` | Endpoints REST. |

- [x] Mantener las vistas en `webapp`, con HTML, CSS y JavaScript.

**Resultado esperado:** organización común para todos los módulos, sin incorporar estructuras innecesarias.

**Resultado de la etapa:** seis paquetes documentados mediante `package-info.java`, estructura y flujo de responsabilidades actualizados en `README.md`, un solo proyecto Maven y las vistas conservadas en `src/main/webapp`. Los paquetes `entity`, `enums`, `repository` y `service` quedan preparados para las siguientes etapas sin adelantar su implementación. Se conservan la configuración REST y los endpoints existentes.

## 4. Crear el modelo de persistencia

- [ ] Mapear las nueve tablas a entidades Java, respetando nombres de columnas, claves únicas y relaciones.
- [ ] Usar enumeraciones almacenadas como texto.
- [ ] Definir el tratamiento de fecha y hora de las citas.
- [ ] Evitar eliminaciones en cascada que borren información clínica relacionada.

**Resultado esperado:** modelo Java alineado con `schema.sql`.

## 5. Preparar la interfaz y el manejo común de errores

- [ ] Leer y aplicar las [directrices de diseño médico](.agents/skills/medical-ui-design/SKILL.md) antes de trabajar en interfaces.
- [ ] Revisar las vistas y estilos existentes.
- [ ] Crear la navegación y los estilos compartidos de formularios, tablas, botones y estados.
- [ ] Incorporar etiquetas visibles, validaciones comprensibles, navegación por teclado y adaptación a distintas resoluciones.
- [ ] Unificar las respuestas para datos inválidos, duplicados, registros inexistentes y relaciones que impiden eliminar.

**Resultado esperado:** una base visual y de interacción consistente.

## 6. Implementar especialidades y médicos

- [ ] Implementar primero especialidades, porque los médicos dependen de ellas.
- [ ] Definir el uso del campo `activo` de especialidades.
- [ ] Desarrollar registro, listado, consulta y edición de médicos, seleccionando una especialidad existente.
- [ ] Validar documento, registro médico y correo únicos.

**Resultado esperado:** médicos disponibles para asignarlos a las citas.

## 7. Implementar pacientes e historias clínicas

- [ ] Desarrollar registro, consulta, edición y búsqueda de pacientes.
- [ ] Incorporar la creación y consulta de su historia clínica con grupo sanguíneo, antecedentes y alergias.
- [ ] Impedir más de una historia clínica por paciente.
- [ ] Permitir acceder a la información relacionada desde la consulta del paciente.

**Resultado esperado:** pacientes registrados con su historia clínica asociada.

## 8. Implementar citas

- [ ] Concretar las reglas de disponibilidad y los cambios de estado permitidos antes de programarlos; la documentación todavía no los define.
- [ ] Permitir seleccionar paciente y médico existentes y registrar fecha, hora y motivo.
- [ ] Incorporar consulta de citas y actualización de su estado.
- [ ] Añadir los filtros básicos necesarios.

**Resultado esperado:** gestión de citas con relaciones y estados consistentes.

## 9. Implementar diagnósticos y tratamientos

- [ ] Registrar diagnósticos desde una cita.
- [ ] Registrar tratamientos desde un diagnóstico.
- [ ] Validar código CIE-10 obligatorio, descripción, tipo, medicamento, dosis, frecuencia y duración positiva.
- [ ] Registrar el código CIE-10 como dato; una integración con un catálogo externo requeriría ampliar el alcance.

**Resultado esperado:** información clínica vinculada a la atención correspondiente.

## 10. Implementar órdenes y detalles

- [ ] Registrar órdenes desde una cita y añadir sus detalles.
- [ ] Validar tipo, estado, cantidades e indicaciones.
- [ ] Guardar la orden y sus detalles en una misma transacción.
- [ ] Exigir al menos un detalle para cumplir el requisito funcional.

**Resultado esperado:** órdenes completas, sin registros parciales.

## 11. Verificar el recorrido completo y actualizar la documentación

- [ ] Comprobar el flujo: especialidad → médico → paciente → historia clínica → cita → diagnóstico → tratamiento y orden con detalles.
- [ ] Verificar persistencia, duplicados, relaciones inválidas, estados, errores y restricciones de eliminación.
- [ ] Añadir pruebas automatizadas para las reglas y transacciones principales.
- [ ] Revisar las interfaces según las directrices de diseño médico.
- [ ] Actualizar el README con la ejecución real.

**Resultado esperado:** primera versión integrada y documentada.

## Alcance pendiente

- **Reportes:** queda pendiente hasta que el usuario lo indique, como establece la documentación. Su especificación está en [Módulo de reportes](docs/ModuloReportes.md).
- **Usuarios, roles y autenticación:** no forman parte de estos requisitos; su incorporación requeriría definir ese alcance.

Las propuestas técnicas y las reglas aún no definidas se concretarán antes de implementar las tareas correspondientes.
