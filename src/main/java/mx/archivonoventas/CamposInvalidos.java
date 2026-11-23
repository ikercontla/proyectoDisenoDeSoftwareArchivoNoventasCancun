package mx.archivonoventas;

import java.util.Map;

final class CamposInvalidos extends RuntimeException {
    private final Map<String, String> campos;

    CamposInvalidos(Map<String, String> campos) {
        super("Revisa los campos indicados.");
        this.campos = Map.copyOf(campos);
    }

    Map<String, String> campos() {
        return campos;
    }
}
