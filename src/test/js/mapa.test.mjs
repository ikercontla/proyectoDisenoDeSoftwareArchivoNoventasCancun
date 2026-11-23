import assert from 'node:assert/strict';
import test from 'node:test';
import { crearProveedorGoogle } from '../../main/resources/static/mapa-google.js';
import { prepararMapa } from '../../main/resources/static/mapa-vista.js';

const avisoCatalogo = 'El mapa no está disponible. Puedes explorar las locaciones en el catálogo.';
const parque = {
  titulo: 'Parque de las Palapas', descripcion: 'Ficha histórica.', href: '/locaciones/1',
  lat: '21.16132', lng: '-86.82869',
};
const palacio = {
  titulo: 'Palacio Municipal', descripcion: 'Reseña del sitio.', href: '/locaciones/2',
  lat: '21.16185', lng: '-86.82572',
};

function entorno(t, locaciones) {
  const contenedor = {};
  const panel = { hidden: true, dataset: {}, querySelector: () => contenedor };
  const estado = { hidden: false, textContent: 'Cargando el mapa…' };
  const scripts = [];
  const temporizadores = new Map();
  const eventos = new Map();
  const fichas = locaciones.map(({ lat, lng, titulo, descripcion, href }) => ({
    dataset: { lat, lng },
    querySelector: (selector) => selector === 'a'
      ? { textContent: titulo, cloneNode: () => ({ href }) }
      : { textContent: descripcion },
  }));
  const pruebaVentana = {
    setTimeout: (callback, ms) => {
      const id = Symbol();
      temporizadores.set(id, { callback, ms });
      return id;
    },
    clearTimeout: (id) => temporizadores.delete(id),
    addEventListener: (evento, callback) => eventos.set(evento, callback),
  };
  const pruebaDocumento = {
    querySelector: (selector) => selector === '[data-mapa]' ? panel : estado,
    querySelectorAll: (selector) => {
      assert.equal(selector, '.lista-locaciones li');
      return fichas;
    },
    createElement: (tag) => ({
      tag, append(...hijos) { this.hijos = hijos; },
    }),
    head: { append: (script) => scripts.push(script) },
  };
  for (const [clave, valor] of Object.entries({ window: pruebaVentana, document: pruebaDocumento })) {
    const anterior = Object.getOwnPropertyDescriptor(globalThis, clave);
    globalThis[clave] = valor;
    t.after(() => {
      if (anterior) Object.defineProperty(globalThis, clave, anterior);
      else delete globalThis[clave];
    });
  }
  return { ventana: pruebaVentana, contenedor, panel, estado, scripts, temporizadores, eventos };
}

function proveedorSimulado() {
  return { cargar(solicitud) { this.solicitud = solicitud; } };
}

function comprobarContingencia({ panel, estado }, solicitud) {
  assert.equal(panel.hidden, true);
  assert.equal(estado.hidden, false);
  assert.equal(estado.textContent, avisoCatalogo);
  assert.equal(solicitud.activo(), false);
}

test('V-01a y V-12a: un proveedor simulado recibe los marcadores visibles y su ficha', (t) => {
  const ambiente = entorno(t, [palacio]);
  const proveedor = proveedorSimulado();
  prepararMapa(ambiente.panel, ambiente.estado, proveedor);
  const solicitud = proveedor.solicitud;

  assert.equal(solicitud.contenedor, ambiente.contenedor);
  assert.deepEqual(solicitud.puntos.map((p) => p.posicion), [{ lat: 21.16185, lng: -86.82572 }]);
  solicitud.montado();
  assert.equal(ambiente.panel.hidden, false);
  assert.equal(ambiente.estado.hidden, false);
  solicitud.listo();
  assert.equal(ambiente.estado.hidden, true);
  assert.equal(ambiente.temporizadores.size, 0);

  const contenido = solicitud.puntos[0].ficha();
  const [titulo, descripcion, enlace] = contenido.hijos;
  assert.equal(contenido.className, 'ficha-mapa');
  assert.equal(titulo.textContent, palacio.titulo);
  assert.equal(descripcion.textContent, palacio.descripcion);
  assert.equal(enlace.href, palacio.href);
  assert.equal(enlace.textContent, 'Consultar archivo');
});

test('V-01b: un proveedor simulado falla y los eventos tardíos conservan el catálogo', (t) => {
  const ambiente = entorno(t, [parque]);
  const proveedor = proveedorSimulado();
  prepararMapa(ambiente.panel, ambiente.estado, proveedor);
  const solicitud = proveedor.solicitud;

  solicitud.montado();
  solicitud.fallar();
  assert.equal(ambiente.temporizadores.size, 0);
  solicitud.montado();
  solicitud.listo();
  comprobarContingencia(ambiente, solicitud);
});

