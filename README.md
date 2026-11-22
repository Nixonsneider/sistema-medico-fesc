# Sistema Web Médico

Proyecto de una plataforma web para gestionar pacientes, médicos, especialidades, citas y la información clínica asociada: historias clínicas, diagnósticos, tratamientos y órdenes médicas.

## Estado actual

El repositorio contiene una aplicación Java web con Maven y Jakarta, una interfaz base con navegación, endpoints técnicos, el modelo JPA y el esquema SQL de los módulos previstos.

El modelo de persistencia incluye las nueve entidades JPA y cuatro enumeraciones. Especialidades, médicos y pacientes cuentan con registro, listado, consulta y edición. También están disponibles la historia clínica única de cada paciente, la agenda de citas, diagnósticos, tratamientos y órdenes médicas con sus detalles. Las etapas 1 a 11 de TASKS.md están implementadas y cuentan con una comprobación técnica breve; la revisión manual final del usuario queda para después. Reportes sigue pendiente. La persistencia usa Hibernate y un pool HikariCP, con conexión PostgreSQL configurada en un archivo externo.

## Tecnologías y configuración

| Elemento | Configuración del repositorio |
| --- | --- |
| Java | Compilación con nivel de lenguaje y bytecode Java 17 |
| Maven | Gestión de dependencias y construcción del proyecto |
| Jakarta Servlet API | `6.1.0`, con alcance `provided` (implementada por Tomcat) |
| Jakarta REST | Jersey 4.0.3 y proveedor JSON Jackson |
| Conexiones | HikariCP 7.0.2 y PostgreSQL JDBC 42.7.14 |
| Jakarta Persistence | Hibernate ORM 7.4.13.Final; transacciones `RESOURCE_LOCAL` |
| CDI | Weld Servlet 6.0.3.Final |
| Empaquetado | WAR: `sistemaWebMedico-1.0-SNAPSHOT.war` |
| Base de datos | Esquema SQL con sintaxis de PostgreSQL |
| IDE | Configuración compartida de Apache NetBeans |

El servidor de referencia es Tomcat 11 con JDK 17 o superior (verificado con JDK 21). El WAR incluye Jersey, Weld, Hibernate, HikariCP y el controlador PostgreSQL. La API Servlet es `provided`; las APIs REST, Persistence y CDI llegan con sus implementaciones. Tomcat no ofrece la plataforma Jakarta EE completa. Las transacciones JPA serán locales y gestionadas explícitamente por la aplicación.

## Estructura del proyecto

```text
sistemaWebMedico/
├── .agents/skills/medical-ui-design/  # Directrices de diseño de interfaces
├── config/
│   └── database.properties.example  # Plantilla de conexión sin credenciales reales
├── docs/
│   ├── ConfiguracionLocal.md         # Conexión, NetBeans y ejecución local
│   ├── EstructuraProyecto.md         # Requisitos funcionales y relaciones
│   └── ModuloReportes.md             # Especificación del módulo de reportes
├── src/main/
│   ├── java/com/nxn/sistemawebmedico/
│   │   ├── JakartaRestConfiguration.java
│   │   ├── config/                  # Configuración y ciclo de vida de persistencia
│   │   ├── dto/                     # Contratos de entrada/salida y páginas
│   │   ├── entity/                  # Nueve entidades JPA
│   │   ├── enums/                   # Cuatro enumeraciones de estados y tipos
│   │   ├── error/                   # Contrato JSON y traducción de errores
│   │   ├── repository/              # Consultas de catálogos, agenda e información clínica
│   │   ├── service/                 # Validaciones y transacciones de los módulos
│   │   └── resources/
│   │       ├── EspecialidadResource.java
│   │       ├── MedicoResource.java
│   │       ├── PacienteResource.java
│   │       ├── CitaResource.java
│   │       ├── DiagnosticoResource.java
│   │       ├── TratamientoResource.java
│   │       ├── OrdenResource.java
│   │       ├── DatabaseHealthResource.java
│   │       └── JakartaEE11Resource.java
│   ├── resources/META-INF/
│   │   └── persistence.xml           # Unidad JPA con proveedor Hibernate
│   └── webapp/
│       ├── index.html                # Inicio, búsqueda y navegación por módulos
│       ├── assets/
│       │   ├── css/app.css            # Estilos compartidos y diseño adaptable
│       │   └── js/                   # Navegación, cliente REST y utilidades de formularios
│       ├── META-INF/context.xml      # Contexto /sistemaWebMedico
│       └── WEB-INF/
│           ├── beans.xml             # Configuración de CDI
│           └── web.xml               # Descriptor web; sesión de 30 minutos
├── AGENTS.md                         # Instrucciones de trabajo del proyecto
├── nb-configuration.xml              # Configuración compartida de NetBeans
├── pom.xml                           # Dependencias y plugins de Maven
├── TASKS.md                          # Plan y seguimiento del desarrollo
└── schema.sql                        # Creación de las nueve tablas
```

