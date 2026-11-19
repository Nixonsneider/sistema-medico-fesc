-- CREACIÓN DE LA ENTIDAD ESPECIALIDAD

CREATE TABLE especialidad (
    id_especialidad INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- CREACIÓN DE LA ENTIDAD MEDICO

CREATE TABLE medico (
    id_medico INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    documento VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    registro_medico VARCHAR(50) NOT NULL UNIQUE,
    telefono VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    id_especialidad INT NOT NULL,
    CONSTRAINT fk_medico_especialidad 
        FOREIGN KEY (id_especialidad) 
        REFERENCES especialidad(id_especialidad) 
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- CREACIÓN DE LA ENTIDAD PACIENTE

CREATE TABLE paciente (
    tipo_documento VARCHAR(20) NOT NULL DEFAULT '',
    id_paciente INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    documento VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    sexo VARCHAR(20) NOT NULL CHECK (sexo IN ('Masculino', 'Femenino', 'Otro')),
    direccion VARCHAR(150) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE
);

-- CREACIÓN DE LA ENTIDAD CITA

CREATE TABLE cita (
    id_cita INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fecha_hora TIMESTAMPTZ NOT NULL,
    motivo TEXT NOT NULL,
    estado_cita VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE' CHECK(estado_cita IN('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'COMPLETADA')),
    id_paciente INT NOT NULL,
    id_medico INT NOT NULL,
    CONSTRAINT fk_cita_paciente
        FOREIGN KEY (id_paciente)
        REFERENCES paciente(id_paciente)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_cita_medico
        FOREIGN KEY (id_medico)
        REFERENCES medico(id_medico)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- CREACIÓN DE LA ENTIDAD HISTORIA_CLINICA

CREATE TABLE historia_clinica (
    id_historia INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fecha_apertura DATE NOT NULL DEFAULT CURRENT_DATE,
    grupo_sanguineo VARCHAR(5) NOT NULL 
        CHECK (grupo_sanguineo IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-')),
    antecedentes TEXT,
    alergias TEXT,
    id_paciente INT NOT NULL UNIQUE,
    CONSTRAINT fk_historia_paciente
        FOREIGN KEY (id_paciente)
        REFERENCES paciente(id_paciente)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- CREACIÓN DE LA ENTIDAD ORDEN

CREATE TABLE orden (
    id_orden INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo_orden VARCHAR(20) NOT NULL CHECK (tipo_orden IN('LABORATORIO', 'IMAGENOLOGIA', 'MEDICAMENTO', 'REMISION')),
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'COMPLETADA', 'CANCELADA')),
    fecha_orden DATE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observaciones TEXT NOT NULL,
    id_cita INT NOT NULL,
    CONSTRAINT fk_orden_cita
        FOREIGN KEY (id_cita)
        REFERENCES cita(id_cita)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- CREACIÓN DE LA ENTIDAD DIAGNOSTICO

CREATE TABLE diagnostico (
    id_diagnostico INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_cie10 VARCHAR(20) NOT NULL,
    descripcion TEXT NOT NULL,
    tipo VARCHAR(30) NOT NULL DEFAULT 'PRESUNTIVO'
        CHECK (tipo IN ('PRESUNTIVO', 'DEFINITIVO', 'PRINCIPAL', 'SECUNDARIO')),
    id_cita INT NOT NULL,
    CONSTRAINT fk_diagnostico_cita
        FOREIGN KEY (id_cita)
        REFERENCES cita(id_cita)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- CREACIÓN DE LA ENTIDAD TRATAMIENTO

CREATE TABLE tratamiento (
    id_tratamiento INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    medicamento VARCHAR(150) NOT NULL,
    dosis VARCHAR(100) NOT NULL,
    frecuencia VARCHAR(100) NOT NULL,
    duracion_dias INT NOT NULL CHECK (duracion_dias > 0), 
    indicaciones TEXT,
    id_diagnostico INT NOT NULL,
    CONSTRAINT fk_tratamiento_diagnostico
        FOREIGN KEY (id_diagnostico)
        REFERENCES diagnostico(id_diagnostico)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- CREACIÓN DE LA ENTIDAD DETALLE_ORDEN

CREATE TABLE detalle_orden (
    id_detalle INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    descripcion TEXT NOT NULL,
    cantidad INT NOT NULL DEFAULT 1 CHECK (cantidad > 0),
    indicaciones TEXT, 
    id_orden INT NOT NULL,
    CONSTRAINT fk_detalle_orden
        FOREIGN KEY (id_orden)
        REFERENCES orden(id_orden)
        ON DELETE RESTRICT ON UPDATE CASCADE
);