package mx.archivonoventas;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
class PaquetesZip {
    private static final long LIMITE = 100_000_000;
    static final String MENSAJE_LIMITE =
            "El archivo de esta locación excede el límite de descarga en lote (100 MB). Realice descargas individuales";
    static final String MENSAJE_VACIO = "Esta locación aún no tiene documentos publicados.";
    private final JdbcTemplate db;
    private final DataSource origen;

    PaquetesZip(JdbcTemplate db, DataSource origen) {
        this.db = db;
        this.origen = origen;
    }

    record Entrada(long binario, String nombre, Long tamano) {}

    List<Entrada> preparar(long locacion) {
        List<Entrada> entradas = db.query("""
                SELECT b.id, b.nombre, length(b.contenido) AS tamano
                FROM documentos d JOIN binarios b ON b.id = d.binario_id
                WHERE d.locacion_id = ? ORDER BY d.id
                """, (f, i) -> {
            long tamano = f.getLong("tamano");
            return new Entrada(f.getLong("id"), f.getString("nombre"), f.getObject("tamano") == null ? null : tamano);
        }, locacion);
        if (entradas.isEmpty()) throw new ErrorAplicacion(422, MENSAJE_VACIO);
        long peso = 0;
        for (Entrada entrada : entradas) {
            if (entrada.tamano() == null) {
                throw new ErrorAplicacion(404, "Un documento de este archivo no está disponible temporalmente.");
            }
            peso += entrada.tamano();
            if (peso > LIMITE) throw new ErrorAplicacion(422, MENSAJE_LIMITE);
        }
        return entradas;
    }

    void escribir(List<Entrada> entradas, OutputStream destino) throws IOException {
        Set<String> usados = new HashSet<>();
        try (var conexion = origen.getConnection();
             var consulta = conexion.prepareStatement("SELECT contenido FROM binarios WHERE id = ?");
             var zip = new ZipOutputStream(destino)) {
            zip.setLevel(Deflater.BEST_SPEED);
            for (Entrada entrada : entradas) {
                consulta.setLong(1, entrada.binario());
                try (var fila = consulta.executeQuery()) {
                    if (!fila.next()) throw new IOException("Original ausente al generar el ZIP.");
                    try (var original = fila.getBinaryStream(1)) {
                        if (original == null) throw new IOException("Original ausente al generar el ZIP.");
                        zip.putNextEntry(new ZipEntry(nombreDisponible(entrada.nombre(), usados)));
                        original.transferTo(zip);
                        zip.closeEntry();
                    }
                }
            }
        } catch (SQLException e) {
            throw new IOException("No se pudo leer un original del archivo.", e);
        }
    }

    private static String nombreDisponible(String original, Set<String> usados) {
        String nombre = Adjunto.nombreSeguro(original).replaceAll("[:*?\"<>|]", "_");
        if (nombre.isBlank() || nombre.equals(".") || nombre.equals("..")) nombre = "documento";
        int punto = nombre.lastIndexOf('.');
        String base = punto > 0 ? nombre.substring(0, punto) : nombre;
        String extension = punto > 0 ? nombre.substring(punto) : "";
        String candidato = nombre;
        for (int sufijo = 2; !usados.add(candidato.toLowerCase(Locale.ROOT)); sufijo++) {
            candidato = base + " (" + sufijo + ")" + extension;
        }
        return candidato;
    }
}
