import assert from 'node:assert/strict';
import { once } from 'node:events';
import { readFileSync } from 'node:fs';
import { after, before, test } from 'node:test';
import { runInNewContext } from 'node:vm';
import { abrirAlmacen } from '../src/almacen.mjs';
import { fichaDemo } from '../src/preparar.mjs';
import { crearServidor } from '../src/servidor.mjs';
import { crearAdministrador } from '../src/sesiones.mjs';

const log = [];
const clave = 'Cuenta de prueba 1995';
const db = abrirAlmacen(':memory:');
const servidor = crearServidor(db, {
  mapsKey: 'clave-simulada',
  log: { warn: (mensaje) => log.push(mensaje), error: (error) => { throw error; } },
});
let base;
let documento;
let documentoSinBinario;

before(async () => {
  const locacion = db.prepare('INSERT INTO locaciones VALUES (?, ?, ?, ?, ?, ?)');
  locacion.run(1, 'Locación pública', 'Reseña histórica de prueba.', 21.16, -86.83, 1);
  locacion.run(2, 'Segunda locación', 'Otra reseña.', 21.19, -86.9, 1);
  locacion.run(3, 'Locación inactiva', 'Fuera del catálogo.', 21.20, -86.80, 0);
  const binario = db.prepare('INSERT INTO binarios (nombre, tipo, contenido) VALUES (?, ?, ?)');
  const publicar = db.prepare('INSERT INTO documentos (locacion_id, binario_id, titulo, descripcion) VALUES (1, ?, ?, ?)');
  documento = publicar.run(binario.run('ficha histórica.pdf', 'application/pdf', fichaDemo()).lastInsertRowid,
    'Un documento publicado', 'Descripción de la ficha.').lastInsertRowid;
  documentoSinBinario = publicar.run(binario.run('ausente.pdf', 'application/pdf', null).lastInsertRowid,
    'Documento sin original', 'Caso de error.').lastInsertRowid;
  await crearAdministrador(db, 'curador', clave);
  servidor.listen(0, '127.0.0.1');
  await once(servidor, 'listening');
  base = `http://127.0.0.1:${servidor.address().port}`;
});

after(async () => {
  if (servidor.listening) {
    servidor.close();
    await once(servidor, 'close');
  }
  db.close();
});

const consultar = (ruta, opciones) => fetch(base + ruta, { redirect: 'manual', ...opciones });
const enviar = (ruta, datos, sesion) => consultar(ruta, {
  method: 'POST',
  headers: sesion ? { Cookie: sesion } : {},
  body: datos,
});
const propuesta = () => {
  const datos = new FormData();
  datos.set('locacion', '1');
  datos.set('descripcion', 'Aporte de prueba');
  return datos;
};
const panel = '/administracion/propuestas';

const iniciarSesion = () => enviar('/administracion', new URLSearchParams({ usuario: 'curador', contrasena: clave }));

async function cookieDeCurador() {
  const respuesta = await iniciarSesion();
  assert.equal(respuesta.status, 303);
  return respuesta.headers.get('set-cookie').split(';', 1)[0];
}

async function enviarPropuesta() {
  const datos = propuesta();
  datos.set('archivo', new Blob([fichaDemo()]), 'aporte.pdf');
  const respuesta = await enviar('/aportar', datos);
  assert.equal(respuesta.status, 303);
  const destino = respuesta.headers.get('location');
  const folio = new URL(destino, base).searchParams.get('folio');
  assert.match(folio, /^[a-f0-9-]{36}$/);
  return { destino, folio, ruta: `${panel}/${folio}` };
}

