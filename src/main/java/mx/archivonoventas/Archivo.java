package mx.archivonoventas;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Archivo {
    private static final String LOCACIONES = "SELECT * FROM locaciones WHERE activa = 1";
    private static final String DOCUMENTOS = """
            SELECT d.*, b.tipo, b.nombre FROM documentos d JOIN binarios b ON b.id = d.binario_id
            """;
    private final JdbcTemplate db;
    private final Originales originales;

    Archivo(JdbcTemplate db, Originales originales) {
        this.db = db;
        this.originales = originales;
    }

    List<Locacion> locaciones() {
        return db.query(LOCACIONES + " ORDER BY nombre", Archivo::leerLocacion);
    }

    Locacion locacion(long id) {
        return db.query(LOCACIONES + " AND id = ?", Archivo::leerLocacion, id).stream().findFirst().orElse(null);
    }

    List<Documento> documentos(long locacionId) {
        return db.query(DOCUMENTOS + "WHERE d.locacion_id = ? ORDER BY d.publicado DESC, d.id DESC",
                Documento::leer, locacionId);
    }

    Documento documentoPublicado(long id) {
        return db.query(DOCUMENTOS + "JOIN locaciones l ON l.id = d.locacion_id WHERE d.id = ? AND l.activa = 1",
                Documento::leer, id).stream().findFirst().orElse(null);
    }

    long incorporar(Documento documento, String folio) {
        documento.validarPublicacion();
        return db.queryForObject("""
                INSERT INTO documentos (locacion_id, binario_id, propuesta_folio, titulo, descripcion, fecha_historica)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, documento.locacionId(), documento.binarioId(), folio, documento.titulo(),
                documento.descripcion(), FechaHistorica.textoDe(documento.fechaHistorica()));
    }

    @Transactional
    public void cargarDemo() {
        if (db.queryForObject("SELECT COUNT(*) FROM locaciones", Integer.class) != 0) return;
        String insertar = "INSERT INTO locaciones (nombre, descripcion, latitud, longitud) VALUES (?, ?, ?, ?)";
        long id = db.queryForObject(insertar + " RETURNING id", Long.class, "Parque de las Palapas",
                "Locación de demostración para consultar una ficha de ejemplo y descargar su original.", 21.16132, -86.82869);
        long municipio = db.queryForObject(insertar + " RETURNING id", Long.class, "Palacio Municipal",
                "Locación de demostración para explorar el mapa y enviar un aporte ciudadano.", 21.16185, -86.82572);
        byte[] pdf;
        try (var fuente = new ClassPathResource("ficha-de-demostracion.pdf").getInputStream()) {
            pdf = fuente.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el PDF de demostración.", e);
        }
        String nombre = "ficha-de-demostracion.pdf";
        long binario = originales.guardar(nombre, "application/pdf", pdf);
        long primeraFicha = incorporar(new Documento(0, id, binario, "Ficha de demostración",
                "Documento de muestra. Explora también [Palacio Municipal](/locaciones/" + municipio + ").",
                FechaHistorica.desdeEntrada("1995"), "application/pdf", nombre), null);
        String nombreComplementario = "nota-de-demostracion.pdf";
        long complementario = originales.guardar(nombreComplementario, "application/pdf", pdf);
        incorporar(new Documento(0, id, complementario, "Nota de demostración",
                "Segundo documento de muestra para la cronología. Consulta la [ficha de demostración](/documentos/" +
                        primeraFicha + ").", FechaHistorica.desdeEntrada("1998-03"), "application/pdf", nombreComplementario), null);
    }

    private static Locacion leerLocacion(ResultSet fila, int indice) throws SQLException {
        return new Locacion(fila.getLong("id"), fila.getString("nombre"), fila.getString("descripcion"),
                fila.getDouble("latitud"), fila.getDouble("longitud"));
    }
}
