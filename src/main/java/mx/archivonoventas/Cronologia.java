package mx.archivonoventas;

import java.util.Comparator;
import java.util.List;

final class Cronologia {
    private Cronologia() {}

    static List<Documento> de(List<Documento> documentos) {
        var ordenados = documentos.stream().filter(doc -> doc.fechaHistorica() != null)
                .sorted(Comparator.comparing(Cronologia::diaDeOrden).thenComparingLong(Documento::id)).toList();
        if (ordenados.size() < 2 ||
                diaDeOrden(ordenados.get(0)).equals(diaDeOrden(ordenados.get(ordenados.size() - 1)))) {
            return List.of();
        }
        return ordenados;
    }

    private static String diaDeOrden(Documento documento) {
        return documento.fechaHistorica().diaDeOrden();
    }
}
