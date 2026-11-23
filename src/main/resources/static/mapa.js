'use strict';

function prepararMapa() {
  const panel = document.querySelector('[data-mapa]');
  const estado = document.querySelector('[data-estado-mapa]');
  if (!panel?.dataset.key) return;

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

  window.gm_authFailure = fallar;
  window.addEventListener('offline', fallar);
  window.iniciarMapaNoventas = () => {
    if (cancelado) return;
    try {
      const maps = window.google.maps;
      const fichas = [...document.querySelectorAll('.lista-locaciones li')];
      const limites = new maps.LatLngBounds();
      const mapa = new maps.Map(panel.querySelector('.mapa'), {
        center: { lat: 21.1619, lng: -86.8515 },
        zoom: 13,
        mapId: panel.dataset.mapId,
        streetViewControl: false,
        mapTypeControl: false,
      });
      const info = new maps.InfoWindow();
      for (const ficha of fichas) {
        const enlace = ficha.querySelector('a');
        const position = { lat: Number(ficha.dataset.lat), lng: Number(ficha.dataset.lng) };
        const marcador = new maps.marker.AdvancedMarkerElement({
          map: mapa, position, title: enlace.textContent, gmpClickable: true,
        });
        limites.extend(position);
        marcador.addEventListener('gmp-click', () => {
          const contenido = document.createElement('div');
          contenido.className = 'ficha-mapa';
          const titulo = document.createElement('strong');
          titulo.textContent = enlace.textContent;
          const descripcion = document.createElement('p');
          descripcion.textContent = ficha.querySelector('.resena').textContent;
          const consultar = enlace.cloneNode();
          consultar.textContent = 'Consultar archivo';
          contenido.append(titulo, descripcion, consultar);
          info.setContent(contenido);
          info.open({ map: mapa, anchor: marcador });
        });
      }
      panel.hidden = false;
      if (fichas.length > 1) mapa.fitBounds(limites, 72);
      else if (fichas.length === 1) mapa.setCenter(limites.getCenter());
      maps.event.addListenerOnce(mapa, 'tilesloaded', () => {
        if (cancelado) return;
        window.clearTimeout(espera);
        estado.hidden = true;
      });
    } catch {
      fallar();
    }
  };
  const parametros = new URLSearchParams({
    key: panel.dataset.key, callback: 'iniciarMapaNoventas', loading: 'async', v: 'weekly', libraries: 'marker',
    language: 'es', region: 'MX',
  });
  const script = document.createElement('script');
  script.src = `https://maps.googleapis.com/maps/api/js?${parametros}`;
  script.async = true;
  script.onerror = fallar;
  document.head.append(script);
}

prepararMapa();
