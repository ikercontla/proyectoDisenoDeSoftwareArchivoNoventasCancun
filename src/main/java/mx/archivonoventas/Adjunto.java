package mx.archivonoventas;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.web.multipart.MultipartFile;

record Adjunto(String nombre, String tipo, byte[] contenido) {
    static final int LIMITE = 25_000_000;
    static final String ARCHIVO_INVALIDO = "Debe adjuntar un archivo válido (.jpg, .png, .webp, .pdf de hasta 25 MB)";

    static Adjunto leer(MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty() || archivo.getSize() > LIMITE) {
            throw new ErrorAplicacion(422, ARCHIVO_INVALIDO);
        }
        String nombre = nombreSeguro(archivo.getOriginalFilename());
        byte[] datos = archivo.getBytes();
        return new Adjunto(nombre, tipoArchivo(nombre, datos), datos);
    }

    static String nombreSeguro(String candidato) {
        if (candidato == null) return "";
        String normal = candidato.replace('\\', '/');
        return normal.substring(normal.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "");
    }

    private static String tipoArchivo(String nombre, byte[] datos) {
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