function mapaSimulado() {
  const estado = { hidden: false };
  const panelMapa = { dataset: { key: 'clave-simulada', mapId: 'DEMO_MAP_ID' }, hidden: true, querySelector: () => ({}) };
  const enlace = { textContent: 'Locación pública', href: '/locaciones/1', cloneNode: () => ({ href: '/locaciones/1' }) };
  const ficha = { dataset: { lat: '21.16', lng: '-86.83' }, querySelector: (s) => s === 'a' ? enlace : { textContent: 'Reseña histórica.' } };
  let script;
  let marcador;
  let contenido;
  let alCargarTeselas;
  const document = {
    querySelector: (s) => s === '[data-mapa]' ? panelMapa : estado,
    querySelectorAll: () => [ficha],
    createElement: () => ({ append(...hijos) { this.hijos = hijos; } }),
    head: { append(nodo) { script = nodo; } },
  };
  const window = {
    setTimeout: () => 1,
    clearTimeout() {},
    addEventListener() {},
    google: { maps: {
      LatLngBounds: class { extend() {} getCenter() {} },
      Map: class { setCenter() {} },
      InfoWindow: class { setContent(valor) { contenido = valor; } open() {} },
      marker: { AdvancedMarkerElement: class {
        constructor(opciones) { this.opciones = opciones; marcador = this; }
        addEventListener(evento, callback) { assert.equal(evento, 'gmp-click'); this.pulsar = callback; }
      } },
      event: { addListenerOnce(_mapa, evento, callback) {
        assert.equal(evento, 'tilesloaded');
        alCargarTeselas = callback;
      } },
    } },
  };
  runInNewContext(readFileSync(new URL('../public/mapa.js', import.meta.url), 'utf8'), { document, window, URLSearchParams });
  return {
    estado, panelMapa, script,
    iniciar() { window.iniciarMapaNoventas(); alCargarTeselas(); return marcador; },
    contenido: () => contenido,
  };
}

test('V-01a: coordenadas y ficha del marcador con la API de mapas simulada', async () => {
  const inicio = await consultar('/');
  assert.equal(inicio.status, 200);
  const pagina = await inicio.text();
  assert.match(pagina, /data-lat="21\.16" data-lng="-86\.83"/);
  assert.match(pagina, /\/mapa\.js/);
  assert.doesNotMatch(pagina, /Locación inactiva/);
  const mapa = mapaSimulado();
  assert.match(mapa.script.src, /^https:\/\/maps\.googleapis\.com\/maps\/api\/js/);
  const marcador = mapa.iniciar();
  assert.equal(marcador.opciones.position.lat, 21.16);
  assert.equal(marcador.opciones.position.lng, -86.83);
  assert.equal(mapa.panelMapa.hidden, false);
  marcador.pulsar();
  const [titulo, descripcion, enlace] = mapa.contenido().hijos;
  assert.equal(titulo.textContent, 'Locación pública');
  assert.equal(descripcion.textContent, 'Reseña histórica.');
  assert.equal(enlace.href, '/locaciones/1');
});

test('V-01b: fallo de carga muestra catálogo de locaciones activas', async () => {
  const mapa = mapaSimulado();
  mapa.script.onerror();
  assert.equal(mapa.panelMapa.hidden, true);
  assert.equal(mapa.estado.hidden, false);
  assert.match(mapa.estado.textContent, /explorar las locaciones en el catálogo/);
  const pagina = await (await consultar('/')).text();
  assert.match(pagina, /21\.16000, -86\.83000/);
  assert.match(pagina, /21\.19000, -86\.90000/);
  assert.doesNotMatch(pagina, /Locación inactiva/);
});

test('V-02a: ficha con reseña y documentos de la locación', async () => {
  const r = await consultar('/locaciones/1');
  assert.equal(r.status, 200);
  const ficha = await r.text();
  assert.match(ficha, /Reseña histórica de prueba/);
  assert.match(ficha, /Un documento publicado/);
  assert.match(ficha, new RegExp(`/documentos/${documento}/original`));
});

test('V-02b: locaciones inexistentes e inactivas devuelven 404 y retorno al mapa', async () => {
  for (const id of [300, 3]) {
    const r = await consultar(`/locaciones/${id}`);
    assert.equal(r.status, 404);
    const ficha = await r.text();
    assert.match(ficha, /La locación no existe/);
    assert.match(ficha, /http-equiv="refresh" content="5; url=\/"/);
  }
});

test('V-03a: se conservan el nombre y los bytes originales', async () => {
  const r = await consultar(`/documentos/${documento}/original`);
  assert.equal(r.status, 200);
  assert.equal(r.headers.get('content-type'), 'application/pdf');
  assert.match(r.headers.get('content-disposition'), /filename\*=UTF-8''ficha%20hist%C3%B3rica\.pdf/);
  assert.deepEqual(Buffer.from(await r.arrayBuffer()), fichaDemo());
});

test('V-03b: el binario faltante se registra y muestra el mensaje del SRS', async () => {
  const r = await consultar(`/documentos/${documentoSinBinario}/original`);
  assert.equal(r.status, 404);
  assert.match(await r.text(), /El documento solicitado no está disponible temporalmente para descarga/);
  assert.ok(log.includes(`Original ausente para el documento ${documentoSinBinario}`));
});

test('V-04a: aporte anónimo con folio y estado Pendiente', async () => {
  const { destino, folio } = await enviarPropuesta();
  const confirmacion = await (await consultar(destino)).text();
  assert.match(confirmacion, new RegExp(folio));
  assert.equal(db.prepare('SELECT estado FROM propuestas WHERE folio = ?').get(folio).estado, 'Pendiente');
});

