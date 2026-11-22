-- DATOS FICTICIOS PARA PRUEBAS MANUALES - PostgreSQL
-- Ejecutar después de schema.sql, en la base que usarás para probar el sistema.
-- En pgAdmin: seleccionar la base -> Query Tool -> abrir este archivo -> ejecutar todo.
-- No ejecutar schema.sql otra vez si las tablas ya existen.
--
-- Inserta: 4 especialidades, 4 médicos, 8 pacientes, 6 historias, 12 citas,
-- 4 diagnósticos, 4 tratamientos, 4 órdenes y 8 detalles de orden.
-- Las citas se calculan respecto al día de ejecución, en horario de Bogotá.
-- Hay citas futuras, pasadas y una confirmada hace una hora para probar atención.
-- Dos pacientes quedan sin historia para poder abrirla desde la interfaz.
-- Todos los nombres, documentos y datos clínicos son ficticios.
-- Los códigos DEMO-CIE y medicamentos de prueba NO son referencias clínicas reales.
--
-- No borra ni actualiza registros existentes y no fija IDs ni reinicia secuencias.
-- Si detecta los registros DEMO, se detiene para evitar duplicar el conjunto.
-- Los IDs se generan automáticamente; todas las inserciones son una transacción.
-- Si pgAdmin informa un error, ejecutar ROLLBACK; para cerrar la transacción fallida.

BEGIN;
SET LOCAL TIME ZONE 'America/Bogota';

DO $datos$
DECLARE
    especialidades INTEGER[] := ARRAY[]::INTEGER[];
    medicos INTEGER[] := ARRAY[]::INTEGER[];
    pacientes INTEGER[] := ARRAY[]::INTEGER[];
    citas INTEGER[] := ARRAY[]::INTEGER[];
    diagnosticos INTEGER[] := ARRAY[]::INTEGER[];
    nuevo_id INTEGER;
    orden_id INTEGER;
    i INTEGER;
    instante TIMESTAMPTZ;
    dias INTEGER[] := ARRAY[-7, -6, -5, -4, -3, 0, 1, 1, 2, 2, 3, 4];
    estados TEXT[] := ARRAY[
        'ATENDIDA', 'ATENDIDA', 'ATENDIDA', 'NO_ASISTIO', 'CANCELADA', 'CONFIRMADA',
        'PROGRAMADA', 'CONFIRMADA', 'PROGRAMADA', 'CANCELADA', 'PROGRAMADA', 'CONFIRMADA'
    ];
    nombres_especialidad TEXT[] := ARRAY[
        'DEMO - Medicina general', 'DEMO - Pediatría',
        'DEMO - Dermatología', 'DEMO - Especialidad inactiva'
    ];
    nombres_medico TEXT[] := ARRAY['Ana', 'Carlos', 'Lucía', 'Andrés'];
    nombres_paciente TEXT[] := ARRAY['Laura', 'Miguel', 'Valentina', 'Daniel', 'Sofía', 'Mateo', 'Camila', 'Samuel'];
    grupos TEXT[] := ARRAY['O+', 'A+', 'B+', 'AB+', 'O-', 'A-'];
    tipos_diagnostico TEXT[] := ARRAY['PRESUNTIVO', 'DEFINITIVO', 'PRINCIPAL', 'SECUNDARIO'];
    tipos_orden TEXT[] := ARRAY['LABORATORIO', 'IMAGENOLOGIA', 'MEDICAMENTO', 'REMISION'];
    estados_orden TEXT[] := ARRAY['PENDIENTE', 'COMPLETADA', 'CANCELADA', 'PENDIENTE'];
