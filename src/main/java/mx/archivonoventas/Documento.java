package mx.archivonoventas;

import java.sql.ResultSet;
import java.sql.SQLException;

public record Documento(long id, long locacionId, long binarioId, String titulo, String descripcion,
                        String fechaHistorica, String tipo, String nombreOriginal) {
    static Documento leer(ResultSet fila, int indice) throws SQLException {
        return new Documento(fila.getLong("id"), fila.getLong("locacion_id"), fila.getLong("binario_id"),
                fila.getString("titulo"), fila.getString("descripcion"), fila.getString("fecha_historica"),
                fila.getString("tipo"), fila.getString("nombre"));
    }

    void validarPublicacion() {
        if (titulo.isBlank() || (!tipo.equals("application/pdf") && (fechaHistorica == null || descripcion.isBlank()))) {
            throw new ErrorAplicacion(422, "Completa el título y, para una imagen, la fecha histórica y la descripción.");
        }
    }

    public String referenciaDescarga() {
        return "/documentos/" + id + "/original";
    }
}
