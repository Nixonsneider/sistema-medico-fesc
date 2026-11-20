# Sistema Web Médico

Proyecto de una plataforma web para gestionar pacientes, médicos, especialidades, citas y la información clínica asociada: historias clínicas, diagnósticos, tratamientos y órdenes médicas.

## Estado actual

El repositorio contiene la estructura inicial de una aplicación Java web con Maven y Jakarta EE, un endpoint REST de prueba, un esquema SQL y la documentación de los módulos previstos.

Los módulos de gestión médica todavía no están implementados en Java. No hay entidades de persistencia, servicios, repositorios ni operaciones CRUD. La persistencia usa Hibernate y un pool HikariCP, con conexión PostgreSQL configurada en un archivo externo.

## Tecnologías y configuración

| Elemento | Configuración del repositorio |
| --- | --- |
| Java | Compilación con nivel de lenguaje y bytecode Java 17 |
| Maven | Gestión de dependencias y construcción del proyecto |
| Jakarta EE API | `11.0.0`, con alcance `provided` |
| Jakarta REST | Jersey 4.0.3 |
| Conexiones | HikariCP 7.0.2 y PostgreSQL JDBC 42.7.14 |
| Jakarta Persistence | Hibernate ORM 7.4.13.Final; transacciones `RESOURCE_LOCAL` |
| CDI | Weld Servlet 6.0.3.Final |
| Empaquetado | WAR: `sistemaWebMedico-1.0-SNAPSHOT.war` |
| Base de datos | Esquema SQL con sintaxis de PostgreSQL |
| IDE | Configuración compartida de Apache NetBeans |

El servidor de referencia es Tomcat 11 con JDK 17 o superior (verificado con JDK 21). El WAR incluye Jersey, Weld, Hibernate, HikariCP y el controlador PostgreSQL. La API Jakarta EE sigue siendo `provided`; Tomcat no ofrece la plataforma completa. Las transacciones JPA serán locales y gestionadas explícitamente por la aplicación.

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
│   │   ├── entity/                  # Entidades JPA (pendientes de etapa 4)
│   │   ├── enums/                   # Estados y tipos (pendientes de etapa 4)
│   │   ├── repository/              # Consultas y acceso a datos
│   │   ├── service/                 # Reglas de negocio y transacciones
│   │   └── resources/
│   │       ├── DatabaseHealthResource.java
│   │       └── JakartaEE11Resource.java
│   ├── resources/META-INF/
│   │   └── persistence.xml           # Unidad JPA con proveedor Hibernate
│   └── webapp/
│       ├── index.html                # Página de entrada
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

Los paquetes de dominio y acceso a datos quedan preparados para las siguientes etapas mediante su documentación, sin clases de negocio vacías. Los endpoints técnicos existentes conservan su comportamiento. Las vistas permanecen en `src/main/webapp`, con HTML, CSS y JavaScript; no se incorpora otro proyecto frontend ni nuevas dependencias en esta etapa.

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

Las citas se crean por defecto como `PROGRAMADA`. Los campos obligatorios y opcionales, fechas, valores predeterminados y reglas de eliminación están unificados en [Requisitos y modelo de datos](docs/EstructuraProyecto.md). Las transiciones entre estados y las reglas de disponibilidad se definirán al implementar citas.

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

El artefacto esperado es `target/sistemaWebMedico-1.0-SNAPSHOT.war`. El repositorio no incluye Maven Wrapper ni pruebas automatizadas.

### Preparar la base de datos

Crear una base de datos vacía en PostgreSQL y ejecutar el esquema sobre ella, sustituyendo los valores del ejemplo:

```sh
psql -h localhost -U <usuario> -d <base_de_datos> -f schema.sql
```

El script crea las tablas; no crea la base de datos ni inserta datos iniciales. Está pensado para una base vacía y no incluye comprobaciones para volver a ejecutarlo sobre tablas existentes.

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