BEGIN
    IF EXISTS (SELECT 1 FROM paciente WHERE documento LIKE 'DEMO-P%')
       OR EXISTS (SELECT 1 FROM medico WHERE documento LIKE 'DEMO-M%')
       OR EXISTS (SELECT 1 FROM especialidad WHERE nombre LIKE 'DEMO - %') THEN
        RAISE EXCEPTION 'Ya existen datos DEMO. No se insertó nada. Este archivo se ejecuta una sola vez por base.';
    END IF;

    FOR i IN 1..4 LOOP
        INSERT INTO especialidad (nombre, descripcion, activo)
        VALUES (nombres_especialidad[i], 'Especialidad ficticia para pruebas del sistema.', i <> 4)
        RETURNING id_especialidad INTO nuevo_id;
        especialidades := array_append(especialidades, nuevo_id);
    END LOOP;

    FOR i IN 1..4 LOOP
        INSERT INTO medico (documento, nombre, apellido, registro_medico, telefono, email, id_especialidad)
        VALUES (
            'DEMO-M00' || i, nombres_medico[i], 'Médico de Prueba',
            'DEMO-RM-00' || i, '0000000000', 'medico' || i || '@demo.example.test',
            especialidades[((i - 1) % 3) + 1]
        ) RETURNING id_medico INTO nuevo_id;
        medicos := array_append(medicos, nuevo_id);
    END LOOP;

    FOR i IN 1..8 LOOP
        INSERT INTO paciente (tipo_documento, documento, nombre, apellido, fecha_nacimiento, sexo, direccion, telefono, email)
        VALUES (
            CASE WHEN i IN (3, 4) THEN 'TI' ELSE 'CC' END,
            'DEMO-P00' || i, nombres_paciente[i], 'Paciente de Prueba',
            CASE WHEN i IN (3, 4) THEN (CURRENT_DATE - INTERVAL '12 years')::DATE
                 ELSE (CURRENT_DATE - make_interval(years => 25 + i))::DATE END,
            CASE WHEN i % 2 = 1 THEN 'Femenino' ELSE 'Masculino' END,
            'Dirección ficticia de prueba ' || i, '0000000000',
            'paciente' || i || '@demo.example.test'
        ) RETURNING id_paciente INTO nuevo_id;
        pacientes := array_append(pacientes, nuevo_id);

        IF i <= 6 THEN
            INSERT INTO historia_clinica (fecha_apertura, grupo_sanguineo, antecedentes, alergias, id_paciente)
            VALUES (
                CURRENT_DATE - 30, grupos[i],
                CASE WHEN i % 2 = 0 THEN NULL ELSE 'Antecedente ficticio registrado para pruebas.' END,
                CASE WHEN i = 2 THEN 'Alergia ficticia de prueba.' ELSE NULL END,
                nuevo_id
            );
        END IF;
    END LOOP;

    FOR i IN 1..12 LOOP
        -- Instantes distintos: no hay reservas activas simultáneas del mismo recurso.
        instante := (CURRENT_DATE + dias[i] + make_time(8 + (i % 8), 0, 0)) AT TIME ZONE 'America/Bogota';
        IF i = 6 THEN
            instante := date_trunc('minute', CURRENT_TIMESTAMP) - INTERVAL '1 hour';
        END IF;
        INSERT INTO cita (fecha_hora, motivo, estado_cita, id_paciente, id_medico)
        VALUES (
            instante, 'Consulta ficticia DEMO ' || i || '. Solo para pruebas de funcionamiento.',
            estados[i], pacientes[((i - 1) % 8) + 1], medicos[((i - 1) % 4) + 1]
        ) RETURNING id_cita INTO nuevo_id;
        citas := array_append(citas, nuevo_id);
    END LOOP;

    FOR i IN 1..4 LOOP
        INSERT INTO diagnostico (codigo_cie10, descripcion, tipo, id_cita)
        VALUES (
            'DEMO-CIE-0' || i,
            'Diagnóstico ficticio ' || i || '. Texto de prueba sin significado clínico.',
            tipos_diagnostico[i], citas[LEAST(i, 3)]
        ) RETURNING id_diagnostico INTO nuevo_id;
        diagnosticos := array_append(diagnosticos, nuevo_id);
    END LOOP;

    FOR i IN 1..4 LOOP
        INSERT INTO tratamiento (medicamento, dosis, frecuencia, duracion_dias, indicaciones, id_diagnostico)
        VALUES (
            'Medicamento ficticio DEMO ' || i, 'Dosis de prueba', 'Frecuencia de prueba', 3 + i,
            CASE WHEN i = 4 THEN NULL ELSE 'Indicación ficticia. No corresponde a una prescripción real.' END,
            diagnosticos[i]
        );

        INSERT INTO orden (tipo_orden, estado, fecha_orden, observaciones, id_cita)
        VALUES (
            tipos_orden[i], estados_orden[i], CURRENT_DATE,
            'Orden ficticia DEMO ' || i || '. Solo para probar el sistema.', citas[LEAST(i, 3)]
        ) RETURNING id_orden INTO orden_id;

        INSERT INTO detalle_orden (descripcion, cantidad, indicaciones, id_orden)
        VALUES
            ('Detalle ficticio principal DEMO ' || i, 1, 'Indicaciones de prueba; sin uso clínico.', orden_id),
            ('Detalle ficticio adicional DEMO ' || i, 2, NULL, orden_id);
    END LOOP;

    RAISE NOTICE 'Datos DEMO insertados: 4 especialidades, 4 médicos, 8 pacientes, 6 historias, 12 citas, 4 diagnósticos, 4 tratamientos, 4 órdenes y 8 detalles.';
END;
$datos$;

COMMIT;

-- Para encontrar los datos en la aplicación: buscar "Prueba" en médicos o pacientes
-- y "DEMO" en especialidades, diagnósticos o tratamientos.
-- Camila y Samuel quedan sin historia clínica.
-- La sexta cita está confirmada y ya pasó su hora: permite probar atención/inasistencia.
-- Las citas futuras permiten probar confirmación, reprogramación y cancelación.
