import { mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { DatabaseSync } from 'node:sqlite';

export function abrirAlmacen(ruta = process.env.ARCHIVO_DB || '.data/archivo.sqlite') {
  if (ruta !== ':memory:') {
    ruta = resolve(ruta);
    mkdirSync(dirname(ruta), { recursive: true });
  }
  const db = new DatabaseSync(ruta);
  db.exec(`
    PRAGMA busy_timeout = 4000;
    PRAGMA journal_mode = WAL;
    PRAGMA foreign_keys = ON;
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
  `);
  return db;
}

export function transaccion(db, operacion) {
  db.exec('BEGIN IMMEDIATE');
  try {
    const resultado = operacion();
    db.exec('COMMIT');
    return resultado;
  } catch (error) {
    db.exec('ROLLBACK');
    throw error;
  }
}

export function locacionesActivas(db) {
  return db.prepare('SELECT * FROM locaciones WHERE activa = 1 ORDER BY nombre').all();
}

export function locacionActiva(db, id) {
  return db.prepare('SELECT * FROM locaciones WHERE id = ? AND activa = 1').get(id);
}
