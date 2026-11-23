package mx.archivonoventas;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

record Adjunto(String nombre, String tipo, byte[] contenido) {
    static Adjunto leer(MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty() || archivo.getSize() > PropuestaAporte.LIMITE) {
            throw new ErrorAplicacion(422, PropuestaAporte.ARCHIVO_INVALIDO);
        }
        String nombre = PropuestaAporte.nombreSeguro(archivo.getOriginalFilename());
        byte[] datos = archivo.getBytes();
        return new Adjunto(nombre, PropuestaAporte.tipoArchivo(nombre, datos), datos);
    }
}
