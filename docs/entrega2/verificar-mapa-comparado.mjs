import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { runInNewContext } from 'node:vm';

const versiones = ['v1-antes', 'v2.1-refactorizado'];
const locaciones = [
  { titulo: 'Locación pública', texto: 'Reseña histórica de prueba.', ruta: '/locaciones/1', lat: 21.16, lng: -86.83 },
  { titulo: 'Segunda locación', texto: 'Otra reseña.', ruta: '/locaciones/2', lat: 21.19, lng: -86.90 },
];

function leerScript(version, archivo) {
  const ruta = version + ':src/main/resources/static/' + archivo;
  return execFileSync('git', ['show', ruta], { encoding: 'utf8' });
}

async function importarScript(version, archivo) {
  const codigo = leerScript(version, archivo);
  return import('data:text/javascript;base64,' + Buffer.from(codigo).toString('base64'));
}

function simular() {
  const panel = {
    hidden: true,
    dataset: { key: 'clave-simulada', mapId: 'DEMO_MAP_ID' },
    querySelector: () => ({}),
  };
  const estado = { hidden: false };
  const scripts = [];
  const marcadores = [];
  const eventos = new Map();
  const fichas = locaciones.map(({ titulo, texto, ruta, lat, lng }) => ({
    dataset: { lat: String(lat), lng: String(lng) },
    querySelector: selector => selector === 'a'
      ? { textContent: titulo, cloneNode: () => ({ href: ruta }) }
      : { textContent: texto },
  }));
  let tarjeta;
  const documento = {
    querySelector: selector => selector === '[data-mapa]' ? panel : estado,
    querySelectorAll: () => fichas,
    createElement: () => ({ append(...hijos) { this.hijos = hijos; } }),
    head: { append: script => scripts.push(script) },
  };
  const ventana = {
    setTimeout: () => 1,
    clearTimeout() {},
    addEventListener() {},
    google: { maps: {
      LatLngBounds: class { extend() {} },
      Map: class { fitBounds() {} },
      InfoWindow: class { setContent(ficha) { tarjeta = ficha; } open() {} },
      marker: { AdvancedMarkerElement: class {
        constructor(opciones) {
          Object.assign(this, opciones);
          marcadores.push(this);
        }
        addEventListener(evento, callback) {
          assert.equal(evento, 'gmp-click');
          this.pulsar = callback;
        }
      } },
      event: { addListenerOnce(_mapa, evento, callback) { eventos.set(evento, callback); } },
    } },
  };
  return { panel, estado, scripts, marcadores, eventos, documento, ventana, tarjeta: () => tarjeta };
}

async function iniciar(version, ambiente) {
  const { panel, estado, documento, ventana } = ambiente;
  if (version === 'v1-antes') {
    runInNewContext(leerScript(version, 'mapa.js'), { document: documento, window: ventana, URLSearchParams });
    return;
  }
  const { prepararMapa } = await importarScript(version, 'mapa-vista.js');
  const { crearProveedorGoogle } = await importarScript(version, 'mapa-google.js');
  globalThis.window = ventana;
  globalThis.document = documento;
  const proveedor = crearProveedorGoogle({ clave: panel.dataset.key, idMapa: panel.dataset.mapId });
  prepararMapa(panel, estado, proveedor);
}

for (const version of versiones) {
  const correcto = simular();
  await iniciar(version, correcto);
  assert.equal(correcto.scripts.length, 1);
  correcto.ventana.iniciarMapaNoventas();
  correcto.eventos.get('tilesloaded')();
  assert.equal(correcto.panel.hidden, false);
  assert.equal(correcto.estado.hidden, true);
  assert.equal(correcto.marcadores.length, locaciones.length);
  for (const [i, marcador] of correcto.marcadores.entries()) {
    const locacion = locaciones[i];
    assert.equal(marcador.title, locacion.titulo);
    assert.equal(marcador.position.lat, locacion.lat);
    assert.equal(marcador.position.lng, locacion.lng);
    marcador.pulsar();
    const [titulo, descripcion, enlace] = correcto.tarjeta().hijos;
    assert.equal(titulo.textContent, locacion.titulo);
    assert.equal(descripcion.textContent, locacion.texto);
    assert.equal(enlace.href, locacion.ruta);
  }
  console.log(version, 'V-01a: dos marcadores con coordenadas, reseña y enlace correctos');

  const fallo = simular();
  await iniciar(version, fallo);
  assert.equal(fallo.scripts.length, 1);
  fallo.scripts[0].onerror();
  fallo.ventana.iniciarMapaNoventas();
  assert.equal(fallo.panel.hidden, true);
  assert.equal(fallo.estado.hidden, false);
  assert.equal(fallo.marcadores.length, 0);
  assert.match(fallo.estado.textContent, /explorar las locaciones en el catálogo/);
  console.log(version, 'V-01b: SDK simulado falla y queda el catálogo');
}
