import { abrirAlmacen, transaccion } from './almacen.mjs';
import { Archivo, Documento } from './dominio.mjs';
import { crearAdministrador } from './sesiones.mjs';

export function fichaDemo() {
  const texto = 'BT /F1 18 Tf 60 740 Td (Archivo Noventas Cancun) Tj 0 -30 Td /F1 12 Tf (Documento de demostracion sin valor historico.) Tj ET\n';
  const objetos = [
    '<< /Type /Catalog /Pages 2 0 R >>',
    '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',
    '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
    `<< /Length ${texto.length} >>\nstream\n${texto}endstream`,
  ];
  let pdf = '%PDF-1.4\n';
  const offsets = objetos.map((objeto, i) => {
    const offset = String(pdf.length).padStart(10, '0');
    pdf += `${i + 1} 0 obj\n${objeto}\nendobj\n`;
    return offset;
  });
  const xref = pdf.length;
  pdf += `xref\n0 6\n0000000000 65535 f \n${offsets.map((o) => `${o} 00000 n \n`).join('')}trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`;
  return Buffer.from(pdf, 'ascii');
}

function cargarDemo(db) {
  transaccion(db, () => {
    if (db.prepare('SELECT COUNT(*) AS total FROM locaciones').get().total !== 0) return;
    const guardar = db.prepare('INSERT INTO locaciones (nombre, descripcion, latitud, longitud) VALUES (?, ?, ?, ?)');
    const { lastInsertRowid: id } = guardar.run('Parque de las Palapas',
      'Locación de demostración para consultar una ficha de ejemplo y descargar su original.', 21.16132, -86.82869);
    guardar.run('Palacio Municipal',
      'Locación de demostración para explorar el mapa y enviar un aporte ciudadano.', 21.16185, -86.82572);
    const nombre = 'ficha-de-demostracion.pdf';
    const binario = db.prepare('INSERT INTO binarios (nombre, tipo, contenido) VALUES (?, ?, ?)')
      .run(nombre, 'application/pdf', fichaDemo());
    const documento = new Documento({
      locacion_id: id, binario_id: binario.lastInsertRowid, titulo: 'Ficha de demostración',
      descripcion: 'Documento de muestra para verificar la consulta y descarga del archivo.',
      fecha_historica: null, tipo: 'application/pdf', nombre,
    });
    new Archivo(db, { id }).incorporar(documento, null);
  });
}

const comando = process.argv[2];
if (comando === 'demo' || comando === 'admin') {
  const db = abrirAlmacen();
  try {
    if (comando === 'demo') {
      cargarDemo(db);
      console.log('Datos de demostración disponibles.');
    } else {
      await crearAdministrador(db, process.env.ADMIN_USER, process.env.ADMIN_PASSWORD);
      console.log('Cuenta de administración creada.');
    }
  } finally {
    db.close();
  }
}