test('V-04b: se rechazan el adjunto ausente, el tipo no permitido y los archivos que exceden 25 MB', async () => {
  const total = () => db.prepare('SELECT COUNT(*) AS n FROM propuestas').get().n;
  const antes = total();
  for (const nombre of [null, 'archivo.zip', 'archivo.pdf']) {
    const datos = propuesta();
    if (nombre) datos.set('archivo', new Blob(['contenido incompatible']), nombre);
    const r = await enviar('/aportar', datos);
    assert.equal(r.status, 422);
    assert.match(await r.text(), /Debe adjuntar un archivo válido/);
  }
  const datos = propuesta();
  datos.set('archivo', new Blob([fichaDemo(), Buffer.alloc(25_000_001)]), 'archivo.pdf');
  const grande = await enviar('/aportar', datos);
  assert.ok([413, 422].includes(grande.status));
  assert.match(await grande.text(), /Debe adjuntar un archivo válido/);
  assert.equal(total(), antes);
});

test('V-05a: credenciales válidas abren sesión y permiten acceder al panel', async () => {
  const r = await iniciarSesion();
  assert.equal(r.status, 303);
  assert.equal(r.headers.get('location'), panel);
  const cabecera = r.headers.get('set-cookie');
  assert.match(cabecera, /HttpOnly; SameSite=Strict/);
  const cookie = cabecera.split(';', 1)[0];
  const consola = await consultar(panel, { headers: { Cookie: cookie } });
  assert.equal(consola.status, 200);
  assert.match(await consola.text(), /Propuestas ciudadanas/);
});

test('V-05b: contraseña inválida incrementa intentos y no permite acceso', async () => {
  const acumular = () => db.prepare('SELECT intentos_fallidos FROM administradores').get().intentos_fallidos;
  const antes = acumular();
  const r = await enviar('/administracion', new URLSearchParams({ usuario: 'curador', contrasena: 'incorrecta' }));
  assert.equal(r.status, 401);
  assert.equal(r.headers.get('set-cookie'), null);
  assert.match(await r.text(), /Credenciales no válidas\. Verifique sus datos/);
  assert.equal(acumular(), antes + 1);
  assert.equal((await consultar(panel)).headers.get('location'), '/administracion');
});

test('V-06a: revisar y aprobar publica un único documento visible en la locación', async () => {
  const cookie = await cookieDeCurador();
  const { ruta, folio } = await enviarPropuesta();
  const estado = () => db.prepare('SELECT estado FROM propuestas WHERE folio = ?').get(folio).estado;
  const datos = new URLSearchParams({ titulo: 'Publicación curada' });
  assert.equal((await enviar(`${ruta}/aprobar`, datos, cookie)).status, 409);
  assert.equal((await enviar(`${ruta}/revisar`, undefined, cookie)).status, 303);
  assert.equal(estado(), 'Revisada');
  assert.equal((await enviar(`${ruta}/aprobar`, datos, cookie)).status, 303);
  assert.equal(estado(), 'Aprobada');
  assert.match(await (await consultar('/locaciones/1')).text(), /Publicación curada/);
  assert.equal((await enviar(`${ruta}/aprobar`, datos, cookie)).status, 409);
  assert.equal(db.prepare('SELECT COUNT(*) AS n FROM documentos WHERE propuesta_folio = ?').get(folio).n, 1);
});

test('V-06b: rechazo sin motivo conserva Revisada y con motivo registra Rechazada', async () => {
  const cookie = await cookieDeCurador();
  const { ruta, folio } = await enviarPropuesta();
  assert.equal((await enviar(`${ruta}/revisar`, undefined, cookie)).status, 303);
  const intento = await enviar(`${ruta}/rechazar`, new URLSearchParams({ motivo: '  ' }), cookie);
  assert.equal(intento.status, 422);
  assert.match(await intento.text(), /Debe ingresar obligatoriamente una justificación para rechazar la propuesta/);
  const leer = () => db.prepare('SELECT estado, motivo FROM propuestas WHERE folio = ?').get(folio);
  assert.equal(leer().estado, 'Revisada');
  assert.equal((await enviar(`${ruta}/rechazar`, new URLSearchParams({ motivo: 'Duplicado' }), cookie)).status, 303);
  assert.equal(leer().estado, 'Rechazada');
  assert.equal(leer().motivo, 'Duplicado');
});
