import { createServer } from 'node:http';
import { readFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';
import { abrirAlmacen, locacionActiva, locacionesActivas } from './almacen.mjs';
import {
  Archivo, PropuestaAporte, ErrorAplicacion, documentoPublicado,
  ARCHIVO_INVALIDO, DESCARGA_NO_DISPONIBLE, LIMITE_ARCHIVO,
} from './dominio.mjs';
import { autenticar, cerrarSesion, cookieSesion, iniciarSesion, usuarioActivo } from './sesiones.mjs';
import { vistas } from './vistas.mjs';

const assets = new Map([
  ['/estilos.css', ['text/css; charset=utf-8', readFileSync(new URL('../public/estilos.css', import.meta.url))]],
  ['/mapa.js', ['text/javascript; charset=utf-8', readFileSync(new URL('../public/mapa.js', import.meta.url))]],
]);
const errorCredenciales = 'Credenciales no válidas. Verifique sus datos';
const rutaPanel = '/administracion/propuestas';

async function formulario(req, tipo = 'application/x-www-form-urlencoded') {
  const carga = tipo === 'multipart/form-data';
  const limite = carga ? LIMITE_ARCHIVO + 64_000 : 16_000;
  const mensaje = carga ? ARCHIVO_INVALIDO : 'El tamaño del formulario supera el límite permitido.';
  const contentType = req.headers['content-type'] ?? '';
  if (contentType.split(';', 1)[0].trim().toLowerCase() !== tipo) {
    throw new ErrorAplicacion(422, carga ? ARCHIVO_INVALIDO : 'El formato del formulario no es válido.');
  }
  if (Number(req.headers['content-length']) > limite) throw new ErrorAplicacion(413, mensaje);
  const bloques = [];
  let total = 0;
  for await (const bloque of req.iterator({ destroyOnReturn: false })) {
    total += bloque.length;
    if (total > limite) throw new ErrorAplicacion(413, mensaje);
    bloques.push(bloque);
  }
  try {
    return await new Response(Buffer.concat(bloques, total), { headers: { 'Content-Type': contentType } }).formData();
  } catch {
    throw new ErrorAplicacion(422, carga ? ARCHIVO_INVALIDO : 'El formulario no se pudo leer.');
  }
}

function html(res, estado, pagina) {
  res.writeHead(estado, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(pagina);
}

function redirigir(res, destino) {
  res.writeHead(303, { Location: destino });
  res.end();
}

function descargar(res, original) {
  const nombre = original.nombre;
  const ascii = nombre.replace(/[^a-zA-Z0-9._ -]/g, '_');
  const utf8 = encodeURIComponent(nombre).replace(/['()*]/g, (c) => `%${c.charCodeAt(0).toString(16)}`);
  res.writeHead(200, {
    'Content-Type': original.tipo,
    'Content-Length': original.contenido.length,
    'Content-Disposition': `attachment; filename="${ascii}"; filename*=UTF-8''${utf8}`,
  });
  res.end(original.contenido);
}

function validarOrigen(req, cookieSegura) {
  const origen = req.headers.origin;
  if (origen && origen !== `${cookieSegura ? 'https' : 'http'}://${req.headers.host}`) {
    throw new ErrorAplicacion(403, 'Envía el formulario desde el sitio del archivo.');
  }
}

export function crearServidor(db, {
  mapsKey = process.env.GOOGLE_MAPS_API_KEY ?? '',
  mapsId = process.env.GOOGLE_MAPS_MAP_ID || 'DEMO_MAP_ID',
  cookieSegura = process.env.COOKIE_SEGURA === '1',
  log = console,
} = {}) {
  return createServer({ requestTimeout: 30_000 }, async (req, res) => {
    res.setHeader('Cache-Control', 'no-store');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
    const metodo = req.method;
    let ruta = '';
    let detalleAdmin = null;
    try {
      if (!URL.canParse(req.url, 'http://localhost')) throw new ErrorAplicacion(400, 'La dirección de la solicitud no es válida.');
      const url = new URL(req.url, 'http://localhost');
      ruta = url.pathname;
      if (metodo === 'POST') validarOrigen(req, cookieSegura);
      if (metodo === 'GET' && assets.has(ruta)) {
        const [tipo, cuerpo] = assets.get(ruta);
        res.writeHead(200, { 'Content-Type': tipo });
        return res.end(cuerpo);
      }
      if (metodo === 'GET' && ruta === '/') {
        return html(res, 200, vistas.inicio(locacionesActivas(db), { mapsKey, mapsId }));
      }
      const locacionMatch = /^\/locaciones\/(\d+)$/.exec(ruta);
      if (metodo === 'GET' && locacionMatch) {
        const locacion = locacionActiva(db, Number(locacionMatch[1]));
        if (!locacion) return html(res, 404, vistas.locacionAusente());
        return html(res, 200, vistas.locacion(locacion, new Archivo(db, locacion).documentos()));
      }
      const descargaMatch = /^\/documentos\/(\d+)\/original$/.exec(ruta);
      if (metodo === 'GET' && descargaMatch) {
        const documento = documentoPublicado(db, Number(descargaMatch[1]));
        if (!documento) throw new ErrorAplicacion(404, 'El documento no existe.');
        const original = documento.descargar(db);
        if (!original) {
          log.warn(`Original ausente para el documento ${documento.id}`);
          throw new ErrorAplicacion(404, DESCARGA_NO_DISPONIBLE);
        }
        return descargar(res, original);
      }
      if (ruta === '/aportar') {
        if (metodo === 'GET') {
          const folio = url.searchParams.get('folio');
          const confirmado = /^[a-f0-9-]{36}$/.test(folio ?? '') &&
            db.prepare('SELECT 1 FROM propuestas WHERE folio = ?').get(folio);
          return html(res, 200, confirmado ? vistas.aporteRegistrado(folio) :
            vistas.aporte(locacionesActivas(db), { locacion: url.searchParams.get('locacion') }));
        }
        if (metodo === 'POST') {
          const datos = await formulario(req, 'multipart/form-data');
          try {
            const folio = await PropuestaAporte.recibir(db, datos);
            return redirigir(res, `/aportar?folio=${folio}`);
          } catch (error) {
            if (!(error instanceof ErrorAplicacion)) throw error;
            return html(res, error.estado, vistas.aporte(locacionesActivas(db), {
              error: error.message, locacion: datos.get('locacion'), descripcion: datos.get('descripcion'), fecha: datos.get('fecha'),
            }));
          }
        }
      }
      if (ruta === '/administracion' && metodo === 'GET') {
        return usuarioActivo(db, req) ? redirigir(res, rutaPanel) : html(res, 200, vistas.acceso());
      }
      if (ruta === '/administracion' && metodo === 'POST') {
        const datos = await formulario(req);
        const usuario = await autenticar(db, datos.get('usuario'), datos.get('contrasena'));
        if (!usuario) return html(res, 401, vistas.acceso(errorCredenciales, datos.get('usuario')));
        cerrarSesion(db, req);
        res.setHeader('Set-Cookie', cookieSesion(iniciarSesion(db, usuario), cookieSegura));
        return redirigir(res, rutaPanel);
      }
      if (ruta.startsWith('/administracion/')) {
        const usuario = usuarioActivo(db, req);
        if (!usuario) return redirigir(res, '/administracion');
        if (metodo === 'POST' && ruta === '/administracion/salir') {
          cerrarSesion(db, req);
          res.setHeader('Set-Cookie', cookieSesion('', cookieSegura));
          return redirigir(res, '/administracion');
        }
        if (metodo === 'GET' && ruta === rutaPanel) {
          const propuestas = db.prepare(`
            SELECT p.folio, p.estado, p.recibida, l.nombre AS locacion, b.nombre
            FROM propuestas p JOIN locaciones l ON l.id = p.locacion_id
            JOIN binarios b ON b.id = p.binario_id ORDER BY p.recibida DESC, p.rowid DESC
          `).all();
          return html(res, 200, vistas.panel(propuestas));
        }
        const match = /^\/administracion\/propuestas\/([a-f0-9-]{36})(?:\/(revisar|aprobar|rechazar|original))?$/.exec(ruta);
        if (match) {
          const [, folio, accion] = match;
          const p = PropuestaAporte.buscar(db, folio);
          detalleAdmin = p.fila;
          if (metodo === 'GET' && !accion) return html(res, 200, vistas.propuesta(p.fila));
          if (metodo === 'GET' && accion === 'original') {
            const binario = db.prepare('SELECT nombre, tipo, contenido FROM binarios WHERE id = ?').get(p.fila.binario_id);
            if (!binario?.contenido) throw new ErrorAplicacion(404, 'El original del aporte no está disponible.');
            return descargar(res, binario);
          }
          if (metodo === 'POST' && accion === 'revisar') {
            p.revisar(usuario);
            return redirigir(res, `${rutaPanel}/${folio}`);
          }
          if (metodo === 'POST' && (accion === 'aprobar' || accion === 'rechazar')) {
            const datos = await formulario(req);
            if (accion === 'aprobar') p.aprobar(usuario, datos);
            else p.rechazar(usuario, datos.get('motivo'));
            return redirigir(res, `${rutaPanel}/${folio}`);
          }
        }
      }
      throw new ErrorAplicacion(404, 'La página solicitada no existe.');
    } catch (error) {
      if (!req.complete) req.resume();
      if (res.destroyed) return;
      if (error instanceof ErrorAplicacion) {
        if (error.estado === 413) res.setHeader('Connection', 'close');
        if (ruta === '/aportar' && metodo === 'POST') {
          return html(res, error.estado, vistas.aporte(locacionesActivas(db), { error: error.message }));
        }
        if (detalleAdmin && metodo === 'POST') {
          return html(res, error.estado, vistas.propuesta(
            PropuestaAporte.buscar(db, detalleAdmin.folio).fila, error.message));
        }
        return html(res, error.estado, vistas.error(error.estado, error.message));
      }
      log.error(error);
      return html(res, 500, vistas.error(500, 'Ocurrió un error al procesar la solicitud.'));
    }
  });
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const servidor = crearServidor(abrirAlmacen());
  const host = process.env.HOST || '127.0.0.1';
  const port = Number(process.env.PORT || 3000);
  servidor.listen(port, host, () => {
    console.log(`Archivo Noventas Cancún disponible en http://${host}:${servidor.address().port}`);
  });
}
