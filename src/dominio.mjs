import { randomUUID } from 'node:crypto';
import { extname, parse, win32 } from 'node:path';
import { locacionActiva, transaccion } from './almacen.mjs';

export const LIMITE_ARCHIVO = 25_000_000;
export const ARCHIVO_INVALIDO = 'Debe adjuntar un archivo válido (.jpg, .png, .webp, .pdf de hasta 25 MB)';
export const MOTIVO_REQUERIDO = 'Debe ingresar obligatoriamente una justificación para rechazar la propuesta';
export const DESCARGA_NO_DISPONIBLE = 'El documento solicitado no está disponible temporalmente para descarga';

export class ErrorAplicacion extends Error {
  constructor(estado, mensaje) {
    super(mensaje);
    this.estado = estado;
  }
}

const formatos = {
  '.jpg': ['image/jpeg', (datos) => datos.subarray(0, 3).equals(Buffer.from([255, 216, 255]))],
  '.png': ['image/png', (datos) => datos.subarray(0, 8).equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]))],
  '.webp': ['image/webp', (datos) => datos.toString('ascii', 0, 4) === 'RIFF' && datos.toString('ascii', 8, 12) === 'WEBP'],
  '.pdf': ['application/pdf', (datos) => datos.toString('ascii', 0, 5) === '%PDF-'],
};

function normalizarFecha(entrada) {
  const valor = String(entrada ?? '').trim();
  if (!valor) return null;
  const coincidencia = /^(\d{4})(?:-(\d{2})(?:-(\d{2}))?)?$/.exec(valor);
  if (coincidencia) {
    const [, anio, mes = '01', dia = '01'] = coincidencia;
    const fecha = new Date(`${anio}-${mes}-${dia}T00:00:00Z`);
    if (anio !== '0000' && Number.isFinite(fecha.getTime()) &&
        fecha.toISOString().slice(0, 10) === `${anio}-${mes}-${dia}`) return valor;
  }
  throw new ErrorAplicacion(422, 'Ingresa una fecha histórica válida en formato AAAA, AAAA-MM o AAAA-MM-DD.');
}

export class Documento {
  constructor({ id, locacion_id, binario_id, titulo, descripcion, fecha_historica, tipo, nombre }) {
    this.id = id;
    this.locacionId = locacion_id;
    this.binarioId = binario_id;
    this.titulo = titulo;
    this.descripcion = descripcion;
    this.fechaHistorica = fecha_historica;
    this.tipo = tipo;
    this.nombreOriginal = nombre;
  }

  validarPublicacion() {
    if (this.titulo && (this.tipo === 'application/pdf' || (this.fechaHistorica && this.descripcion))) return;
    throw new ErrorAplicacion(422, 'Completa el título y, para una imagen, la fecha histórica y la descripción.');
  }

  referenciaDescarga() {
    return `/documentos/${this.id}/original`;
  }

  descargar(db) {
    const original = db.prepare('SELECT contenido FROM binarios WHERE id = ?').get(this.binarioId);
    if (!original?.contenido) return null;
    return {
      nombre: this.nombreOriginal,
      tipo: this.tipo,
      contenido: original.contenido,
    };
  }
}

export class Archivo {
  constructor(db, locacion) {
    this.db = db;
    this.locacion = locacion;
  }

  documentos() {
    return this.db.prepare(`
      SELECT d.*, b.tipo, b.nombre FROM documentos d
      JOIN binarios b ON b.id = d.binario_id WHERE d.locacion_id = ?
      ORDER BY d.publicado DESC, d.id DESC
    `).all(this.locacion.id).map((fila) => new Documento(fila));
  }

  incorporar(documento, folio) {
    documento.validarPublicacion();
    return this.db.prepare(`
      INSERT INTO documentos (locacion_id, binario_id, propuesta_folio, titulo, descripcion, fecha_historica)
      VALUES (?, ?, ?, ?, ?, ?)
    `).run(this.locacion.id, documento.binarioId, folio, documento.titulo,
      documento.descripcion, documento.fechaHistorica).lastInsertRowid;
  }
}

export class PropuestaAporte {
  constructor(db, fila) {
    this.db = db;
    this.fila = fila;
  }

