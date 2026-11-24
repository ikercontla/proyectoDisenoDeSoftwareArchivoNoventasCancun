package mx.archivonoventas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class AdjuntoTest {
    @Test
    void reconoceLasCuatroFirmasPorExtension() throws Exception {
        String[][] casos = {
            {"foto.JPG", "ffd8ff", "image/jpeg"},
            {"foto.png", "89504e470d0a1a0a", "image/png"},
            {"foto.webp", "524946460000000057454250", "image/webp"},
            {"ficha.pdf", "255044462d", "application/pdf"}
        };
        for (String[] caso : casos) {
            byte[] bytes = HexFormat.of().parseHex(caso[1]);
            var adjunto = Adjunto.leer(new MockMultipartFile("archivo", caso[0], "application/octet-stream", bytes));
            assertThat(adjunto.tipo()).isEqualTo(caso[2]);
            assertThat(adjunto.contenido()).isEqualTo(bytes);
        }
    }

    @Test
    void rechazaUnaFirmaDeFormatoDistinto() {
        byte[] pdf = HexFormat.of().parseHex("255044462d");
        var archivo = new MockMultipartFile("archivo", "foto.png", "image/png", pdf);
        assertThatThrownBy(() -> Adjunto.leer(archivo)).isInstanceOfSatisfying(
                ErrorAplicacion.class, error -> assertThat(error.estado()).isEqualTo(422));
    }
}
