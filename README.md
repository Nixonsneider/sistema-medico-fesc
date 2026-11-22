# Sistema Web Médico — Versión 1.0

Aplicación web para gestionar pacientes, médicos, especialidades, citas, historias clínicas, diagnósticos, tratamientos, órdenes médicas y reportes.

## Estado actual

La **versión funcional 1.0** incluye todos los módulos de las etapas 1 a 12 de [TASKS.md](TASKS.md). Se completaron la implementación y las comprobaciones técnicas breves; la revisión manual final con el usuario sigue pendiente. Esto no sustituye una validación exhaustiva para un despliegue de producción.

El modelo de persistencia incluye nueve entidades JPA y cuatro enumeraciones. La aplicación usa PostgreSQL, Hibernate y un pool HikariCP, con credenciales en un archivo externo. Los reportes están implementados con el [alcance acordado](docs/ModuloReportes.md), sobre las tablas existentes.

La denominación de esta entrega es **1.0**; el `pom.xml` conserva por ahora la versión técnica `1.0-SNAPSHOT`, por lo que el archivo generado sigue llamándose `sistemaWebMedico-1.0-SNAPSHOT.war`.

### Qué incluye y qué queda fuera

- Especialidades y médicos: registro, consulta, edición y activación/desactivación de especialidades.
- Pacientes: registro y edición con selector de documento CC/TI, búsqueda y datos de contacto.
- Historia clínica única por paciente, con grupo sanguíneo, antecedentes y alergias.
- Citas: programación, filtros, reprogramación y cambios de estado.
- Diagnósticos y tratamientos vinculados a la cita y al diagnóstico correspondiente.
- Órdenes con uno o varios detalles, consulta y estados pendiente, completada o cancelada.
- Nueve reportes con filtros, indicadores, gráfico, paginación y exportación CSV, Excel y PDF.

Usuarios, roles y autenticación no están implementados: el sistema no solicita inicio de sesión. Las exportaciones registran `SIN_AUTENTICACION` mientras no exista un usuario autenticado. No se calculan pacientes nuevos, zonas ni vencimientos de órdenes porque esos campos no existen. Los códigos CIE-10 se introducen manualmente, sin catálogo externo.

