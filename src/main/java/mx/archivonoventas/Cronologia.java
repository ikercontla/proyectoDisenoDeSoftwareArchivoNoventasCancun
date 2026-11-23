package mx.archivonoventas;

import java.util.Comparator;
import java.util.List;

final class Cronologia {
    private Cronologia() {}

    static List<Documento> de(List<Documento> documentos) {
        var ordenados = documentos.stream().filter(doc -> doc.fechaHistorica() != null)
                .sorted(Comparator.comparing(Documento::fechaOrden).thenComparingLong(Documento::id)).toList();
        if (ordenados.size() < 2 ||
                ordenados.get(0).fechaOrden().equals(ordenados.get(ordenados.size() - 1).fechaOrden())) {
            return List.of();
        }
        return ordenados;
    }
}