Maven genera los archivos compilados y el WAR en `target/`.

### Organización del código

Se mantiene una sola aplicación Maven con un único `pom.xml` y un WAR. Los paquetes Java están dentro de `com.nxn.sistemawebmedico`; cada paquete tiene un `package-info.java` con su responsabilidad.

El flujo previsto para los módulos es `resources → service → repository`, utilizando las entidades de `entity` y los tipos de `enums`. Los servicios delimitarán las transacciones locales; los repositorios realizarán las consultas con `EntityManager`. `config` mantiene el ciclo de vida de la fábrica JPA y del pool. `JakartaRestConfiguration` conserva la configuración REST en el paquete raíz y la ruta `/resources`.

El modelo JPA está implementado. Los repositorios y servicios de especialidades y médicos realizan las consultas y transacciones; el paquete `dto` separa los datos de entrada y salida de las entidades persistentes. El paquete `error` define el contrato público de errores y su traducción HTTP. Los endpoints técnicos existentes conservan su comportamiento. Las vistas permanecen en `src/main/webapp`, con HTML, CSS y JavaScript; no se incorpora otro proyecto frontend ni nuevas dependencias en esta etapa.

## Interfaz y errores comunes

La página inicial ofrece navegación lateral, un listado de módulos con búsqueda por nombre o descripción y vistas que indican su disponibilidad. La búsqueda del listado inicial admite texto sin tildes. Especialidades, médicos, pacientes, historias clínicas, citas, diagnósticos, tratamientos y órdenes están disponibles con datos de la base configurada. Reportes permanece pendiente.

`assets/css/app.css` contiene estilos comunes para tablas, formularios, botones, avisos, indicadores, estados vacíos y diálogos. En móvil el menú es desplegable y las tablas tienen desplazamiento dentro de su contenedor. La interfaz incluye enlace para saltar al contenido, foco visible, etiquetas y estado del módulo activo.

Los formularios reutilizan `assets/js/ui.js`: `validateForm` para campos obligatorios y restricciones nativas, `showFieldErrors` para mensajes del servidor, `setFeedback` para avisos, `setBusy` para bloquear envíos repetidos y `confirmAction` para confirmar operaciones destructivas. Cada campo tiene etiqueta visible, un `name` coincidente con su mensaje de error y una indicación de obligatoriedad. Las utilidades conservan las ayudas de `aria-describedby`, asocian los errores al campo y enfocan el primer campo inválido. El servidor valida los datos independientemente del navegador.

`assets/js/api.js` exporta `request`, que resuelve las rutas REST con respecto al contexto del WAR, envía y recibe JSON y lanza `ApiRequestError` con `status`, `code`, `message` y `fields`. Los mensajes deben presentarse con `textContent` o las utilidades compartidas. Los endpoints técnicos de prueba y conexión conservan sus respuestas en texto.

Los errores REST comunes usan este formato:

```json
{
  "status": 400,
  "code": "INVALID_DATA",
  "message": "Revisa los campos indicados.",
  "fields": { "nombre": "Completa este campo." }
}
```

| Situación | HTTP | Código |
| --- | --- | --- |
| Datos inválidos o JSON malformado | 400 | `INVALID_DATA` |
| Registro o recurso inexistente | 404 | `NOT_FOUND` |
| Valor único duplicado | 409 | `DUPLICATE` |
| Referencia inválida o registro con información relacionada | 409 | `RELATED_RECORDS` |
| Modificación simultánea | 409 | `CONFLICT` |
| Conexión temporalmente no disponible | 503 | `SERVICE_UNAVAILABLE` |
| Error inesperado | 500 | `INTERNAL_ERROR` |

