package mx.archivonoventas;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public record TextoVinculado(String texto, String destino) {
    private static final Pattern VINCULO = Pattern.compile(
            "\\[([^\\[\\]\\r\\n]{1,150})\\]\\(/(locaciones|documentos)/([1-9][0-9]{0,18})\\)");
    static final Pattern DESTINO = Pattern.compile("(locaciones|documentos)/([1-9][0-9]{0,18})");

    static List<TextoVinculado> separar(String descripcion) {
        var segmentos = new ArrayList<TextoVinculado>();
        var coincidencia = VINCULO.matcher(descripcion);
        int desde = 0;
        while (coincidencia.find()) {
            if (desde < coincidencia.start()) {
                segmentos.add(new TextoVinculado(descripcion.substring(desde, coincidencia.start()), null));
            }
            segmentos.add(new TextoVinculado(coincidencia.group(1), coincidencia.group(2) + "/" + coincidencia.group(3)));
            desde = coincidencia.end();
        }
        if (desde < descripcion.length()) segmentos.add(new TextoVinculado(descripcion.substring(desde), null));
        return List.copyOf(segmentos);
    }
}