La guía de arranque está en [Preparación y ejecución](#preparación-y-ejecución). Para detalles del entorno Windows, consultar [Configuración local](docs/ConfiguracionLocal.md).

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

El servidor de referencia es Tomcat 11 con JDK 17 o superior (verificado con JDK 21 y PostgreSQL 18). El WAR incluye Jersey, Weld, Hibernate, HikariCP y el controlador PostgreSQL. La API Servlet es `provided`; las APIs REST, Persistence y CDI llegan con sus implementaciones. Tomcat no ofrece la plataforma Jakarta EE completa. Las transacciones JPA son locales y gestionadas explícitamente por la aplicación.

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
│   │       ├── ReportResource.java
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
├── datos_prueba.sql                  # Datos ficticios opcionales de todos los módulos
└── schema.sql                        # Creación de las nueve tablas
```

Maven genera los archivos compilados y el WAR en `target/`.

### Organización del código

Se mantiene una sola aplicación Maven con un único `pom.xml` y un WAR. Los paquetes Java están dentro de `com.nxn.sistemawebmedico`; cada paquete tiene un `package-info.java` con su responsabilidad.

El flujo habitual de los módulos es `resources → service → repository`, utilizando las entidades de `entity` y los tipos de `enums`. Los servicios delimitan las transacciones locales y los repositorios consultan con `EntityManager`. Reportes realiza consultas SQL de lectura parametrizadas desde su servicio. `config` mantiene el ciclo de vida de la fábrica JPA y del pool. `JakartaRestConfiguration` conserva la configuración REST en el paquete raíz y la ruta `/resources`.

El modelo JPA está implementado. Los repositorios y servicios de especialidades y médicos realizan las consultas y transacciones; el paquete `dto` separa los datos de entrada y salida de las entidades persistentes. El paquete `error` define el contrato público de errores y su traducción HTTP. Los endpoints técnicos existentes conservan su comportamiento. Las vistas permanecen en `src/main/webapp`, con HTML, CSS y JavaScript; no se incorpora otro proyecto frontend ni nuevas dependencias en esta etapa.

## Interfaz y errores comunes

La página inicial ofrece navegación lateral, un listado de módulos con búsqueda por nombre o descripción y vistas que indican su disponibilidad. La búsqueda del listado inicial admite texto sin tildes. Especialidades, médicos, pacientes, historias clínicas, citas, diagnósticos, tratamientos y órdenes están disponibles con datos de la base configurada. Reportes está disponible con filtros, indicadores y exportación CSV, Excel y PDF.

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

## Módulos disponibles en la versión 1.0

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
| Reportes | Consultar indicadores operativos y clínicos y exportar CSV, Excel y PDF. |

Los requisitos se encuentran en [Estructura del proyecto](docs/EstructuraProyecto.md). El alcance de reportes se describe en [Módulo de reportes](docs/ModuloReportes.md), implementado sobre las nueve tablas actuales.

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

## Reportes

Desde el menú Reportes, elegir reporte y filtros y pulsar **Generar**. Se incluyen agenda, productividad, demanda, diagnósticos frecuentes, demografía, órdenes, cumplimiento, servicios y resumen clínico individual. La pantalla muestra indicadores, gráfico por estado y tabla paginada. Buscar primero las opciones de médico, especialidad o paciente si se desea filtrar por ellas.

Se puede exportar la selección completa como CSV, Excel o PDF (hasta 10000 filas). Las fechas se aplican a las citas en Bogotá. El resumen clínico requiere paciente. Las exportaciones generan eventos en el logger `medico.auditoria.exportaciones` de Tomcat, con actor SIN_AUTENTICACION mientras no haya usuarios. No hace falta modificar el esquema. Fórmulas, filtros y limitaciones: [Módulo de reportes](docs/ModuloReportes.md).

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

Las relaciones son unidireccionales desde el registro que contiene la clave foránea: siete `ManyToOne` y una `OneToOne` entre historia clínica y paciente. Usan carga `LAZY`, sin `CascadeType.REMOVE`, `CascadeType.ALL` ni eliminación de huérfanos. Las consultas inversas se realizan explícitamente en los repositorios o servicios de lectura.

`EstadoCita`, `EstadoOrden`, `TipoOrden` y `TipoDiagnostico` usan `EnumType.STRING`, con columnas `VARCHAR`. Sexo y grupo sanguíneo conservan sus textos del esquema. Las citas usan `OffsetDateTime` y almacenamiento nativo `TIMESTAMPTZ`, preservando el instante aunque PostgreSQL normalice el desplazamiento. Las fechas sin hora usan `LocalDate`. Al persistir una historia u orden sin fecha, `@PrePersist` la completa con la fecha de `America/Bogota`; las fechas suministradas se conservan.

Hibernate mantiene desactivada la creación y modificación automática de tablas. Los servicios aplican las validaciones funcionales y guardan cada orden con sus detalles en una transacción local.

## Preparación y ejecución

### Requisitos

- JDK 17 o superior; compilación con `release 17`.
- Apache Maven disponible en la terminal, o el Maven integrado de NetBeans.
- Apache Tomcat 11.
- PostgreSQL y su cliente `psql` si se desea crear el esquema de datos.

### 1. Preparar PostgreSQL

Iniciar PostgreSQL y crear una base vacía llamada `sistema-web-medico`, por ejemplo desde pgAdmin. El usuario local previsto es `postgres`.

En **Query Tool** de esa base, abrir [schema.sql](schema.sql) y ejecutarlo completo **solo si todavía no existen las tablas**. El script crea las nueve tablas; no crea la base de datos ni migra esquemas existentes. Hibernate tampoco crea ni modifica tablas automáticamente.

Como alternativa, desde una terminal con `psql` disponible:

```sh
psql -h localhost -U postgres -d sistema-web-medico -v ON_ERROR_STOP=1 -1 -f schema.sql
```

Para disponer de datos ficticios, ejecutar después [datos_prueba.sql](datos_prueba.sql) completo en Query Tool. Incluye todos los módulos, citas pasadas y futuras respecto al día de carga, pacientes con y sin historia y órdenes con detalles. Es opcional: la aplicación también permite comenzar con las tablas vacías. No borra datos existentes y rechaza una segunda carga del conjunto DEMO. Si una ejecución falla y pgAdmin deja una transacción abortada, ejecutar `ROLLBACK;` antes de continuar.

### 2. Configurar la conexión local

Copiar `config/database.properties.example` como `config/database.properties` **si este último aún no existe**. Completar localmente:

```properties
db.url=jdbc:postgresql://localhost:5432/sistema-web-medico
db.user=postgres
db.password=TU_CONTRASENA_LOCAL
```

Ajustar host, puerto, base y usuario si el entorno es diferente. Este archivo está excluido de Git y no se incorpora al WAR. No publicar la contraseña. Su formato es `java.util.Properties`: las barras invertidas se escriben como `\\` y los caracteres no ASCII pueden expresarse con escapes `\uXXXX`.

### 3. Configurar Tomcat en NetBeans

1. Abrir la carpeta del proyecto como proyecto Maven y permitir la descarga de dependencias en la primera construcción.
2. En **Servicios → Servidores**, registrar Apache Tomcat 11 si todavía no está registrado. Seleccionar el servidor en las propiedades de ejecución del proyecto.
3. Abrir las propiedades del servidor y entrar en **Platform / Plataforma → VM Options / Opciones de máquina virtual**. Añadir la propiedad siguiente, conservando cualquier otra opción existente y ajustando la ruta al equipo:

```text
-Dmedico.db.config="D:/Documents/NetBeansProjects/sistemaWebMedico/config/database.properties"
```

La ruta apunta al archivo de conexión, no a una carpeta. Como alternativa se puede definir `MEDICO_DB_CONFIG` con la ruta absoluta antes de iniciar NetBeans/Tomcat. La propiedad Java tiene prioridad. Detener y volver a iniciar Tomcat después de cambiar esta configuración.

### 4. Construir y ejecutar

En NetBeans: **clic derecho en el proyecto → Clean and Build / Limpiar y construir**, y después **Run / Ejecutar**. Construir genera el WAR; ejecutar además despliega la aplicación y permite abrirla. Evitar iniciar simultáneamente otra instancia de Tomcat con los mismos puertos.

Para compilar desde terminal, en la raíz del proyecto:

```sh
mvn clean package
```

En este equipo, también se puede utilizar Maven de NetBeans desde PowerShell:

```powershell
& 'C:/Program Files/Apache NetBeans/java/maven/bin/mvn.cmd' clean package
```

La primera compilación necesita acceso a los repositorios de dependencias. El resultado es `target/sistemaWebMedico-1.0-SNAPSHOT.war`. No se necesita instalar un proyecto frontend, ejecutar npm ni copiar manualmente el controlador JDBC a Tomcat.

Con el contexto `/sistemaWebMedico` y puerto 8080, abrir:

```text
http://localhost:8080/sistemaWebMedico/
```

El contexto y puerto efectivos dependen del despliegue. Usar la dirección que muestre NetBeans si es diferente.

**También se puede ejecutar sin NetBeans:** configurar `MEDICO_DB_CONFIG` en el entorno que inicia Tomcat (o la propiedad Java equivalente), copiar el WAR en `webapps` de Tomcat 11 y arrancar el servidor. Si se conserva el nombre `sistemaWebMedico-1.0-SNAPSHOT.war`, el contexto habitual será `/sistemaWebMedico-1.0-SNAPSHOT`; para usar el contexto de los ejemplos, desplegarlo como `sistemaWebMedico.war`. Configurar la conexión antes del arranque y usar una sola instancia para ese despliegue.

### 5. Comprobar el arranque

Con la aplicación ejecutada:

| Dirección, con puerto 8080 y contexto `/sistemaWebMedico` | Resultado esperado |
| --- | --- |
| `/sistemaWebMedico/` | Página inicial con los módulos disponibles. |
| `/sistemaWebMedico/resources/jakartaee11` | HTTP 200: `ping Jakarta EE`. |
| `/sistemaWebMedico/resources/health/db` | HTTP 200: `database OK`. |

El endpoint de base de datos comprueba la conexión mediante `SELECT 1`; no verifica que existan todas las tablas. Si faltan credenciales o no se inicializa la persistencia, la aplicación puede no arrancar. Si la conexión falla después del arranque, el endpoint responde 503.

### 6. Recorrido de uso

1. Registrar una especialidad activa y un médico asociado.
2. Registrar un paciente con CC o TI; consultar su ficha y abrir su única historia clínica.
3. Desde el paciente, programar una cita futura y seleccionar un médico existente.
4. Consultar la cita para confirmar, cancelar, atender o registrar inasistencia. Atención e inasistencia requieren que haya llegado la hora.
5. Desde **Diagnósticos de la cita**, registrar el diagnóstico y acceder a sus **Tratamientos**.
6. Desde **Órdenes de la cita**, registrar tipo, observaciones y uno o más detalles; consultar y completar o cancelar la orden.
7. Entrar en **Reportes**, elegir el reporte, buscar las referencias si se quieren aplicar filtros y pulsar **Generar**. El resumen clínico exige paciente; las exportaciones incluyen toda la selección, hasta 10000 filas.

Con los datos DEMO se puede explorar este recorrido sin registrar todo desde cero. Sus medicamentos y códigos son ficticios y no corresponden a prescripciones reales.

### Problemas habituales

| Situación | Qué revisar |
| --- | --- |
| `BUILD SUCCESS` pero la página no abre | La construcción terminó; comprobar que también se ejecutó **Run** y que Tomcat desplegó el proyecto. |
| HTTP 404 | Verificar puerto, contexto y que la aplicación esté desplegada y arrancada. La URL de conexión incluye el contexto antes de `/resources/health/db`. |
| «El contexto no pudo arrancar» | Revisar los logs de Tomcat: archivo de conexión, ruta de VM Options, credenciales, PostgreSQL iniciado y compatibilidad con Tomcat 11. |
| Paquetes `jakarta` no encontrados en NetBeans | Recargar el proyecto Maven, comprobar la descarga de dependencias y el JDK configurado y ejecutar Clean and Build. No cambiar los imports a `javax` ni añadir JAR manualmente. |
| Puerto ocupado | Detener la otra instancia de Tomcat o usar puertos distintos; no iniciar a la vez el mismo servidor desde fuera y desde NetBeans. |
| `database OK` pero errores al abrir módulos | Comprobar que las nueve tablas de `schema.sql` existen en la base configurada. |
| Reporte vacío | Revisar fechas de las citas en Bogotá, estados y referencias seleccionadas. Cambiar filtros y volver a pulsar Generar. |

### Comprobaciones y revisión pendiente

`mvn test` ejecuta las pruebas sin conexión y omite las pruebas PostgreSQL cuando no se proporciona `medico.test.jdbcUrl`. Las pruebas de integración requieren una base **separada**, con las tablas creadas; no usar la base de trabajo. Las instrucciones están en [Configuración local](docs/ConfiguracionLocal.md).

Se verificaron compilación, persistencia y reglas principales en etapas anteriores. La última comprobación breve incluyó los nueve reportes con filtros y generación de archivos CSV/XLSX/PDF. La revisión manual final completa de la versión 1.0 sigue pendiente con el usuario.

## Directrices de desarrollo

Las instrucciones del repositorio están en [AGENTS.md](AGENTS.md). Antes de crear, modificar o revisar interfaces, componentes o estilos, se deben leer y aplicar las [directrices de diseño médico](.agents/skills/medical-ui-design/SKILL.md).
