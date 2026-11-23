package mx.archivonoventas;

import java.sql.ResultSet;
import java.sql.SQLException;

public record PropuestaAporte(String folio, long locacionId, long binarioId, String descripcion,
                             FechaHistorica fechaHistorica, String estado, String motivo, String recibida,
                             String nombre, String tipo, String locacion) {
    static final String MOTIVO_REQUERIDO = "Debe ingresar obligatoriamente una justificación para rechazar la propuesta";
    static final String DESCARGA_NO_DISPONIBLE = "El documento solicitado no está disponible temporalmente para descarga";

    static PropuestaAporte leer(ResultSet f, int indice) throws SQLException {
        return new PropuestaAporte(f.getString("folio"), f.getLong("locacion_id"), f.getLong("binario_id"),
                f.getString("descripcion"), FechaHistorica.desdeRegistro(f.getString("fecha_historica")), f.getString("estado"),
                f.getString("motivo"), f.getString("recibida"), f.getString("nombre"), f.getString("tipo"),
                f.getString("locacion"));
    }

    Documento paraPublicar(String tituloPropuesto, String fechaPropuesta) {
        String titulo = tituloPropuesto.strip();
        if (titulo.isEmpty()) titulo = nombre.substring(0, nombre.lastIndexOf('.'));
        if (titulo.length() > 240) throw new ErrorAplicacion(422, "El título puede tener hasta 240 caracteres.");
        FechaHistorica fecha = FechaHistorica.desdeEntrada(fechaPropuesta);
        return new Documento(0, locacionId, binarioId, titulo, descripcion,
                fecha == null ? fechaHistorica : fecha, tipo, nombre);
    }
}
