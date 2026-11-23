/**
 * @typedef {Object} PuntoDelMapa
 * @property {{lat: number, lng: number}} posicion
 * @property {string} titulo
 * @property {() => HTMLElement} ficha
 */

/**
 * @typedef {Object} SolicitudMapa
 * @property {HTMLElement} contenedor
 * @property {PuntoDelMapa[]} puntos
 * @property {() => boolean} activo Indica si todavía se puede montar el mapa.
 * @property {() => void} montado Muestra el contenedor antes de ajustar sus límites.
 * @property {() => void} listo Indica que terminó de cargar el mapa.
 * @property {() => void} fallar Activa la vista alternativa.
 */

/**
 * @typedef {Object} ProveedorMapa
 * @property {(solicitud: SolicitudMapa) => void} cargar
 */

/**
 * @param {HTMLElement} panel
 * @param {HTMLElement} estado
 * @param {ProveedorMapa} proveedor
 */
export function prepararMapa(panel, estado, proveedor) {
  let cancelado = false;
  const mostrarCatalogo = () => {
    cancelado = true;
    panel.hidden = true;
    estado.hidden = false;
    estado.textContent = 'El mapa no está disponible. Puedes explorar las locaciones en el catálogo.';
  };
  const espera = window.setTimeout(mostrarCatalogo, 5000);
  const fallar = () => {
    window.clearTimeout(espera);
    mostrarCatalogo();
  };
  window.addEventListener('offline', fallar);

  proveedor.cargar({
    contenedor: panel.querySelector('.mapa'),
    puntos: puntosDelCatalogo(),
    activo: () => !cancelado,
    montado: () => {
      if (!cancelado) panel.hidden = false;
    },
    listo: () => {
      if (cancelado) return;
      window.clearTimeout(espera);
      estado.hidden = true;
    },
    fallar,
  });
}

function puntosDelCatalogo() {
  return Array.from(document.querySelectorAll('.lista-locaciones li'), (locacion) => {
    const enlace = locacion.querySelector('a');
    return {
      posicion: { lat: Number(locacion.dataset.lat), lng: Number(locacion.dataset.lng) },
      titulo: enlace.textContent,
      ficha: () => crearFicha(locacion, enlace),
    };
  });
}

function crearFicha(locacion, enlace) {
  const contenido = document.createElement('div');
  contenido.className = 'ficha-mapa';
  const titulo = document.createElement('strong');
  titulo.textContent = enlace.textContent;
  const descripcion = document.createElement('p');
  descripcion.textContent = locacion.querySelector('.resena').textContent;
  const consultar = enlace.cloneNode();
  consultar.textContent = 'Consultar archivo';
  contenido.append(titulo, descripcion, consultar);
  return contenido;
}