  static async recibir(db, formulario) {
    const original = formulario.get('archivo');
    const nombre = original instanceof File ? win32.basename(original.name).replace(/[\x00-\x1f\x7f]/g, '') : '';
    const extension = extname(nombre).toLowerCase();
    const formato = Object.hasOwn(formatos, extension) ? formatos[extension] : null;
    if (!formato || original.size === 0 || original.size > LIMITE_ARCHIVO || nombre.length > 240) {
      throw new ErrorAplicacion(422, ARCHIVO_INVALIDO);
    }
    const datos = Buffer.from(await original.arrayBuffer());
    if (!formato[1](datos)) throw new ErrorAplicacion(422, ARCHIVO_INVALIDO);

    const locacionId = Number(formulario.get('locacion'));
    const descripcion = String(formulario.get('descripcion') ?? '').trim();
    if (!Number.isSafeInteger(locacionId) || !locacionActiva(db, locacionId) ||
        !descripcion || descripcion.length > 2000) {
      throw new ErrorAplicacion(422, 'Selecciona una locación y escribe una descripción de hasta 2000 caracteres.');
    }
    const fecha = normalizarFecha(formulario.get('fecha'));
    const folio = randomUUID();
    transaccion(db, () => {
      const binario = db.prepare('INSERT INTO binarios (nombre, tipo, contenido) VALUES (?, ?, ?)')
        .run(nombre, formato[0], datos);
      db.prepare(`
        INSERT INTO propuestas (folio, locacion_id, binario_id, descripcion, fecha_historica)
        VALUES (?, ?, ?, ?, ?)
      `).run(folio, locacionId, binario.lastInsertRowid, descripcion, fecha);
    });
    return folio;
  }

  static buscar(db, folio) {
    const fila = db.prepare(`
      SELECT p.*, b.nombre, b.tipo, l.nombre AS locacion FROM propuestas p
      JOIN binarios b ON b.id = p.binario_id
      JOIN locaciones l ON l.id = p.locacion_id WHERE p.folio = ?
    `).get(folio);
    if (!fila) throw new ErrorAplicacion(404, 'No se encontró la propuesta.');
    return new PropuestaAporte(db, fila);
  }

  revisar(usuario) {
    this.db.prepare(`
      UPDATE propuestas SET estado = 'Revisada', curador = ?
      WHERE folio = ? AND estado = 'Pendiente'
    `).run(usuario, this.fila.folio);
  }

  aprobar(usuario, formulario) {
    const p = this.fila;
    const titulo = String(formulario.get('titulo') ?? '').trim() || parse(p.nombre).name;
    if (titulo.length > 240) throw new ErrorAplicacion(422, 'El título puede tener hasta 240 caracteres.');
    const fecha = normalizarFecha(formulario.get('fecha')) || p.fecha_historica;
    return transaccion(this.db, () => {
      const locacion = locacionActiva(this.db, p.locacion_id);
      if (!locacion) throw new ErrorAplicacion(422, 'La locación de esta propuesta ya no está activa.');
      const disponible = this.db.prepare('SELECT 1 FROM binarios WHERE id = ? AND contenido IS NOT NULL').get(p.binario_id);
      if (!disponible) throw new ErrorAplicacion(422, 'El original del aporte no está disponible.');
      this.dictaminar(usuario, 'Aprobada', null);
      const archivo = new Archivo(this.db, locacion);
      const documento = new Documento({ ...p, titulo, fecha_historica: fecha });
      const id = archivo.incorporar(documento, p.folio);
      this.db.prepare('UPDATE propuestas SET fecha_historica = ? WHERE folio = ?').run(fecha, p.folio);
      return id;
    });
  }

  rechazar(usuario, entrada) {
    const motivo = String(entrada ?? '').trim();
    if (!motivo) throw new ErrorAplicacion(422, MOTIVO_REQUERIDO);
    if (motivo.length > 2000) throw new ErrorAplicacion(422, 'El motivo puede tener hasta 2000 caracteres.');
    this.dictaminar(usuario, 'Rechazada', motivo);
  }

  dictaminar(usuario, estado, motivo) {
    const resultado = this.db.prepare(`
      UPDATE propuestas SET estado = ?, motivo = ?, curador = ?, dictaminada = CURRENT_TIMESTAMP
      WHERE folio = ? AND estado = 'Revisada'
    `).run(estado, motivo, usuario, this.fila.folio);
    if (resultado.changes !== 1) throw new ErrorAplicacion(409, 'Solo se pueden dictaminar propuestas en revisión.');
  }
}

export function documentoPublicado(db, id) {
  const fila = db.prepare(`
    SELECT d.*, b.nombre, b.tipo FROM documentos d
    JOIN binarios b ON b.id = d.binario_id JOIN locaciones l ON l.id = d.locacion_id
    WHERE d.id = ? AND l.activa = 1
  `).get(id);
  return fila ? new Documento(fila) : null;
}
