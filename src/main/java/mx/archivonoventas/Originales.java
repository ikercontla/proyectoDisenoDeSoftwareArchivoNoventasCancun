package mx.archivonoventas;

import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class Originales {
    record Original(String nombre, String tipo, byte[] contenido) {}
    record Entrada(long binario, String nombre, Long tamano) {}

    @FunctionalInterface
    interface Receptor {
        void leer(InputStream original) throws IOException;
    }

    @FunctionalInterface
    interface LectorLote {
        void leer(long id, Receptor receptor) throws IOException;
    }

    @FunctionalInterface
    interface OperacionLote {
        void ejecutar(LectorLote lote) throws IOException;
    }

    private final JdbcTemplate db;
    private final DataSource origen;

    Originales(JdbcTemplate db, DataSource origen) {
        this.db = db;
        this.origen = origen;
    }

    long guardar(String nombre, String tipo, byte[] contenido) {
        return db.queryForObject("INSERT INTO binarios (nombre, tipo, contenido) VALUES (?, ?, ?) RETURNING id",
                Long.class, nombre, tipo, contenido);
    }

    Original buscar(long id) {
        return db.query("SELECT nombre, tipo, contenido FROM binarios WHERE id = ? AND contenido IS NOT NULL",
                (fila, i) -> new Original(fila.getString("nombre"), fila.getString("tipo"), fila.getBytes("contenido")),
                id).stream().findFirst().orElse(null);
    }

    boolean disponible(long id) {
        return Boolean.TRUE.equals(db.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM binarios WHERE id = ? AND contenido IS NOT NULL)", Boolean.class, id));
    }

    List<Entrada> entradasParaLocacion(long locacion) {
        return db.query("""
                SELECT b.id, b.nombre, length(b.contenido) AS tamano
                FROM documentos d JOIN binarios b ON b.id = d.binario_id
                WHERE d.locacion_id = ? ORDER BY d.id
                """, (fila, i) -> {
            long bytes = fila.getLong("tamano");
            Long tamano = fila.wasNull() ? null : bytes;
            return new Entrada(fila.getLong("id"), fila.getString("nombre"), tamano);
        }, locacion);
    }

    void conLote(OperacionLote operacion) throws IOException {
        try (var conexion = origen.getConnection();
             var consulta = conexion.prepareStatement("SELECT contenido FROM binarios WHERE id = ?")) {
            operacion.ejecutar((id, receptor) -> leerOriginal(consulta, id, receptor));
        } catch (SQLException e) {
            throw errorDeLectura(e);
        }
    }

    private static void leerOriginal(PreparedStatement consulta, long id, Receptor receptor) throws IOException {
        try {
            consulta.setLong(1, id);
            try (var fila = consulta.executeQuery()) {
                if (!fila.next()) throw new IOException("Original ausente al generar el ZIP.");
                try (var original = fila.getBinaryStream(1)) {
                    if (original == null) throw new IOException("Original ausente al generar el ZIP.");
                    receptor.leer(original);
                }
            }
        } catch (SQLException e) {
            throw errorDeLectura(e);
        }
    }

    private static IOException errorDeLectura(SQLException causa) {
        return new IOException("No se pudo leer un original del archivo.", causa);
    }
}
