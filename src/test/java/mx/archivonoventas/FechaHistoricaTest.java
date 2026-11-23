package mx.archivonoventas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FechaHistoricaTest {
    @Test
    void conservaLaPrecisionYSeparaElDiaDeOrden() {
        String[][] fechas = {{"1995", "1995-01-01"}, {"1998-03", "1998-03-01"}, {"2000-02-29", "2000-02-29"}};
        for (String[] datos : fechas) {
            FechaHistorica fecha = FechaHistorica.desdeEntrada(datos[0]);
            assertThat(FechaHistorica.textoDe(fecha)).isEqualTo(datos[0]);
            assertThat(fecha.diaDeOrden()).isEqualTo(datos[1]);
            assertThat(FechaHistorica.desdeRegistro(datos[0]).toString()).isEqualTo(datos[0]);
        }
        assertThat(FechaHistorica.desdeEntrada(" \t")).isNull();
        assertThat(FechaHistorica.textoDe(null)).isNull();
    }

    @Test
    void noAceptaFechasConDiasOMesesInvalidos() {
        for (String fecha : new String[] {"1999-02-29", "1999-13", "0000"}) {
            assertThatThrownBy(() -> FechaHistorica.desdeEntrada(fecha)).isInstanceOfSatisfying(
                    ErrorAplicacion.class, error -> assertThat(error.estado()).isEqualTo(422));
        }
    }
}