Los servicios podrán usar `ApplicationException` para errores funcionales; los mensajes por campo deben ser comprensibles y no incluir datos clínicos ni valores privados. `ApiExceptionMapper` también reconoce restricciones PostgreSQL en excepciones anidadas. Las respuestas no incluyen SQL, trazas ni mensajes internos del controlador, y conservan las cabeceras HTTP necesarias, como `Allow` en un error 405. Este contrato cubre la API bajo `/resources`; los errores de arranque y las páginas estáticas de Tomcat se gestionan fuera de Jersey.

## Módulos previstos

| Módulo | Responsabilidad |
| --- | --- |
| Pacientes | Registrar identificación, datos personales y contacto. |
| Especialidades | Mantener las especialidades que se asignan a los médicos. |
| Médicos | Registrar identificación, registro médico, contacto y especialidad. |
| Citas | Asociar paciente y médico con fecha, hora, motivo y estado. |
| Historias clínicas | Registrar apertura, grupo sanguíneo, antecedentes y alergias de cada paciente. |
| Diagnósticos | Registrar código CIE-10, descripción y tipo, asociados a una cita. |
| Tratamientos | Registrar medicamento, dosis, frecuencia, duración e indicaciones de un diagnóstico. |
| Órdenes médicas | Emitir órdenes de laboratorio, imagenología, medicamento o remisión desde una cita. |
| Detalles de orden | Registrar descripción, cantidad e indicaciones de los elementos de una orden. |
| Reportes | Consultar indicadores operativos y clínicos y exportar información; desarrollo pendiente. |

Los requisitos se encuentran en [Estructura del proyecto](docs/EstructuraProyecto.md). El alcance de reportes se describe en [Módulo de reportes](docs/ModuloReportes.md), y su implementación queda para una etapa posterior.

## Especialidades y médicos

Ambos módulos permiten registrar, listar, consultar y editar. Los listados muestran 20 registros por página, con búsqueda y filtros. Las especialidades se filtran por estado; los médicos se buscan por nombre completo, documento o registro médico y se filtran por especialidad.

Una especialidad inactiva conserva sus médicos asociados y puede reactivarse. No se puede asignar a un médico nuevo ni en una reasignación, pero los médicos que ya la tienen pueden conservarla al editar sus demás datos. No hay eliminación física de especialidades o médicos en esta etapa.

Los servicios recortan espacios en los extremos de los textos, validan los campos obligatorios, sus longitudes y el correo, y detectan los datos únicos duplicados. La unicidad conserva la comparación exacta del esquema SQL. Los DTO evitan enviar entidades JPA y relaciones diferidas directamente al navegador.

Rutas relativas a `/resources`:

| Método | Ruta | Operación |
| --- | --- | --- |
| `GET` | `/especialidades` | Listado con `q`, `activo`, `page` y `pageSize`. |
| `POST` | `/especialidades` | Crear con `nombre`, `descripcion` y `activo`. |
| `GET` / `PUT` | `/especialidades/{id}` | Consultar / editar. |
| `GET` | `/medicos` | Listado con `q`, `idEspecialidad`, `page` y `pageSize`. |
| `POST` | `/medicos` | Crear con `documento`, `nombre`, `apellido`, `registroMedico`, `telefono`, `email` e `idEspecialidad`. |
| `GET` / `PUT` | `/medicos/{id}` | Consultar / editar. |

Los listados responden con `{ "items": [], "total": 0, "page": 1, "pageSize": 20 }`. `page` comienza en 1 y `pageSize` admite valores de 1 a 100. Los registros nuevos responden con HTTP 201 y cabecera `Location`; la edición devuelve HTTP 200. El identificador de la URL selecciona el registro que se edita. Si se omite `activo`, una especialidad nueva queda activa y una existente conserva su estado.

## Pacientes, historias clínicas y citas

Pacientes permite registrar, buscar, consultar y editar identificación, nacimiento, sexo, dirección y contacto. El tipo de documento se selecciona entre Cédula de Ciudadanía (`CC`) y Tarjeta de Identidad (`TI`). La fecha de nacimiento no puede ser futura; documento y correo no pueden repetirse en otro paciente. La ficha enlaza con su historia clínica y sus citas.

