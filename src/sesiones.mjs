import { createHash, randomBytes, scrypt, timingSafeEqual } from 'node:crypto';
import { promisify } from 'node:util';

const derivar = promisify(scrypt);
const DURACION = 8 * 60 * 60;
const cookieNombre = 'archivo_sesion';
const hashToken = (token) => createHash('sha256').update(token).digest('hex');

function tokenDe(solicitud) {
  const valor = solicitud.headers.cookie?.match(/(?:^|;\s*)archivo_sesion=([a-f0-9]{64})(?=;|$)/)?.[1];
  return valor ? hashToken(valor) : null;
}

export async function crearAdministrador(db, entrada, contrasena) {
  const usuario = String(entrada ?? '').trim().toLowerCase();
  if (!/^[a-z0-9@._+-]{3,120}$/.test(usuario) || typeof contrasena !== 'string' ||
      contrasena.length < 12 || contrasena.length > 256) {
    throw new Error('ADMIN_USER debe tener de 3 a 120 caracteres y ADMIN_PASSWORD de 12 a 256.');
  }
  const salt = randomBytes(16).toString('hex');
  const hash = (await derivar(contrasena, salt, 64)).toString('hex');
  db.prepare('INSERT INTO administradores (usuario, salt, hash) VALUES (?, ?, ?)').run(usuario, salt, hash);
}

export async function autenticar(db, entrada, contrasena) {
  const usuario = String(entrada ?? '').trim().toLowerCase();
  const admin = db.prepare('SELECT * FROM administradores WHERE usuario = ?').get(usuario);
  const clave = String(contrasena ?? '');
  const calculado = await derivar(clave.length <= 256 ? clave : '', admin?.salt ?? '0'.repeat(32), 64);
  if (admin && clave.length <= 256 && timingSafeEqual(calculado, Buffer.from(admin.hash, 'hex'))) return usuario;
  if (admin) db.prepare('UPDATE administradores SET intentos_fallidos = intentos_fallidos + 1 WHERE usuario = ?').run(usuario);
  return null;
}

export function usuarioActivo(db, solicitud) {
  const hash = tokenDe(solicitud);
  if (!hash) return null;
  return db.prepare('SELECT usuario FROM sesiones WHERE token_hash = ? AND vence > ?')
    .get(hash, Math.floor(Date.now() / 1000))?.usuario ?? null;
}

export function cerrarSesion(db, solicitud) {
  const hash = tokenDe(solicitud);
  if (hash) db.prepare('DELETE FROM sesiones WHERE token_hash = ?').run(hash);
}

export function iniciarSesion(db, usuario) {
  const token = randomBytes(32).toString('hex');
  const ahora = Math.floor(Date.now() / 1000);
  db.prepare('DELETE FROM sesiones WHERE vence <= ?').run(ahora);
  db.prepare('INSERT INTO sesiones (token_hash, usuario, vence) VALUES (?, ?, ?)')
    .run(hashToken(token), usuario, ahora + DURACION);
  return token;
}

export function cookieSesion(token, segura) {
  return `${cookieNombre}=${token}; Max-Age=${token ? DURACION : 0}; Path=/; HttpOnly; SameSite=Strict${segura ? '; Secure' : ''}`;
}
