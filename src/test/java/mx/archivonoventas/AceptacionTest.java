package mx.archivonoventas;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "archivo.maps-key=clave-simulada")
@ExtendWith(OutputCaptureExtension.class)
class AceptacionTest {
    private static final String ADMIN = "/administracion";
    private static final String PANEL = ADMIN + "/propuestas";
    private static final String CLAVE = "Cuenta de prueba 1995";
    private static final String LIMITE_MULTIPART = "ArchivoNoventas-Prueba";
    private static final byte[] PDF = leerPdf();
    private static final HttpClient CLIENTE = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    @TempDir
    static Path carpeta;
    @LocalServerPort
    int puerto;
    @Autowired
    JdbcTemplate db;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registro) {
        registro.add("archivo.base-datos", () -> carpeta.resolve("aceptacion.sqlite").toString());
    }

    @BeforeAll
    static void preparar(@Autowired JdbcTemplate db, @Autowired Sesiones sesiones) {
        String sql = "INSERT INTO locaciones (id,nombre,descripcion,latitud,longitud,activa) VALUES (?,?,?,?,?,?)";
        db.update(sql, 1, "Locación pública", "Reseña histórica de prueba.", 21.16, -86.83, 1);
        db.update(sql, 2, "Segunda locación", "Otra reseña.", 21.19, -86.90, 1);
        db.update(sql, 3, "Locación inactiva", "Fuera del catálogo.", 21.20, -86.80, 0);
        db.update("INSERT INTO binarios (id,nombre,tipo,contenido) VALUES (1,?,?,?)", "ficha histórica.pdf", "application/pdf", PDF);
        db.update("INSERT INTO binarios (id,nombre,tipo) VALUES (2,?,?)", "ausente.pdf", "application/pdf");
        String publicar = "INSERT INTO documentos (id,locacion_id,binario_id,titulo,descripcion) VALUES (?,1,?,?,?)";
        db.update(publicar, 1, 1, "Un documento publicado", "Descripción de la ficha.");
        db.update(publicar, 2, 2, "Documento sin original", "Caso de error.");
        sesiones.crearAdministrador("curador", CLAVE);
        db.update("INSERT INTO administradores (usuario,salt,hash) VALUES (?,?,?)", "cuenta-anterior",
                "00112233445566778899aabbccddeeff",
                "726a4c4cd8f2c97ec440659dd73b820073a2e2172fa8ce919cc8b09fd3945f83bcd3d8a0a5717d6e0389b0a7b45fa9e385c50acfc2cf20fa07886f3c55bf1f9a");
    }

    @Test
    @DisplayName("V-01a · datos, coordenadas y vínculo al archivo para el marcador")
    void datosMapa() throws Exception {
        var r = get("/");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(texto(r)).contains("data-key=\"clave-simulada\"", "data-lat=\"21.16\"", "data-lng=\"-86.83\"",
                "href=\"/locaciones/1\"", "/mapa.js").doesNotContain("Locación inactiva");
        assertThat(texto(get("/mapa.js"))).contains("AdvancedMarkerElement", "gmp-click", "Consultar archivo");
    }

    @Test
    @DisplayName("V-01b · catálogo alternativo con coordenadas de las locaciones activas")
    void catalogoAlternativo() throws Exception {
        assertThat(texto(get("/"))).contains("21.16000, -86.83000", "21.19000, -86.90000", "data-estado-mapa")
                .doesNotContain("Locación inactiva");
        assertThat(texto(get("/mapa.js"))).contains("script.onerror = fallar", "panel.hidden = true");
    }

    @Test
    @DisplayName("V-02a · cada ficha muestra su reseña y documentos")
    void fichaDeLocacion() throws Exception {
        var r = get("/locaciones/1");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(texto(r)).contains("Reseña histórica de prueba.", "Un documento publicado", "/documentos/1/original");
        assertThat(texto(get("/locaciones/2"))).doesNotContain("Un documento publicado");
        assertThat(texto(get("/aportar?locacion=1"))).contains("value=\"1\" selected");
    }

    @Test
    @DisplayName("V-02b · locaciones inexistentes e inactivas devuelven 404")
    void locacionesAusentes() throws Exception {
        for (int id : new int[] {300, 3}) {
            var r = get("/locaciones/" + id);
            assertThat(r.statusCode()).isEqualTo(404);
            assertThat(texto(r)).contains("La locación no existe", "http-equiv=\"refresh\"", "5; url=/");
        }
    }

    @Test
    @DisplayName("V-03a · la descarga conserva nombre y bytes originales")
    void descargaOriginal() throws Exception {
        var r = get("/documentos/1/original");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(cabecera(r, "Content-Type")).isEqualTo("application/pdf");
        assertThat(cabecera(r, "Content-Disposition")).contains("attachment", "ficha%20hist%C3%B3rica.pdf");
        assertThat(r.body()).isEqualTo(PDF);
    }

    @Test
    @DisplayName("V-03b · un binario ausente devuelve el mensaje y se registra")
    void descargaAusente(CapturedOutput salida) throws Exception {
        var r = get("/documentos/2/original");
        assertThat(r.statusCode()).isEqualTo(404);
        assertThat(texto(r)).contains(PropuestaAporte.DESCARGA_NO_DISPONIBLE);
        assertThat(salida).contains("Original ausente para el documento 2");
    }

    @Test
    @DisplayName("V-04a · el aporte anónimo recibe folio y estado Pendiente")
    void aporteAnonimo() throws Exception {
        String folio = enviarAporte();
        assertThat(estado(folio)).isEqualTo("Pendiente");
        assertThat(texto(get("/aportar?folio=" + folio))).contains("Aporte recibido", folio);
    }

    @Test
    @DisplayName("V-04b · adjunto ausente, inválido o mayor de 25 MB")
    void adjuntoInvalido() throws Exception {
        int antes = db.queryForObject("SELECT COUNT(*) FROM propuestas", Integer.class);
        var ausente = multipart(null, new byte[0]);
        var zip = multipart("archivo.zip", PDF);
        var firma = multipart("archivo.pdf", new byte[] {1, 2, 3});
        var grande = multipart("archivo.pdf", new byte[25_000_001]);
        for (var r : List.of(ausente, zip, firma, grande)) {
            assertThat(r.statusCode()).isIn(422, 413);
            assertThat(texto(r)).contains(PropuestaAporte.ARCHIVO_INVALIDO);
        }
        assertThat(db.queryForObject("SELECT COUNT(*) FROM propuestas", Integer.class)).isEqualTo(antes);
    }

    @Test
    @DisplayName("V-05a · las credenciales válidas permiten abrir el panel")
    void autenticacionExitosa() throws Exception {
        var r = entrar("curador", CLAVE);
        assertThat(r.statusCode()).isEqualTo(303);
        assertThat(cabecera(r, "Location")).isEqualTo(PANEL);
        assertThat(cabecera(r, "Set-Cookie")).contains("HttpOnly", "SameSite=Strict");
        var panel = get(PANEL, cookie(r));
        assertThat(panel.statusCode()).isEqualTo(200);
        assertThat(texto(panel)).contains("Propuestas ciudadanas");
    }

    @Test
    @DisplayName("V-05b · se deniega el acceso y aumenta el contador de intentos")
    void autenticacionFallida() throws Exception {
        String sql = "SELECT intentos_fallidos FROM administradores WHERE usuario = 'curador'";
        int intentos = db.queryForObject(sql, Integer.class);
        var r = entrar("curador", "incorrecta");
        assertThat(r.statusCode()).isEqualTo(401);
        assertThat(r.headers().firstValue("Set-Cookie")).isEmpty();
        assertThat(texto(r)).contains("Credenciales no válidas. Verifique sus datos");
        assertThat(db.queryForObject(sql, Integer.class)).isEqualTo(intentos + 1);
        assertThat(cabecera(get(PANEL), "Location")).isEqualTo(ADMIN);
    }

    @Test
    @DisplayName("V-06a · aprobar una propuesta Revisada publica un solo documento")
    void aprobar() throws Exception {
        String folio = enviarAporte();
        String ruta = PANEL + "/" + folio;
        String token = iniciarSesion();
        String formulario = "titulo=" + encode("Publicación curada");
        assertThat(post(ruta + "/aprobar", formulario, token).statusCode()).isEqualTo(409);
        assertThat(post(ruta + "/revisar", "", token).statusCode()).isEqualTo(303);
        assertThat(estado(folio)).isEqualTo("Revisada");
        assertThat(texto(get(ruta, token))).contains("Aprobar propuesta", "Rechazar propuesta");
        assertThat(post(ruta + "/aprobar", formulario, token).statusCode()).isEqualTo(303);
        assertThat(estado(folio)).isEqualTo("Aprobada");
        assertThat(texto(get("/locaciones/1"))).contains("Publicación curada");
        assertThat(post(ruta + "/aprobar", formulario, token).statusCode()).isEqualTo(409);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM documentos WHERE propuesta_folio = ?", Integer.class, folio)).isEqualTo(1);
    }

    @Test
    @DisplayName("V-06b · se exige motivo para rechazar la propuesta")
    void rechazar() throws Exception {
        String folio = enviarAporte();
        String ruta = PANEL + "/" + folio;
        String token = iniciarSesion();
        assertThat(post(ruta + "/revisar", "", token).statusCode()).isEqualTo(303);
        var error = post(ruta + "/rechazar", "motivo=+", token);
        assertThat(error.statusCode()).isEqualTo(422);
        assertThat(texto(error)).contains(PropuestaAporte.MOTIVO_REQUERIDO);
        assertThat(estado(folio)).isEqualTo("Revisada");
        assertThat(post(ruta + "/rechazar", "motivo=Duplicado", token).statusCode()).isEqualTo(303);
        assertThat(estado(folio)).isEqualTo("Rechazada");
        assertThat(db.queryForObject("SELECT motivo FROM propuestas WHERE folio = ?", String.class, folio)).isEqualTo("Duplicado");
    }

    @Test
    @DisplayName("Compatibilidad · una cuenta creada por Node.js puede iniciar sesión")
    void credencialPrevia() throws Exception {
        assertThat(entrar("cuenta-anterior", "Contraseña previa 1995").statusCode()).isEqualTo(303);
    }

    private String estado(String folio) {
        return db.queryForObject("SELECT estado FROM propuestas WHERE folio = ?", String.class, folio);
    }

    private String enviarAporte() throws Exception {
        var r = multipart("aporte.pdf", PDF);
        assertThat(r.statusCode()).isEqualTo(303);
        return cabecera(r, "Location").substring("/aportar?folio=".length());
    }

    private String iniciarSesion() throws Exception {
        var r = entrar("curador", CLAVE);
        assertThat(r.statusCode()).isEqualTo(303);
        return cookie(r);
    }

    private HttpResponse<byte[]> entrar(String usuario, String clave) throws Exception {
        return post(ADMIN, "usuario=" + encode(usuario) + "&contrasena=" + encode(clave), null);
    }

    private HttpResponse<byte[]> get(String ruta) throws Exception {
        return get(ruta, null);
    }

    private HttpResponse<byte[]> get(String ruta, String sesion) throws Exception {
        return CLIENTE.send(solicitud(ruta, sesion).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private HttpResponse<byte[]> post(String ruta, String campos, String sesion) throws Exception {
        return CLIENTE.send(solicitud(ruta, sesion).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(campos)).build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private HttpResponse<byte[]> multipart(String nombre, byte[] contenido) throws Exception {
        var cuerpo = new ByteArrayOutputStream();
        for (String[] campo : new String[][] {{"locacion", "1"}, {"descripcion", "Aporte de prueba"}}) {
            cuerpo.writeBytes(("--" + LIMITE_MULTIPART + "\r\nContent-Disposition: form-data; name=\"" + campo[0] +
                    "\"\r\n\r\n" + campo[1] + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        if (nombre != null) {
            cuerpo.writeBytes(("--" + LIMITE_MULTIPART + "\r\nContent-Disposition: form-data; name=\"archivo\"; filename=\"" +
                    nombre + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            cuerpo.writeBytes(contenido);
            cuerpo.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        cuerpo.writeBytes(("--" + LIMITE_MULTIPART + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return CLIENTE.send(solicitud("/aportar", null)
                .header("Content-Type", "multipart/form-data; boundary=" + LIMITE_MULTIPART)
                .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo.toByteArray())).build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private HttpRequest.Builder solicitud(String ruta, String sesion) {
        var solicitud = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + puerto + ruta));
        if (sesion != null) solicitud.header("Cookie", sesion);
        return solicitud;
    }

    private static String texto(HttpResponse<byte[]> respuesta) {
        return new String(respuesta.body(), StandardCharsets.UTF_8);
    }

    private static String cabecera(HttpResponse<?> respuesta, String nombre) {
        return respuesta.headers().firstValue(nombre).orElseThrow();
    }

    private static String cookie(HttpResponse<?> respuesta) {
        return cabecera(respuesta, "Set-Cookie").split(";", 2)[0];
    }

    private static String encode(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }

    private static byte[] leerPdf() {
        try (var pdf = new ClassPathResource("ficha-de-demostracion.pdf").getInputStream()) {
            return pdf.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
