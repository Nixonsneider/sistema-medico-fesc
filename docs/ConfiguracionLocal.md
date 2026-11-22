# Configuración local

## Entorno

- Java 17 o superior (compilación con `release 17`; comprobado con JDK 21.0.7).
- Maven (en este equipo, el incluido en Apache NetBeans).
- Tomcat 11 (instalación local detectada: `D:\Documents\apache-tomcat-11.0.29`).
- PostgreSQL (en este equipo, PostgreSQL 18).

El WAR incluye Jersey para REST, Weld para CDI, Hibernate para JPA, HikariCP para el pool y el controlador PostgreSQL. Las APIs necesarias llegan con esas dependencias; la API Servlet 6.1 es `provided`, suministrada por Tomcat. No se necesita copiar controladores a Tomcat. No se utilizan EJB, JTA ni inyección automática mediante `@PersistenceContext`; las futuras operaciones usarán transacciones `RESOURCE_LOCAL`.

## Archivo de conexión

1. Copiar `config/database.properties.example` como `config/database.properties` si aún no existe.
2. Mantener la URL `jdbc:postgresql://localhost:5432/sistema-web-medico` y el usuario `postgres`, y sustituir `CAMBIAR_LOCALMENTE` por la contraseña real.
3. Mantener este archivo fuera de Git; `.gitignore` ya lo excluye. Tampoco se empaqueta dentro del WAR.

No publicar la contraseña ni pasarla como argumento de ejecución. El formato es el de `java.util.Properties`; las barras invertidas se escriben como `\\` y los caracteres no ASCII pueden expresarse como `\uXXXX`.

## NetBeans y Tomcat

En las opciones de Java del servidor Tomcat registrado en NetBeans, añadir:

```text
-Dmedico.db.config="D:/Documents/NetBeansProjects/sistemaWebMedico/config/database.properties"
```

Como alternativa, definir la variable de entorno `MEDICO_DB_CONFIG` con esa ruta antes de iniciar NetBeans o Tomcat. La propiedad de Java tiene prioridad sobre la variable de entorno. Reiniciar el servidor después de cambiar la configuración.

La aplicación abre un pool de hasta cinco conexiones y una fábrica JPA al iniciar. Configura las sesiones PostgreSQL con `America/Bogota` y cierra los recursos al detenerse. Si faltan datos de conexión o no se puede inicializar la persistencia, el despliegue falla con un mensaje genérico.

## Tablas

La base `sistema-web-medico` ya fue creada por el usuario en pgAdmin. Ejecutar `schema.sql` únicamente si esa base todavía no tiene las tablas. Desde PowerShell y la raíz del proyecto:

```powershell
& 'C:/Program Files/PostgreSQL/18/bin/psql.exe' -h localhost -U postgres -d sistema-web-medico -v ON_ERROR_STOP=1 -1 -f schema.sql
```

`psql` solicitará la contraseña. `-1` ejecuta el script en una transacción. El esquema no migra tablas existentes. Hibernate tiene desactivada la generación automática de tablas.

## Compilación y comprobación

Desde NetBeans, construir el proyecto; o desde PowerShell:

```powershell
& 'C:/Program Files/Apache NetBeans/java/maven/bin/mvn.cmd' package
```

Desplegar `target/sistemaWebMedico-1.0-SNAPSHOT.war`, usando el contexto `/sistemaWebMedico`. Comprobar, sustituyendo el puerto si fuera necesario:

```text
GET http://localhost:8080/sistemaWebMedico/resources/jakartaee11
200: ping Jakarta EE

GET http://localhost:8080/sistemaWebMedico/resources/health/db
200: database OK
```

El segundo endpoint ejecuta `SELECT 1` mediante un `EntityManager`; no lee datos clínicos ni comprueba la existencia de las nueve tablas. Si la conexión deja de estar disponible tras el arranque, devuelve `503` sin detalles internos.

## Pruebas del modelo JPA

`mvn test` arranca la unidad de persistencia sin conexión para comprobar el reconocimiento de las nueve entidades. Las pruebas PostgreSQL quedan omitidas si no se indica `medico.test.jdbcUrl`.

Para ejecutarlas, crear una base vacía dedicada a pruebas, cargar `schema.sql` y usar una URL que apunte únicamente a esa base. No usar la base de trabajo: las pruebas insertan datos ficticios y revierten las transacciones, pero consumen valores de las secuencias.

Ejemplo desde PowerShell, para una instancia de pruebas ya iniciada en el puerto 55433:

```powershell
# Si la instancia exige contraseña, definir MEDICO_TEST_DB_PASSWORD localmente.
& 'C:/Program Files/Apache NetBeans/java/maven/bin/mvn.cmd' '-Dmedico.test.jdbcUrl=jdbc:postgresql://127.0.0.1:55433/medico_pruebas' '-Dmedico.test.user=postgres' test
```

Las pruebas validan el esquema existente con Hibernate y comprueban el recorrido por las nueve entidades, los estados almacenados como texto, las fechas, la restricción de una historia por paciente y la protección frente al borrado de una cita con información clínica asociada. No crean ni migran tablas.

Para una comprobación breve del recorrido de servicios completo, añadir `-Dtest=ClinicalRecordsServiceTest` al comando anterior. Esa prueba recorre especialidad → médico → paciente → historia → cita → diagnóstico → tratamiento y orden con detalles, comprueba validaciones y estados, y elimina únicamente sus datos ficticios al terminar.


## Verificar pacientes, historias y citas

Tras reconstruir y ejecutar el proyecto en NetBeans:

1. En Pacientes, registrar un paciente y abrir su ficha.
2. Usar Historia clínica para abrir su única historia o consultar la existente. La fecha se asigna al guardar.
3. Desde la ficha, usar Citas del paciente y Programar cita. Buscar y seleccionar un médico, fecha/hora futura de Bogotá y motivo.
4. Consultar la cita para confirmar, cancelar o registrar atención/inasistencia. Las dos últimas acciones requieren que haya llegado la hora de atención. Las citas finalizadas no se reabren.
5. Para reprogramar una cita activa, volver al listado y usar Reprogramar. Un cambio de hora o participantes devuelve el estado a Programada.

Los filtros de fecha incluyen días completos en Bogotá. No se calcula duración ni se bloquean intervalos entre horas distintas. Las pruebas de ClinicalServiceTest se ejecutan con la misma propiedad medico.test.jdbcUrl de la base independiente de pruebas e incluyen dos reservas simultáneas del mismo médico.

## Revisión manual posterior del recorrido completo

La implementación está terminada con una comprobación técnica breve. La revisión manual final se realizará cuando el usuario lo indique; estos pasos quedan preparados para ese momento:

1. Reconstruir y ejecutar el proyecto en NetBeans con Tomcat y su archivo de conexión configurados. Comprobar `database OK`.
2. Crear una especialidad y un médico asociado. Registrar un paciente seleccionando Cédula de Ciudadanía o Tarjeta de Identidad y abrir su historia clínica.
3. Desde el paciente, programar y consultar una cita. Usar **Diagnósticos de la cita**, registrar un diagnóstico y abrir **Tratamientos** para registrar su tratamiento.
4. Volver a consultar la cita y usar **Órdenes de la cita**. Registrar una orden con observaciones y uno o varios detalles. Comprobar que aparecen juntos en la consulta.
5. Volver al listado, probar los filtros y consultar la orden. Marcarla como completada o cancelar una orden pendiente y comprobar el estado.

No hace falta volver a ejecutar `schema.sql` para estas etapas: utilizan las nueve tablas existentes. Reportes se consulta desde el menú: elegir filtros y pulsar Generar.
