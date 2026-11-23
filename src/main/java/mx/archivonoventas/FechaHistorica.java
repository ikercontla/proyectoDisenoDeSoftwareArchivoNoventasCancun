package mx.archivonoventas;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public final class FechaHistorica {
    private final String texto;

    private FechaHistorica(String texto) {
        this.texto = texto;
    }

    static FechaHistorica desdeEntrada(String entrada) {
        String fecha = entrada == null ? "" : entrada.strip();
        if (fecha.isEmpty()) return null;
        if (!fecha.startsWith("0000")) {
            try {
                if (fecha.matches("\\d{4}")) {
                    Year.parse(fecha);
                    return new FechaHistorica(fecha);
                }
                if (fecha.matches("\\d{4}-\\d{2}")) {
                    return new FechaHistorica(YearMonth.parse(fecha).toString());
                }
                if (fecha.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    return new FechaHistorica(LocalDate.parse(fecha).toString());
                }
            } catch (DateTimeParseException ignorado) {
                // La forma es válida, pero la fecha no existe en el calendario.
            }
        }
        throw new ErrorAplicacion(422, "Ingresa una fecha histórica válida en formato AAAA, AAAA-MM o AAAA-MM-DD.");
    }

    static FechaHistorica desdeRegistro(String texto) {
        return texto == null ? null : new FechaHistorica(texto);
    }

    static String textoDe(FechaHistorica fecha) {
        return fecha == null ? null : fecha.texto;
    }

    String diaDeOrden() {
        return switch (texto.length()) {
            case 4 -> texto + "-01-01";
            case 7 -> texto + "-01";
            default -> texto;
        };
    }

    @Override
    public String toString() {
        return texto;
    }
}
