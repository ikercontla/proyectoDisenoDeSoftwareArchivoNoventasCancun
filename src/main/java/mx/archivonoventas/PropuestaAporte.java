package mx.archivonoventas;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Locale;

public record PropuestaAporte(String folio, long locacionId, long binarioId, String descripcion,
                             String fechaHistorica, String estado, String motivo, String recibida,
                             String nombre, String tipo, String locacion) {
    static final int LIMITE = 25_000_000;
    static final String ARCHIVO_INVALIDO = "Debe adjuntar un archivo válido (.jpg, .png, .webp, .pdf de hasta 25 MB)";
    static final String MOTIVO_REQUERIDO = "Debe ingresar obligatoriamente una justificación para rechazar la propuesta";
    static final String DESCARGA_NO_DISPONIBLE = "El documento solicitado no está disponible temporalmente para descarga";

    static PropuestaAporte leer(ResultSet f, int indice) throws SQLException {
        return new PropuestaAporte(f.getString("folio"), f.getLong("locacion_id"), f.getLong("binario_id"),
                f.getString("descripcion"), f.getString("fecha_historica"), f.getString("estado"),
                f.getString("motivo"), f.getString("recibida"), f.getString("nombre"), f.getString("tipo"),
                f.getString("locacion"));
    }

    static String nombreSeguro(String candidato) {
        if (candidato == null) return "";
        String normal = candidato.replace('\\', '/');
        return normal.substring(normal.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "");
    }

    static String tipoArchivo(String nombre, byte[] datos) {
        int punto = nombre.lastIndexOf('.');
        if (punto < 1 || nombre.length() > 240 || datos.length == 0 || datos.length > LIMITE) {
            throw new ErrorAplicacion(422, ARCHIVO_INVALIDO);
        }
        String extension = nombre.substring(punto).toLowerCase(Locale.ROOT);
        String tipo = switch (extension) {
            case ".jpg" -> comienza(datos, 0, HexFormat.of().parseHex("ffd8ff")) ? "image/jpeg" : null;
            case ".png" -> comienza(datos, 0, HexFormat.of().parseHex("89504e470d0a1a0a")) ? "image/png" : null;
            case ".webp" -> comienza(datos, 0, ascii("RIFF")) && comienza(datos, 8, ascii("WEBP")) ? "image/webp" : null;
            case ".pdf" -> comienza(datos, 0, ascii("%PDF-")) ? "application/pdf" : null;
            default -> null;
        };
        if (tipo == null) throw new ErrorAplicacion(422, ARCHIVO_INVALIDO);
        return tipo;
    }

    static String normalizarFecha(String entrada) {
        String fecha = entrada == null ? "" : entrada.strip();
        if (fecha.isEmpty()) return null;
        if (!fecha.startsWith("0000")) {
            try {
                if (fecha.matches("\\d{4}")) {
                    Year.parse(fecha);
                    return fecha;
                }
                if (fecha.matches("\\d{4}-\\d{2}")) return YearMonth.parse(fecha).toString();
                if (fecha.matches("\\d{4}-\\d{2}-\\d{2}")) return LocalDate.parse(fecha).toString();
            } catch (DateTimeParseException ignorado) {
                // La forma es válida, pero la fecha no existe en el calendario.
            }
        }
        throw new ErrorAplicacion(422, "Ingresa una fecha histórica válida en formato AAAA, AAAA-MM o AAAA-MM-DD.");
    }

    Documento paraPublicar(String tituloPropuesto, String fechaPropuesta) {
        String titulo = tituloPropuesto.strip();
        if (titulo.isEmpty()) titulo = nombre.substring(0, nombre.lastIndexOf('.'));
        if (titulo.length() > 240) throw new ErrorAplicacion(422, "El título puede tener hasta 240 caracteres.");
        String fecha = normalizarFecha(fechaPropuesta);
        return new Documento(0, locacionId, binarioId, titulo, descripcion,
                fecha == null ? fechaHistorica : fecha, tipo, nombre);
    }

    private static byte[] ascii(String firma) {
        return firma.getBytes(StandardCharsets.US_ASCII);
    }

    private static boolean comienza(byte[] datos, int inicio, byte[] firma) {
        if (datos.length < inicio + firma.length) return false;
        for (int i = 0; i < firma.length; i++) {
            if (datos[inicio + i] != firma[i]) return false;
        }
        return true;
    }
}
