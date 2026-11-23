import { crearProveedorGoogle } from './mapa-google.js';
import { prepararMapa } from './mapa-vista.js';

const panel = document.querySelector('[data-mapa]');
if (panel?.dataset.key) {
  const proveedor = crearProveedorGoogle({ clave: panel.dataset.key, idMapa: panel.dataset.mapId });
  prepararMapa(panel, document.querySelector('[data-estado-mapa]'), proveedor);
}
