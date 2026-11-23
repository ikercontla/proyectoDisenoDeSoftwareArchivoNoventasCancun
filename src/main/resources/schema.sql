CREATE TABLE IF NOT EXISTS locaciones (
  id INTEGER PRIMARY KEY,
  nombre TEXT NOT NULL UNIQUE,
  descripcion TEXT NOT NULL,
  latitud REAL NOT NULL CHECK(latitud BETWEEN -90 AND 90),
  longitud REAL NOT NULL CHECK(longitud BETWEEN -180 AND 180),
  activa INTEGER NOT NULL DEFAULT 1 CHECK(activa IN (0, 1))
);
CREATE TABLE IF NOT EXISTS binarios (
  id INTEGER PRIMARY KEY,
  nombre TEXT NOT NULL,
  tipo TEXT NOT NULL,
  contenido BLOB
);
CREATE TABLE IF NOT EXISTS propuestas (
  folio TEXT PRIMARY KEY,
  locacion_id INTEGER NOT NULL REFERENCES locaciones(id),
  binario_id INTEGER NOT NULL REFERENCES binarios(id),
  descripcion TEXT NOT NULL,
  fecha_historica TEXT,
  estado TEXT NOT NULL DEFAULT 'Pendiente'
    CHECK(estado IN ('Pendiente', 'Revisada', 'Aprobada', 'Rechazada')),
  motivo TEXT,
  recibida TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  curador TEXT REFERENCES administradores(usuario),
  dictaminada TEXT
);
CREATE TABLE IF NOT EXISTS documentos (
  id INTEGER PRIMARY KEY,
  locacion_id INTEGER NOT NULL REFERENCES locaciones(id),
  binario_id INTEGER NOT NULL REFERENCES binarios(id),
  propuesta_folio TEXT UNIQUE REFERENCES propuestas(folio),
  titulo TEXT NOT NULL,
  descripcion TEXT NOT NULL,
  fecha_historica TEXT,
  publicado TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS administradores (
  usuario TEXT PRIMARY KEY,
  salt TEXT NOT NULL,
  hash TEXT NOT NULL,
  intentos_fallidos INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS sesiones (
  token_hash TEXT PRIMARY KEY,
  usuario TEXT NOT NULL REFERENCES administradores(usuario),
  vence INTEGER NOT NULL
);
