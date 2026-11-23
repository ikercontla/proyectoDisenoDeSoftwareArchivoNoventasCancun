package mx.archivonoventas;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import org.bouncycastle.crypto.generators.SCrypt;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Sesiones {
    static final String COOKIE = "archivo_sesion";
    static final int DURACION = 8 * 60 * 60;
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();
    private final JdbcTemplate db;

    Sesiones(JdbcTemplate db) {
        this.db = db;
    }

    private record Credencial(String salt, String hash) {}

    void crearAdministrador(String entrada, String contrasena) {
        String usuario = normalizarUsuario(entrada);
        if (!usuario.matches("[a-z0-9@._+\\-]{3,120}") || contrasena == null ||
                contrasena.length() < 12 || contrasena.length() > 256) {
            throw new IllegalArgumentException("ADMIN_USER debe tener de 3 a 120 caracteres y ADMIN_PASSWORD de 12 a 256.");
        }
        String salt = aleatorio(16);
        db.update("INSERT INTO administradores (usuario, salt, hash) VALUES (?, ?, ?)",
                usuario, salt, HEX.formatHex(derivar(contrasena, salt)));
    }

    String autenticar(String entrada, String contrasena) {
        String usuario = normalizarUsuario(entrada);
        Credencial registro = db.query("SELECT salt, hash FROM administradores WHERE usuario = ?",
                (f, i) -> new Credencial(f.getString("salt"), f.getString("hash")), usuario)
                .stream().findFirst().orElse(null);
        boolean longitudValida = contrasena.length() <= 256;
        byte[] hash = derivar(longitudValida ? contrasena : "", registro == null ? "0".repeat(32) : registro.salt());
        if (registro == null) return null;
        if (longitudValida && MessageDigest.isEqual(hash, HEX.parseHex(registro.hash()))) return usuario;
        db.update("UPDATE administradores SET intentos_fallidos = intentos_fallidos + 1 WHERE usuario = ?", usuario);
        return null;
    }

    @Transactional
    public String iniciar(String usuario, String anterior) {
        cerrar(anterior);
        long ahora = Instant.now().getEpochSecond();
        db.update("DELETE FROM sesiones WHERE vence <= ?", ahora);
        String token = aleatorio(32);
        db.update("INSERT INTO sesiones (token_hash, usuario, vence) VALUES (?, ?, ?)",
                hashToken(token), usuario, ahora + DURACION);
        return token;
    }

    String usuarioActivo(String token) {
        if (!tokenValido(token)) return null;
        return db.query("SELECT usuario FROM sesiones WHERE token_hash = ? AND vence > ?",
                (f, i) -> f.getString("usuario"), hashToken(token), Instant.now().getEpochSecond())
                .stream().findFirst().orElse(null);
    }

    void cerrar(String token) {
        if (tokenValido(token)) db.update("DELETE FROM sesiones WHERE token_hash = ?", hashToken(token));
    }

    private static String normalizarUsuario(String entrada) {
        return entrada == null ? "" : entrada.strip().toLowerCase(Locale.ROOT);
    }

    private static boolean tokenValido(String token) {
        return token != null && token.matches("[a-f0-9]{64}");
    }

    private static byte[] derivar(String texto, String salt) {
        // Conserva los parámetros y la codificación de las cuentas ya existentes.
        return SCrypt.generate(texto.getBytes(StandardCharsets.UTF_8), salt.getBytes(StandardCharsets.UTF_8),
                16_384, 8, 1, 64);
    }

    private static String aleatorio(int longitud) {
        byte[] bytes = new byte[longitud];
        ALEATORIO.nextBytes(bytes);
        return HEX.formatHex(bytes);
    }

    private static String hashToken(String token) {
        try {
            return HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
