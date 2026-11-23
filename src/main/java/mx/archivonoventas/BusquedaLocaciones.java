package mx.archivonoventas;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

record BusquedaLocaciones(List<Locacion> locaciones, String filtroActivo, boolean sinCoincidencias) {
    private static final Pattern ACENTOS = Pattern.compile("\\p{M}+");

    static BusquedaLocaciones buscar(List<Locacion> todas, String solicitud, String anterior) {
        List<Locacion> encontradas = filtrar(todas, solicitud);
        if (solicitud.isBlank() || !encontradas.isEmpty()) {
            return new BusquedaLocaciones(encontradas, solicitud.strip(), false);
        }
        List<Locacion> previas = filtrar(todas, anterior);
        return previas.isEmpty() ? new BusquedaLocaciones(todas, "", true)
                : new BusquedaLocaciones(previas, anterior.strip(), true);
    }

    private static List<Locacion> filtrar(List<Locacion> locaciones, String entrada) {
        String termino = normalizar(entrada.strip());
        if (termino.isEmpty()) return locaciones;
        return locaciones.stream().filter(l -> normalizar(l.nombre()).contains(termino) ||
                normalizar(l.descripcion()).contains(termino)).toList();
    }

    private static String normalizar(String texto) {
        String descompuesto = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return ACENTOS.matcher(descompuesto).replaceAll("");
    }
}
