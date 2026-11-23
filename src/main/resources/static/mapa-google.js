/** @returns {import('./mapa-vista.js').ProveedorMapa} */
export function crearProveedorGoogle({ clave, idMapa }) {
  return {
    cargar(solicitud) {
      window.gm_authFailure = solicitud.fallar;
      window.iniciarMapaNoventas = () => {
        if (!solicitud.activo()) return;
        try {
          renderizar(window.google.maps, idMapa, solicitud);
        } catch {
          solicitud.fallar();
        }
      };

      const parametros = new URLSearchParams({
        key: clave, callback: 'iniciarMapaNoventas', loading: 'async', v: 'weekly', libraries: 'marker',
        language: 'es', region: 'MX',
      });
      const script = document.createElement('script');
      script.src = `https://maps.googleapis.com/maps/api/js?${parametros}`;
      script.async = true;
      script.onerror = solicitud.fallar;
      document.head.append(script);
    },
  };
}

/** @param {import('./mapa-vista.js').SolicitudMapa} solicitud */
function renderizar(maps, idMapa, solicitud) {
  const limites = new maps.LatLngBounds();
  const mapa = new maps.Map(solicitud.contenedor, {
    center: { lat: 21.1619, lng: -86.8515 },
    zoom: 13,
    mapId: idMapa,
    streetViewControl: false,
    mapTypeControl: false,
  });
  const info = new maps.InfoWindow();
  for (const punto of solicitud.puntos) {
    const marcador = new maps.marker.AdvancedMarkerElement({
      map: mapa, position: punto.posicion, title: punto.titulo, gmpClickable: true,
    });
    limites.extend(punto.posicion);
    marcador.addEventListener('gmp-click', () => {
      info.setContent(punto.ficha());
      info.open({ map: mapa, anchor: marcador });
    });
  }
  solicitud.montado();
  if (solicitud.puntos.length > 1) mapa.fitBounds(limites, 72);
  else if (solicitud.puntos.length === 1) mapa.setCenter(limites.getCenter());
  maps.event.addListenerOnce(mapa, 'tilesloaded', solicitud.listo);
}
