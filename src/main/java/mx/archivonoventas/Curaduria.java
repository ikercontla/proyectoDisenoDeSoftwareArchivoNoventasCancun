package mx.archivonoventas;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class Curaduria {
    private final JdbcTemplate db;
    private final Archivo archivo;

    Curaduria(JdbcTemplate db, Archivo archivo) {
        this.db = db;
        this.archivo = archivo;
    }

    @Transactional
    public long registrarLocacion(String nombreEntrada, String descripcionEntrada, String latitud, String longitud) {
        Map<String, String> errores = new LinkedHashMap<>();
        String nombre = texto(nombreEntrada, "nombre", "Escribe un nombre de hasta 160 caracteres.", 160, errores);
        String descripcion = texto(descripcionEntrada, "descripcion", "Escribe una descripción de hasta 2000 caracteres.", 2000, errores);
        double lat = coordenada(latitud, 90, "latitud", errores);
        double lng = coordenada(longitud, 180, "longitud", errores);
        if (!errores.containsKey("nombre")) {
            String comparacion = nombre.toLowerCase(Locale.ROOT);
            if (db.queryForList("SELECT nombre FROM locaciones", String.class).stream()
                    .anyMatch(actual -> actual.toLowerCase(Locale.ROOT).equals(comparacion))) {
                errores.put("nombre", "Ya existe una locación con ese nombre.");
            }
        }
        confirmar(errores);
        return db.queryForObject("""
                INSERT INTO locaciones (nombre, descripcion, latitud, longitud) VALUES (?, ?, ?, ?) RETURNING id
                """, Long.class, nombre, descripcion, lat, lng);
    }

    @Transactional(rollbackFor = IOException.class)
    public long publicar(String locacionEntrada, String tituloEntrada, String fechaEntrada,
                         String descripcionEntrada, MultipartFile archivoEntrada) throws IOException {
        Map<String, String> errores = new LinkedHashMap<>();
        String titulo = texto(tituloEntrada, "titulo", "Escribe un título de hasta 240 caracteres.", 240, errores);
        String descripcion = texto(descripcionEntrada, "descripcion", "Escribe una descripción de hasta 2000 caracteres.", 2000, errores);
        long locacion = id(locacionEntrada);
        if (archivo.locacion(locacion) == null) errores.put("locacion", "Selecciona una locación activa.");
        FechaHistorica fecha = null;
        try {
            fecha = FechaHistorica.desdeEntrada(fechaEntrada);
            if (fecha == null) errores.put("fecha", "Indica el año de procedencia.");
        } catch (ErrorAplicacion error) {
            errores.put("fecha", error.getMessage());
        }
        Adjunto adjunto = null;
        try {
            adjunto = Adjunto.leer(archivoEntrada);
        } catch (ErrorAplicacion error) {
            errores.put("archivo", error.getMessage());
        }
        confirmar(errores);
        long binario = archivo.guardarOriginal(adjunto.nombre(), adjunto.tipo(), adjunto.contenido());
        return archivo.incorporar(new Documento(0, locacion, binario, titulo, descripcion, fecha,
                adjunto.tipo(), adjunto.nombre()), null);
    }

    private static long id(String valor) {
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double coordenada(String valor, int limite, String campo, Map<String, String> errores) {
        String limpio = valor.strip();
        if (limpio.matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)")) {
            double numero = Double.parseDouble(limpio);
            if (numero >= -limite && numero <= limite) return numero;
        }
        errores.put(campo, "Ingresa un valor entre " + -limite + " y " + limite + ".");
        return 0;
    }

    private static String texto(String valor, String campo, String mensaje, int maximo, Map<String, String> errores) {
        String limpio = valor.strip();
        if (limpio.isEmpty() || limpio.length() > maximo) errores.put(campo, mensaje);
        return limpio;
    }

    private static void confirmar(Map<String, String> errores) {
        if (!errores.isEmpty()) throw new CamposInvalidos(errores);
    }
}
