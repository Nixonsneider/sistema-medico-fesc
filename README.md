# Sistema Web Médico

Proyecto de una plataforma web para gestionar pacientes, médicos, especialidades, citas y la información clínica asociada: historias clínicas, diagnósticos, tratamientos y órdenes médicas.

## Estado actual

El repositorio contiene la estructura inicial de una aplicación Java web con Maven y Jakarta EE, un endpoint REST de prueba, un esquema SQL y la documentación de los módulos previstos.

Los módulos de gestión médica todavía no están implementados en Java. No hay entidades de persistencia, servicios, repositorios ni operaciones CRUD. La unidad de persistencia está declarada, pero aún no tiene configuración de conexión a la base de datos.

## Tecnologías y configuración

| Elemento | Configuración del repositorio |
| --- | --- |
| Java | Compilación con nivel de lenguaje y bytecode Java 17 |
| Maven | Gestión de dependencias y construcción del proyecto |
| Jakarta EE API | `11.0.0-M1`, con alcance `provided` |
| Jakarta REST | Configuración de rutas y endpoint de prueba |
| Jakarta Persistence | Descriptor `persistence.xml`, versión 3.0 |
| CDI | Descriptor `beans.xml`, con descubrimiento `all` |
| Empaquetado | WAR: `sistemaWebMedico-1.0-SNAPSHOT.war` |
| Base de datos | Esquema SQL con sintaxis de PostgreSQL |
| IDE | Configuración compartida de Apache NetBeans |

La API de Jakarta EE tiene alcance `provided`: sus implementaciones deben estar disponibles en el entorno de ejecución. La configuración de NetBeans señala Tomcat como servidor, pero el proyecto no incluye implementaciones de Jakarta REST, Persistence o CDI. Debe verificarse que el servidor elegido proporcione las capacidades utilizadas o configurar las implementaciones correspondientes antes del despliegue.

## Estructura del proyecto

```text
sistemaWebMedico/
├── .agents/skills/medical-ui-design/  # Directrices de diseño de interfaces
├── docs/
│   ├── EstructuraProyecto.md         # Requisitos funcionales y relaciones
│   └── ModuloReportes.md             # Especificación del módulo de reportes
├── src/main/
│   ├── java/com/nxn/sistemawebmedico/
│   │   ├── JakartaRestConfiguration.java
│   │   └── resources/
│   │       └── JakartaEE11Resource.java
│   ├── resources/META-INF/
│   │   └── persistence.xml           # Unidad de persistencia sin configurar
│   └── webapp/
│       ├── index.html                # Página de entrada
│       ├── META-INF/context.xml      # Contexto /sistemaWebMedico
│       └── WEB-INF/
│           ├── beans.xml             # Configuración de CDI
│           └── web.xml               # Descriptor web; sesión de 30 minutos
├── AGENTS.md                         # Instrucciones de trabajo del proyecto
├── nb-configuration.xml              # Configuración compartida de NetBeans
├── pom.xml                           # Dependencias y plugins de Maven
└── schema.sql                        # Creación de las nueve tablas
```

Maven genera los archivos compilados y el WAR en `target/`.

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
| Estado de cita | `PENDIENTE`, `CONFIRMADA`, `CANCELADA`, `COMPLETADA` |
| Tipo de orden | `LABORATORIO`, `IMAGENOLOGIA`, `MEDICAMENTO`, `REMISION` |
| Estado de orden | `PENDIENTE`, `COMPLETADA`, `CANCELADA` |
| Tipo de diagnóstico | `PRESUNTIVO`, `DEFINITIVO`, `PRINCIPAL`, `SECUNDARIO` |

Los requisitos de citas describen otros estados: `PROGRAMADA`, `CONFIRMADA`, `ATENDIDA`, `CANCELADA` y `NO_ASISTIO`. Esta diferencia entre documentación y SQL está pendiente de unificación antes de implementar el módulo.

## Preparación y ejecución

### Requisitos

- JDK compatible con la API de Jakarta EE utilizada y con la compilación configurada para Java 17.
- Apache Maven disponible en la terminal, o el Maven integrado de NetBeans.
- Un servidor con las implementaciones de Jakarta necesarias para ejecutar la aplicación.
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

Para conectar la aplicación todavía se requiere configurar el controlador JDBC, el origen de datos y la unidad `my_persistence_unit` en `src/main/resources/META-INF/persistence.xml`. El repositorio no define credenciales ni variables de entorno para esa conexión.

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

La respuesta esperada es HTTP `200` con el cuerpo `ping Jakarta EE`. Este endpoint permite comprobar la ejecución de Jakarta REST; no consulta la base de datos.

## Directrices de desarrollo

Las instrucciones del repositorio están en [AGENTS.md](AGENTS.md). Antes de crear, modificar o revisar interfaces, componentes o estilos, se deben leer y aplicar las [directrices de diseño médico](.agents/skills/medical-ui-design/SKILL.md).
