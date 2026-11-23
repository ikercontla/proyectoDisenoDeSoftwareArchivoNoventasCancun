package mx.archivonoventas;

final class ErrorAplicacion extends RuntimeException {
    private final int estado;

    ErrorAplicacion(int estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    int estado() {
        return estado;
    }
}
