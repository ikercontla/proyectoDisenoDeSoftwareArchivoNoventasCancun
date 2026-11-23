const entidades = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
const e = (valor) => String(valor ?? '').replace(/[&<>"']/g, (caracter) => entidades[caracter]);
const aviso = (mensaje) => mensaje ? `<p class="aviso error" role="alert">${e(mensaje)}</p>` : '';
const panelRuta = '/administracion/propuestas';

function pagina(titulo, contenido, { seccion, volverAlMapa = false, usaMapa = false } = {}) {
  return `<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="description" content="Consulta el acervo por locación y aporta documentos a la memoria de Cancún.">
  ${volverAlMapa ? '<meta http-equiv="refresh" content="5; url=/">' : ''}
  <title>${e(titulo)} · Archivo Noventas Cancún</title>
  <link rel="stylesheet" href="/estilos.css">
  ${usaMapa ? '<script src="/mapa.js" defer></script>' : ''}
</head>
<body>
  <a class="saltar" href="#contenido">Ir al contenido</a>
  <header class="cabecera">
    <div class="contenedor barra">
      <a class="marca" href="/">Archivo <strong>Noventas</strong><span>Cancún</span></a>
      <nav aria-label="Navegación principal">
        <a href="/"${seccion === 'mapa' ? ' aria-current="page"' : ''}>Explorar</a>
        <a href="/aportar"${seccion === 'aportar' ? ' aria-current="page"' : ''}>Aportar</a>
        <a href="/administracion"${seccion === 'admin' ? ' aria-current="page"' : ''}>Administración</a>
      </nav>
    </div>
  </header>
  <main class="contenedor principal" id="contenido">${contenido}</main>
  <footer class="pie"><div class="contenedor">Archivo Noventas Cancún</div></footer>
</body>
</html>`;
}

function estado(valor) {
  return `<span class="estado ${e(valor.toLowerCase())}">${e(valor)}</span>`;
}

const enlaceMapa = '<a class="enlace-volver" href="/">Volver al mapa y catálogo</a>';

export const vistas = {
  inicio(locaciones, { mapsKey, mapsId }) {
    const tarjetas = locaciones.map(({ id, nombre, descripcion, latitud, longitud }) => `
      <li data-lat="${latitud}" data-lng="${longitud}">
        <a href="/locaciones/${id}">${e(nombre)}</a>
        <p class="resena">${e(descripcion)}</p>
        <p class="coordenadas">${latitud.toFixed(5)}, ${longitud.toFixed(5)}</p>
      </li>`).join('');
    return pagina('Explorar el archivo', `
      <header class="portada">
        <h1>La memoria de Cancún,<br> lugar por lugar.</h1>
        <p>Explora las locaciones y consulta los documentos que reúnen su historia.</p>
      </header>
      <section class="explorador" aria-label="Explorar por locación">
        <div class="mapa-panel" data-mapa data-key="${e(mapsKey)}" data-map-id="${e(mapsId)}" hidden>
          <div class="mapa" aria-label="Mapa interactivo de locaciones"></div>
        </div>
        <section class="catalogo" aria-labelledby="titulo-catalogo">
          <div class="catalogo-cabecera">
            <h2 id="titulo-catalogo">Locaciones</h2>
            <span class="cantidad">${locaciones.length}</span>
          </div>
          <p class="aviso" data-estado-mapa role="status">${mapsKey ? 'Cargando el mapa…' :
            'El mapa no está disponible. Puedes explorar las locaciones en el catálogo.'}</p>
          ${mapsKey ? '<noscript><p>Activa JavaScript para abrir el mapa interactivo.</p></noscript>' : ''}
          ${locaciones.length ? `<ul class="lista-locaciones">${tarjetas}</ul>` : '<p>Aún no hay locaciones registradas.</p>'}
        </section>
      </section>`, { seccion: 'mapa', usaMapa: Boolean(mapsKey) });
  },

  locacion(locacion, documentos) {
    const lista = documentos.map((doc) => `
      <li class="documento">
        <div>
          <h3>${e(doc.titulo)}</h3>
          <p>${e(doc.descripcion)}</p>
          ${doc.fechaHistorica ? `<p class="coordenadas">${e(doc.fechaHistorica)}</p>` : ''}
        </div>
        <a class="boton secundario" href="${doc.referenciaDescarga()}">Descargar ${e(doc.nombreOriginal)}</a>
      </li>`).join('');
    return pagina(locacion.nombre, `
      ${enlaceMapa}
      <header class="encabezado">
        <p class="coordenadas">${locacion.latitud.toFixed(5)}, ${locacion.longitud.toFixed(5)}</p>
        <h1>${e(locacion.nombre)}</h1>
        <p>${e(locacion.descripcion)}</p>
        <a class="boton secundario" href="/aportar?locacion=${locacion.id}">Aportar a esta locación</a>
      </header>
      <section aria-labelledby="documentos">
        <h2 id="documentos">Documentos del archivo</h2>
        ${documentos.length ? `<ul class="documentos">${lista}</ul>` : '<p>Esta locación aún no tiene documentos publicados.</p>'}
      </section>`);
  },

  locacionAusente() {
    return pagina('Locación inexistente', `
      <header class="encabezado compacto">
        <h1>La locación no existe</h1>
        <p>Volverás al mapa general en unos segundos.</p>
        ${enlaceMapa}
      </header>`, { volverAlMapa: true });
  },

  aporte(locaciones, { error, locacion, descripcion, fecha } = {}) {
    const opciones = locaciones.map(({ id, nombre }) =>
      `<option value="${id}"${String(id) === String(locacion) ? ' selected' : ''}>${e(nombre)}</option>`).join('');
    return pagina('Aportar al archivo', `
      <header class="encabezado compacto">
        <h1>Aportar al archivo</h1>
        <p>Envía un documento o una fotografía de una locación para que el equipo del archivo revise tu aporte.</p>
      </header>
      ${aviso(error)}
      ${locaciones.length ? `
        <form class="formulario" action="/aportar" method="post" enctype="multipart/form-data">
          <label>Locación <select name="locacion" required><option value="">Elige una locación</option>${opciones}</select></label>
          <label>Descripción <textarea name="descripcion" rows="4" maxlength="2000" required>${e(descripcion)}</textarea></label>
          <label>Fecha histórica (opcional)
            <input name="fecha" maxlength="10" value="${e(fecha)}" aria-describedby="fecha-ayuda">
            <small id="fecha-ayuda">Puedes indicar el año, el año y mes o la fecha completa (AAAA-MM-DD).</small>
          </label>
          <label>Archivo original
            <input name="archivo" type="file" accept=".jpg,.png,.webp,.pdf" aria-describedby="archivo-ayuda" required>
            <small id="archivo-ayuda">JPG, PNG, WebP o PDF de hasta 25 MB.</small>
          </label>
          <button type="submit">Enviar aporte</button>
        </form>` : '<p>Podrás aportar cuando haya una locación registrada.</p>'}`, { seccion: 'aportar' });
  },

  aporteRegistrado(folio) {
    return pagina('Aporte recibido', `
      <header class="encabezado compacto">
        <h1>Aporte recibido</h1>
        <p>Tu propuesta queda pendiente de revisión. Guarda este folio.</p>
        <p class="folio"><code>${e(folio)}</code></p>
        ${enlaceMapa}
      </header>`, { seccion: 'aportar' });
  },

  acceso(error, usuario) {
    return pagina('Acceso de administración', `
      <header class="encabezado compacto"><h1>Administración</h1><p>Ingresa para revisar los aportes ciudadanos.</p></header>
      ${aviso(error)}
      <form class="formulario" action="/administracion" method="post">
        <label>Usuario <input name="usuario" value="${e(usuario)}" autocomplete="username" maxlength="120" required></label>
        <label>Contraseña <input type="password" name="contrasena" autocomplete="current-password" maxlength="256" required></label>
        <button type="submit">Iniciar sesión</button>
      </form>`, { seccion: 'admin' });
  },

  panel(propuestas) {
    const filas = propuestas.map((p) => `<li class="documento">
      <div>
        <h2>${e(p.nombre)}</h2>
        <p>${e(p.locacion)} · ${e(p.recibida)} UTC</p>
        ${estado(p.estado)}
      </div>
      ${p.estado === 'Pendiente'
        ? `<form action="${panelRuta}/${p.folio}/revisar" method="post"><button type="submit">Abrir propuesta</button></form>`
        : `<a class="boton secundario" href="${panelRuta}/${p.folio}">Ver propuesta</a>`}
    </li>`).join('');
    return pagina('Propuestas ciudadanas', `
      <div class="encabezado encabezado-panel">
        <h1>Propuestas ciudadanas</h1>
        <form action="/administracion/salir" method="post"><button class="secundario" type="submit">Cerrar sesión</button></form>
      </div>
      ${propuestas.length ? `<ul class="documentos">${filas}</ul>` : '<p>Aún no se han recibido propuestas.</p>'}`, { seccion: 'admin' });
  },

  propuesta(p, error) {
    const ruta = `${panelRuta}/${p.folio}`;
    const revisar = `<form action="${ruta}/revisar" method="post"><button type="submit">Marcar en revisión</button></form>`;
    const dictaminar = `<div class="dictamenes">
      <form class="formulario" action="${ruta}/aprobar" method="post">
        <h2>Aprobar y publicar</h2>
        <label>Título de publicación <input name="titulo" maxlength="240" placeholder="Nombre del archivo por defecto"></label>
        <label>Fecha histórica ${p.tipo === 'application/pdf' ? '(opcional)' : ''}
          <input name="fecha" maxlength="10" value="${e(p.fecha_historica)}"${p.tipo === 'application/pdf' ? '' : ' required'}>
        </label>
        <button type="submit">Aprobar propuesta</button>
      </form>
      <form class="formulario" action="${ruta}/rechazar" method="post">
        <h2>Rechazar</h2>
        <label>Justificación <textarea name="motivo" rows="4" maxlength="2000" required></textarea></label>
        <button class="secundario" type="submit">Rechazar propuesta</button>
      </form>
    </div>`;
    return pagina(p.nombre, `
      <a class="enlace-volver" href="${panelRuta}">Volver a propuestas</a>
      <header class="encabezado compacto">
        ${estado(p.estado)}
        <h1>${e(p.nombre)}</h1>
        <p>${e(p.locacion)} · Folio <code>${e(p.folio)}</code></p>
        <p>${e(p.descripcion)}</p>
        <a class="boton secundario" href="${ruta}/original">Descargar original del aporte</a>
        ${p.motivo ? `<p class="aviso"><strong>Justificación</strong><br>${e(p.motivo)}</p>` : ''}
      </header>
      ${aviso(error)}
      ${p.estado === 'Pendiente' ? revisar : ''}
      ${p.estado === 'Revisada' ? dictaminar : ''}`, { seccion: 'admin' });
  },

  error(codigo, mensaje) {
    return pagina(`Error ${codigo}`, `<header class="encabezado compacto"><h1>${e(mensaje)}</h1>${enlaceMapa}</header>`);
  },
};
