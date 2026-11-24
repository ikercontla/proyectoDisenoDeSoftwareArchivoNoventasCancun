package mx.archivonoventas;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;

@Service
class PaquetesZip {
    private static final long LIMITE = 100_000_000;
    static final String MENSAJE_LIMITE =
            "El archivo de esta locación excede el límite de descarga en lote (100 MB). Realice descargas individuales";
    static final String MENSAJE_VACIO = "Esta locación aún no tiene documentos publicados.";
    private final Originales originales;

    PaquetesZip(Originales originales) {
        this.originales = originales;
    }

    List<Originales.Entrada> preparar(long locacion) {
        var entradas = originales.entradasParaLocacion(locacion);
        if (entradas.isEmpty()) throw new ErrorAplicacion(422, MENSAJE_VACIO);
        long peso = 0;
        for (Originales.Entrada entrada : entradas) {
            if (entrada.tamano() == null) {
                throw new ErrorAplicacion(404, "Un documento de este archivo no está disponible temporalmente.");
            }
            peso += entrada.tamano();
            if (peso > LIMITE) throw new ErrorAplicacion(422, MENSAJE_LIMITE);
        }
        return entradas;
    }

    void escribir(List<Originales.Entrada> entradas, OutputStream destino) throws IOException {
        Set<String> usados = new HashSet<>();
        originales.conLote(lote -> {
            try (var zip = new ZipOutputStream(destino)) {
                zip.setLevel(Deflater.BEST_SPEED);
                for (Originales.Entrada entrada : entradas) {
                    lote.leer(entrada.binario(), original -> {
                        zip.putNextEntry(new ZipEntry(nombreDisponible(entrada.nombre(), usados)));
                        original.transferTo(zip);
                        zip.closeEntry();
                    });
                }
            }
        });
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