La historia se abre desde un paciente existente y requiere grupo sanguíneo. Antecedentes y alergias son opcionales. Se permite consultar y editar su contenido, conservando la fecha de apertura y el paciente propietario. Se impide una segunda historia incluso ante solicitudes simultáneas.

La agenda permite crear, consultar y reprogramar citas activas y cambiar sus estados. Se muestran en horario de Bogotá y pueden filtrarse por paciente, médico, estado, fechas y texto. Un médico o paciente no puede tener dos citas activas en el mismo instante; no se calculan intervalos de atención porque el esquema no define duración.

Las citas comienzan como `PROGRAMADA`. Se pueden confirmar, atender, cancelar o marcar como `NO_ASISTIO`; atención e inasistencia requieren que haya llegado su hora. Los estados finales no se reabren. Cambiar la hora o los participantes de una cita activa la devuelve a `PROGRAMADA`. Las reglas completas están en [Requisitos y modelo de datos](docs/EstructuraProyecto.md).

Rutas adicionales relativas a `/resources`:

| Método | Ruta | Operación |
| --- | --- | --- |
| `GET` / `POST` | `/pacientes` | Listar con `q`, `page`, `pageSize` / registrar. |
| `GET` / `PUT` | `/pacientes/{id}` | Consultar / editar datos del paciente. |
| `GET` | `/pacientes/opciones`, `/medicos/opciones` | Hasta 100 coincidencias mínimas por `q`, sin contacto ni datos clínicos. |
| `GET` / `POST` / `PUT` | `/pacientes/{id}/historia` | Consultar / abrir / editar su historia. GET devuelve 204 si aún no se abrió. |
| `GET` / `POST` | `/citas` | Listar / programar con `idPaciente`, `idMedico`, `fechaHora` y `motivo`. |
| `GET` / `PUT` | `/citas/{id}` | Consultar / reprogramar o editar una cita activa. |
| `PATCH` | `/citas/{id}/estado` | Actualizar con `{ "estado": "CONFIRMADA" }`. |

La búsqueda de citas admite `q`, `idPaciente`, `idMedico`, `estado`, `desde`, `hasta`, `page` y `pageSize`. Las fechas de filtro usan `AAAA-MM-DD`; la fecha de nacimiento también. Las horas de citas incluyen desplazamiento: `2026-11-23T10:00:00-05:00`. Las fechas se transmiten como cadenas ISO, sin depender de la zona del equipo cliente. Los selectores muestran como máximo 100 referencias y permiten refinar la búsqueda. La lista de pacientes omite dirección y contacto; la agenda omite los motivos clínicos. Los datos completos se consultan en cada ficha o cita individual.

## Diagnósticos y tratamientos

Desde la consulta de una cita se accede a sus diagnósticos; desde cada diagnóstico, a sus tratamientos. Ambos módulos permiten registrar, consultar y editar, con búsqueda, filtros por referencia y paginación. Las relaciones se conservan al editar y no se ofrece eliminación física. Una cita con diagnósticos u órdenes no permite cambiar paciente ni médico; la reprogramación de una cita activa conserva las demás reglas de agenda.

El diagnóstico requiere `idCita`, `codigoCie10` (hasta 20 caracteres), `descripcion` y `tipo` (`PRESUNTIVO`, `DEFINITIVO`, `PRINCIPAL` o `SECUNDARIO`). El código se introduce manualmente, sin validación contra un catálogo externo. El tratamiento requiere `idDiagnostico`, `medicamento`, `dosis`, `frecuencia` y `duracionDias`, un entero positivo sin redondear fracciones; `indicaciones` es opcional.

Rutas relativas a `/resources`:

| Método | Ruta | Operación |
| --- | --- | --- |
| `GET` / `POST` | `/diagnosticos` | Listar / registrar; filtros `q`, `idCita`, `page`, `pageSize`. |
| `GET` / `PUT` | `/diagnosticos/{id}` | Consultar / editar conservando la cita. |
| `GET` / `POST` | `/tratamientos` | Listar / registrar; filtros `q`, `idDiagnostico`, `page`, `pageSize`. |
| `GET` / `PUT` | `/tratamientos/{id}` | Consultar / editar conservando el diagnóstico. |
| `GET` | `/citas/opciones`, `/diagnosticos/opciones` | Buscar referencias por `q`, con hasta 100 coincidencias. |

