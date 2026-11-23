package mx.archivonoventas;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class Propuestas {
    private static final String CONSULTA = """
            SELECT p.*, b.tipo, b.nombre, l.nombre AS locacion FROM propuestas p
            JOIN binarios b ON b.id = p.binario_id JOIN locaciones l ON l.id = p.locacion_id
            """;
    private final JdbcTemplate db;
    private final Archivo archivo;

    Propuestas(JdbcTemplate db, Archivo archivo) {
        this.db = db;
        this.archivo = archivo;
    }

    List<PropuestaAporte> listar() {
        return db.query(CONSULTA + "ORDER BY p.recibida DESC, p.rowid DESC", PropuestaAporte::leer);
    }

    PropuestaAporte buscar(String folio) {
        return db.query(CONSULTA + "WHERE p.folio = ?", PropuestaAporte::leer, folio).stream().findFirst()
                .orElseThrow(() -> new ErrorAplicacion(404, "No se encontró la propuesta."));
    }

    boolean existe(String folio) {
        return db.queryForObject("SELECT EXISTS(SELECT 1 FROM propuestas WHERE folio = ?)", Boolean.class, folio);
    }

    @Transactional(rollbackFor = IOException.class)
    public String recibir(long locacion, String descripcion, String fecha, MultipartFile adjunto) throws IOException {
        Adjunto original = Adjunto.leer(adjunto);
        String resena = descripcion.strip();
        if (archivo.locacion(locacion) == null || resena.isEmpty() || resena.length() > 2000) {
            throw new ErrorAplicacion(422, "Selecciona una locación y escribe una descripción de hasta 2000 caracteres.");
        }
        FechaHistorica fechaHistorica = FechaHistorica.desdeEntrada(fecha);
        String folio = UUID.randomUUID().toString();
        long binario = archivo.guardarOriginal(original.nombre(), original.tipo(), original.contenido());
        db.update("""
                INSERT INTO propuestas (folio, locacion_id, binario_id, descripcion, fecha_historica)
                VALUES (?, ?, ?, ?, ?)
                """, folio, locacion, binario, resena, FechaHistorica.textoDe(fechaHistorica));
        return folio;
    }

    void revisar(String folio, String usuario) {
        buscar(folio);
        db.update("""
                UPDATE propuestas SET estado = 'Revisada', curador = ? WHERE folio = ? AND estado = 'Pendiente'
                """, usuario, folio);
    }

    @Transactional
    public void aprobar(String folio, String usuario, String titulo, String fecha) {
        PropuestaAporte propuesta = buscar(folio);
        if (archivo.locacion(propuesta.locacionId()) == null) {
            throw new ErrorAplicacion(422, "La locación de esta propuesta ya no está activa.");
        }
        if (!archivo.tieneOriginal(propuesta.binarioId())) {
            throw new ErrorAplicacion(422, "El original del aporte no está disponible.");
        }
        Documento documento = propuesta.paraPublicar(titulo, fecha);
        dictaminar(folio, usuario, "Aprobada", null);
        archivo.incorporar(documento, folio);
        db.update("UPDATE propuestas SET fecha_historica = ? WHERE folio = ?",
                FechaHistorica.textoDe(documento.fechaHistorica()), folio);
    }

    void rechazar(String folio, String usuario, String motivo) {
        buscar(folio);
        String limpio = motivo.strip();
        if (limpio.isEmpty()) throw new ErrorAplicacion(422, PropuestaAporte.MOTIVO_REQUERIDO);
        if (limpio.length() > 2000) throw new ErrorAplicacion(422, "El motivo puede tener hasta 2000 caracteres.");
        dictaminar(folio, usuario, "Rechazada", limpio);
    }

    private void dictaminar(String folio, String usuario, String estado, String motivo) {
        int cambios = db.update("""
                UPDATE propuestas SET estado = ?, motivo = ?, curador = ?, dictaminada = CURRENT_TIMESTAMP
                WHERE folio = ? AND estado = 'Revisada'
                """, estado, motivo, usuario, folio);
        if (cambios != 1) throw new ErrorAplicacion(409, "Solo se pueden dictaminar propuestas en revisión.");
    }
}
