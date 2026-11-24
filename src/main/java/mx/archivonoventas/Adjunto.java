package mx.archivonoventas;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;

record Adjunto(String nombre, String tipo, byte[] contenido) {
    private record FormatoOriginal(String extension, String etiqueta, String mime, Predicate<byte[]> firma) {}

    private static final List<FormatoOriginal> FORMATOS = List.of(
            new FormatoOriginal(".jpg", "JPG", "image/jpeg",
                    datos -> comienza(datos, 0, HexFormat.of().parseHex("ffd8ff"))),
            new FormatoOriginal(".png", "PNG", "image/png",
                    datos -> comienza(datos, 0, HexFormat.of().parseHex("89504e470d0a1a0a"))),
            new FormatoOriginal(".webp", "WebP", "image/webp",
                    datos -> comienza(datos, 0, ascii("RIFF")) && comienza(datos, 8, ascii("WEBP"))),
            new FormatoOriginal(".pdf", "PDF", "application/pdf", datos -> comienza(datos, 0, ascii("%PDF-"))));

    static final int LIMITE = 25_000_000;
    private static final String AYUDA_LIMITE = " de hasta 25 MB";
    static final String ARCHIVO_INVALIDO = "Debe adjuntar un archivo válido ("
            + extensionesAceptadas().replace(",", ", ") + AYUDA_LIMITE + ")";

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

    static String extensionesAceptadas() {
        return FORMATOS.stream().map(FormatoOriginal::extension).collect(Collectors.joining(","));
    }

    static String ayudaFormatos() {
        String etiquetas = FORMATOS.stream().map(FormatoOriginal::etiqueta).collect(Collectors.joining(", "));
        int ultimaComa = etiquetas.lastIndexOf(", ");
        if (ultimaComa >= 0) etiquetas = etiquetas.substring(0, ultimaComa) + " o " + etiquetas.substring(ultimaComa + 2);
        return etiquetas + AYUDA_LIMITE + ".";
    }

    private static String tipoArchivo(String nombre, byte[] datos) {
        int punto = nombre.lastIndexOf('.');
        if (punto < 1 || nombre.length() > 240 || datos.length == 0 || datos.length > LIMITE) {
            throw new ErrorAplicacion(422, ARCHIVO_INVALIDO);
        }
        String extension = nombre.substring(punto).toLowerCase(Locale.ROOT);
        for (FormatoOriginal formato : FORMATOS) {
            if (formato.extension().equals(extension) && formato.firma().test(datos)) return formato.mime();
        }
        throw new ErrorAplicacion(422, ARCHIVO_INVALIDO);
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