## Órdenes médicas

Desde la consulta de una cita, usar **Órdenes de la cita** y **Registrar orden**. Seleccionar tipo, completar observaciones y añadir uno o varios detalles con descripción, cantidad entera positiva e indicaciones opcionales. La orden y todos sus detalles se guardan en una misma transacción; no se admite una orden vacía ni se redondean cantidades fraccionarias. Los textos admiten hasta 10 000 caracteres.

La fecha se asigna automáticamente en Bogotá y el estado inicial es `PENDIENTE`. La consulta muestra todos los detalles y permite marcar la orden como `COMPLETADA` o `CANCELADA`; la cancelación pide confirmación. Los estados finales no se reabren. No se ofrece edición de la orden emitida ni borrado físico: se cancela y se registra otra cuando sea necesario. La cita original se conserva. El listado permite buscar por nombre del paciente, filtrar por cita o estado y paginar de 20 en 20.

| Método | Ruta relativa a `/resources` | Operación |
| --- | --- | --- |
| `GET` / `POST` | `/ordenes` | Listar / emitir con `idCita`, `tipoOrden`, `observaciones` y `detalles`. |
| `GET` | `/ordenes/{id}` | Consultar la orden y sus detalles. |
| `PATCH` | `/ordenes/{id}/estado` | Cambiar con `{ "estado": "COMPLETADA" }` o `CANCELADA`. |

El listado admite `q`, `idCita`, `estado`, `page` y `pageSize`. Cada elemento de `detalles` recibe `descripcion`, `cantidad` e `indicaciones` opcionales. Solo se admiten `LABORATORIO`, `IMAGENOLOGIA`, `MEDICAMENTO` y `REMISION` como tipo.

## Modelo de datos

El archivo [schema.sql](schema.sql) define las tablas y sus restricciones. Las relaciones son:

- Una especialidad puede estar asociada a muchos médicos.
- Un médico y un paciente pueden estar asociados a muchas citas.
- Un paciente puede tener una historia clínica; la clave foránea única impide registrar más de una para el mismo paciente.
- Una cita puede registrar muchos diagnósticos y generar muchas órdenes.
- Un diagnóstico puede tener muchos tratamientos.
- Una orden puede tener muchos detalles. El requisito funcional contempla al menos uno, aunque el esquema no impone ese mínimo.

El esquema incluye claves primarias autogeneradas, claves foráneas, campos únicos y restricciones sobre estados, tipos y cantidades. Las relaciones usan `ON DELETE RESTRICT` y `ON UPDATE CASCADE`.

### Estados y tipos definidos en el SQL

| Campo | Valores permitidos |
| --- | --- |
| Estado de cita | `PROGRAMADA`, `CONFIRMADA`, `ATENDIDA`, `CANCELADA`, `NO_ASISTIO` |
| Tipo de orden | `LABORATORIO`, `IMAGENOLOGIA`, `MEDICAMENTO`, `REMISION` |
| Estado de orden | `PENDIENTE`, `COMPLETADA`, `CANCELADA` |
| Tipo de diagnóstico | `PRESUNTIVO`, `DEFINITIVO`, `PRINCIPAL`, `SECUNDARIO` |