test('V-01b: la espera máxima se conserva en cinco segundos', (t) => {
  const ambiente = entorno(t, [parque]);
  const proveedor = proveedorSimulado();
  prepararMapa(ambiente.panel, ambiente.estado, proveedor);
  const [espera] = ambiente.temporizadores.values();

  assert.equal(espera.ms, 5000);
  espera.callback();
  comprobarContingencia(ambiente, proveedor.solicitud);
});

test('V-01b: la desconexión muestra el catálogo incluso después de montar el mapa', (t) => {
  const ambiente = entorno(t, [parque]);
  const proveedor = proveedorSimulado();
  prepararMapa(ambiente.panel, ambiente.estado, proveedor);
  proveedor.solicitud.montado();
  proveedor.solicitud.listo();

  ambiente.eventos.get('offline')();
  comprobarContingencia(ambiente, proveedor.solicitud);
});

test('V-01a: el módulo de Google conserva opciones, marcadores, encuadre y ficha', (t) => {
  const ambiente = entorno(t, [parque, palacio]);
  const marcadores = [];
  let mapa;
  let ventanaInfo;
  let terminarCarga;
  ambiente.ventana.google = { maps: {
    LatLngBounds: class {
      posiciones = [];
      extend(posicion) { this.posiciones.push(posicion); }
    },
    Map: class {
      constructor(contenedor, opciones) {
        this.contenedor = contenedor;
        this.opciones = opciones;
        mapa = this;
      }
      fitBounds(limites, margen) {
        assert.equal(ambiente.panel.hidden, false);
        this.encuadre = { posiciones: limites.posiciones, margen };
      }
    },
    InfoWindow: class {
      constructor() { ventanaInfo = this; }
      setContent(contenido) { this.contenido = contenido; }
      open(opciones) { this.opciones = opciones; }
    },
    marker: { AdvancedMarkerElement: class {
      constructor(opciones) { this.opciones = opciones; marcadores.push(this); }
      addEventListener(evento, callback) {
        assert.equal(evento, 'gmp-click');
        this.seleccionar = callback;
      }
    } },
    event: { addListenerOnce(_mapa, evento, callback) {
      assert.equal(evento, 'tilesloaded');
      terminarCarga = callback;
    } },
  } };

  const proveedor = crearProveedorGoogle({ clave: 'prueba-local', idMapa: 'DEMO_MAP_ID' });
  prepararMapa(ambiente.panel, ambiente.estado, proveedor);
  const script = new URL(ambiente.scripts[0].src);
  assert.equal(script.origin + script.pathname, 'https://maps.googleapis.com/maps/api/js');
  assert.equal(script.searchParams.get('libraries'), 'marker');
  assert.equal(script.searchParams.get('callback'), 'iniciarMapaNoventas');
  ambiente.ventana.iniciarMapaNoventas();

  assert.equal(mapa.contenedor, ambiente.contenedor);
  assert.equal(mapa.opciones.mapId, 'DEMO_MAP_ID');
  assert.deepEqual(mapa.encuadre, {
    posiciones: [{ lat: 21.16132, lng: -86.82869 }, { lat: 21.16185, lng: -86.82572 }],
    margen: 72,
  });
  assert.equal(marcadores.length, 2);
  assert.equal(marcadores[0].opciones.gmpClickable, true);
  marcadores[0].seleccionar();
  assert.equal(ventanaInfo.contenido.hijos[2].href, parque.href);
  assert.equal(ventanaInfo.opciones.anchor, marcadores[0]);
  terminarCarga();
  assert.equal(ambiente.estado.hidden, true);
});

test('V-01b: el módulo de Google comunica un error de carga a la vista', (t) => {
  const ambiente = entorno(t, [parque]);
  const proveedor = crearProveedorGoogle({ clave: 'prueba-local', idMapa: 'DEMO_MAP_ID' });
  prepararMapa(ambiente.panel, ambiente.estado, proveedor);

  ambiente.scripts[0].onerror();
  ambiente.ventana.iniciarMapaNoventas();
  assert.equal(ambiente.panel.hidden, true);
  assert.equal(ambiente.estado.textContent, avisoCatalogo);
});

test('Sin clave, el módulo de entrada deja disponible el catálogo sin solicitar el proveedor', async (t) => {
  const ambiente = entorno(t, [parque]);
  await import('../../main/resources/static/mapa.js');
  assert.equal(ambiente.scripts.length, 0);
  assert.equal(ambiente.panel.hidden, true);
  assert.equal(ambiente.temporizadores.size, 0);
});