Las citas se crean por defecto como `PROGRAMADA`. Los campos obligatorios y opcionales, fechas, valores predeterminados y reglas de eliminación están unificados en [Requisitos y modelo de datos](docs/EstructuraProyecto.md). Las transiciones entre estados y la disponibilidad se validan en el servicio de citas según las [reglas de agenda](docs/EstructuraProyecto.md#reglas-de-agenda).

### Modelo JPA implementado

Las nueve entidades se registran explícitamente en `persistence.xml`. Sus claves `Integer` se generan con `IDENTITY` y no tienen setters públicos. Se respetan las columnas, longitudes, campos opcionales y restricciones únicas del esquema.

Las relaciones son unidireccionales desde el registro que contiene la clave foránea: siete `ManyToOne` y una `OneToOne` entre historia clínica y paciente. Usan carga `LAZY`, sin `CascadeType.REMOVE`, `CascadeType.ALL` ni eliminación de huérfanos. Las consultas inversas se implementarán en los repositorios.

`EstadoCita`, `EstadoOrden`, `TipoOrden` y `TipoDiagnostico` usan `EnumType.STRING`, con columnas `VARCHAR`. Sexo y grupo sanguíneo conservan sus textos del esquema. Las citas usan `OffsetDateTime` y almacenamiento nativo `TIMESTAMPTZ`, preservando el instante aunque PostgreSQL normalice el desplazamiento. Las fechas sin hora usan `LocalDate`. Al persistir una historia u orden sin fecha, `@PrePersist` la completa con la fecha de `America/Bogota`; las fechas suministradas se conservan.

Hibernate mantiene desactivada la creación y modificación automática de tablas. Los servicios aplican las validaciones funcionales y guardan cada orden con sus detalles en una transacción local.

## Preparación y ejecución

### Requisitos

- JDK 17 o superior; compilación con `release 17`.
- Apache Maven disponible en la terminal, o el Maven integrado de NetBeans.
- Apache Tomcat 11.
- PostgreSQL y su cliente `psql` si se desea crear el esquema de datos.

### Compilar

Desde la raíz del proyecto:

```sh
mvn clean package
```

El artefacto esperado es `target/sistemaWebMedico-1.0-SNAPSHOT.war`. El repositorio no incluye Maven Wrapper. `mvn test` comprueba el arranque del modelo sin conexión; las pruebas de integración PostgreSQL requieren una base de pruebas separada, según [Configuración local](docs/ConfiguracionLocal.md).

### Preparar la base de datos

Crear una base de datos vacía en PostgreSQL y ejecutar el esquema sobre ella, sustituyendo los valores del ejemplo:

```sh
psql -h localhost -U <usuario> -d <base_de_datos> -f schema.sql
```

El script crea las tablas; no crea la base de datos ni inserta datos iniciales. Está pensado para una base vacía y no incluye comprobaciones para volver a ejecutarlo sobre tablas existentes.

Para cargar datos ficticios de todos los módulos, abrir [datos_prueba.sql](datos_prueba.sql) en Query Tool de pgAdmin y ejecutarlo completo sobre la base de pruebas con las tablas ya creadas. Incluye citas pasadas y futuras respecto al día de ejecución, pacientes con y sin historia y órdenes con detalles. No borra datos existentes y rechaza una segunda carga del conjunto DEMO.

La conexión se configura en `config/database.properties`, excluido de Git, a partir de `config/database.properties.example`. La base local prevista es `sistema-web-medico` y el usuario es `postgres`. Completar la contraseña localmente y configurar `-Dmedico.db.config` o `MEDICO_DB_CONFIG` con la ruta absoluta del archivo antes de iniciar Tomcat. Las instrucciones para NetBeans están en [Configuración local](docs/ConfiguracionLocal.md). Hibernate no crea ni modifica las tablas automáticamente.

### Desplegar y comprobar

1. Configurar el servidor de aplicaciones y desplegar el WAR generado. En NetBeans, abrir el proyecto Maven, seleccionar el servidor y ejecutarlo desde el IDE.
2. Comprobar el contexto asignado por el servidor. El archivo `context.xml` declara `/sistemaWebMedico`; la dirección definitiva depende del despliegue.
3. Consultar el endpoint REST de prueba:

```text
GET /<contexto>/resources/jakartaee11
```

Con el contexto declarado y un servidor local en el puerto 8080, la URL sería:

```text
http://localhost:8080/sistemaWebMedico/resources/jakartaee11
```

La respuesta esperada es HTTP `200` con el cuerpo `ping Jakarta EE`. Para verificar PostgreSQL a través de JPA, consultar `GET /<contexto>/resources/health/db`: responde `200` con `database OK`, o `503` si la conexión falla después del arranque. Una configuración ausente o inválida impide el arranque de la aplicación.

## Directrices de desarrollo

Las instrucciones del repositorio están en [AGENTS.md](AGENTS.md). Antes de crear, modificar o revisar interfaces, componentes o estilos, se deben leer y aplicar las [directrices de diseño médico](.agents/skills/medical-ui-design/SKILL.md).
