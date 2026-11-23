package mx.archivonoventas;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import java.util.Map;
import java.util.zip.ZipInputStream;
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
    private static final String ALTA_LOCACION = ADMIN + "/locaciones/nueva";
    private static final String PUBLICAR_DOCUMENTO = ADMIN + "/documentos/nuevo";
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

    @Test
    @DisplayName("V-07a · la cronología ordena fechas parciales y completas")
    void lineaDelTiempo() throws Exception {
        long locacion = locacionPrueba("Cronología de prueba");
        documentoPrueba(locacion, "Segundo hito", "1999-02", "segundo.pdf", "Texto.");
        documentoPrueba(locacion, "Primer hito", "1995", "primero.pdf", "Texto.");
        documentoPrueba(locacion, "Sin fecha visible en el archivo", null, "sin-fecha.pdf", "Texto.");
        String pagina = texto(get("/locaciones/" + locacion));
        assertThat(pagina).contains("Ver línea del tiempo", "id=\"cronologia\"");
        String linea = pagina.substring(pagina.indexOf("id=\"cronologia\""), pagina.indexOf("aria-labelledby=\"documentos\""));
        assertThat(linea).contains("Primer hito", "Segundo hito", "1995", "1999-02").doesNotContain("Sin fecha");
        assertThat(linea.indexOf("Primer hito")).isLessThan(linea.indexOf("Segundo hito"));
    }

    @Test
    @DisplayName("V-07b · la cronología se oculta si las fechas no son suficientes")
    void cronologiaInsuficiente() throws Exception {
        long locacion = locacionPrueba("Cronología insuficiente");
        String ruta = "/locaciones/" + locacion;
        for (String fecha : new String[] {"1995", "1995-01-01", null}) {
            String pagina = texto(get(ruta));
            assertThat(pagina).contains("Esta locación aún no cuenta con suficientes documentos fechados para generar una cronología")
                    .doesNotContain("Ver línea del tiempo", "id=\"cronologia\"");
            if (fecha != null) documentoPrueba(locacion, "Hito " + fecha, fecha, "hito.pdf", "Texto.");
        }
    }

    @Test
    @DisplayName("V-08a · el ZIP contiene todos los originales y conserva sus nombres")
    void zipDeOriginales() throws Exception {
        long locacion = locacionPrueba("Archivo para ZIP");
        documentoPrueba(locacion, "Original", "1994", "ficha histórica.pdf", "Texto.");
        documentoPrueba(locacion, "Homónimo", "1995", "ficha histórica.pdf", "Texto.");
        var r = get("/locaciones/" + locacion + "/archivo.zip");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(cabecera(r, "Content-Type")).isEqualTo("application/zip");
        assertThat(cabecera(r, "Content-Disposition")).contains("attachment", ".zip");
        try (var zip = new ZipInputStream(new ByteArrayInputStream(r.body()))) {
            assertThat(zip.getNextEntry().getName()).isEqualTo("ficha histórica.pdf");
            assertThat(zip.readAllBytes()).isEqualTo(PDF);
            assertThat(zip.getNextEntry().getName()).isEqualTo("ficha histórica (2).pdf");
            assertThat(zip.readAllBytes()).isEqualTo(PDF);
            assertThat(zip.getNextEntry()).isNull();
        }
    }

    @Test
    @DisplayName("V-08b · el ZIP se permite en 100 MB y se rechaza si el total los excede")
    void limiteZip() throws Exception {
        long locacion = locacionPrueba("Límite ZIP");
        String ruta = "/locaciones/" + locacion + "/archivo.zip";
        assertThat(get(ruta).statusCode()).isEqualTo(422);
        long blob = db.queryForObject("""
                INSERT INTO binarios(nombre,tipo,contenido) VALUES ('grande.pdf','application/pdf',zeroblob(25000000))
                RETURNING id
                """, Long.class);
        for (int i = 0; i < 4; i++) insertarDocumento(locacion, blob, "Parte " + i, null, "Texto.");
        var r = get(ruta);
        assertThat(r.statusCode()).isEqualTo(200);
        long total = 0;
        try (var zip = new ZipInputStream(new ByteArrayInputStream(r.body()))) {
            while (zip.getNextEntry() != null) total += zip.transferTo(OutputStream.nullOutputStream());
        }
        assertThat(total).isEqualTo(100_000_000L);
        documentoPrueba(locacion, "Un original adicional", "1998", "otro.pdf", "Texto.");
        var rechazo = get(ruta);
        assertThat(rechazo.statusCode()).isEqualTo(422);
        assertThat(rechazo.headers().firstValue("Content-Disposition")).isEmpty();
        assertThat(texto(rechazo)).contains(PaquetesZip.MENSAJE_LIMITE);
    }

    @Test
    @DisplayName("V-09a · un administrador registra una locación visible en el mapa")
    void registrarLocacion() throws Exception {
        assertThat(cabecera(get(ALTA_LOCACION), "Location")).isEqualTo(ADMIN);
        String sesion = iniciarSesion();
        assertThat(texto(get(ALTA_LOCACION, sesion))).contains("Registrar locación");
        var r = post(ALTA_LOCACION, "nombre=Glorieta+de+Prueba&descripcion=Sitio+de+prueba&latitud=21.173&longitud=-86.821", sesion);
        assertThat(r.statusCode()).isEqualTo(303);
        assertThat(texto(get(cabecera(r, "Location")))).contains("Glorieta de Prueba", "21.17300, -86.82100");
        assertThat(texto(get("/"))).contains("Glorieta de Prueba", "data-lat=\"21.173\"", "data-lng=\"-86.821\"");
    }

    @Test
    @DisplayName("V-09b · se resaltan nombre duplicado y coordenadas fuera de rango")
    void locacionInvalida() throws Exception {
        String sesion = iniciarSesion();
        int antes = db.queryForObject("SELECT COUNT(*) FROM locaciones", Integer.class);
        var r = post(ALTA_LOCACION, "nombre=" + encode("LOCACIÓN PÚBLICA")
                + "&descripcion=Texto&latitud=91&longitud=-181", sesion);
        assertThat(r.statusCode()).isEqualTo(422);
        assertThat(texto(r)).contains("Ya existe una locación con ese nombre.", "error-nombre", "error-latitud",
                "error-longitud", "aria-invalid=\"true\"");
        assertThat(db.queryForObject("SELECT COUNT(*) FROM locaciones", Integer.class)).isEqualTo(antes);
    }

    @Test
    @DisplayName("V-10a · la publicación directa incorpora y descarga el documento")
    void publicarDocumento() throws Exception {
        long locacion = locacionPrueba("Publicación directa");
        assertThat(cabecera(get(PUBLICAR_DOCUMENTO), "Location")).isEqualTo(ADMIN);
        String sesion = iniciarSesion();
        assertThat(texto(get(PUBLICAR_DOCUMENTO, sesion))).contains("Año o fecha histórica");
        var r = multipart(PUBLICAR_DOCUMENTO, Map.of("locacion", Long.toString(locacion), "titulo", "Ficha publicada",
                "fecha", "1997", "descripcion", "Texto de publicación"), "original.pdf", PDF, sesion);
        assertThat(r.statusCode()).isEqualTo(303);
        String documento = cabecera(r, "Location");
        assertThat(texto(get(documento))).contains("Ficha publicada", "Texto de publicación", "1997");
        assertThat(texto(get("/locaciones/" + locacion))).contains("Ficha publicada", documento);
        assertThat(get(documento + "/original").body()).isEqualTo(PDF);
    }

    @Test
    @DisplayName("V-10b · publicar sin locación o año no crea documentos ni binarios")
    void publicacionIncompleta() throws Exception {
        String sesion = iniciarSesion();
        int documentos = db.queryForObject("SELECT COUNT(*) FROM documentos", Integer.class);
        int binarios = db.queryForObject("SELECT COUNT(*) FROM binarios", Integer.class);
        var r = multipart(PUBLICAR_DOCUMENTO, Map.of("titulo", "Sin año", "descripcion", "Texto"),
                "original.pdf", PDF, sesion);
        assertThat(r.statusCode()).isEqualTo(422);
        assertThat(texto(r)).contains("Selecciona una locación activa.", "Indica el año de procedencia.",
                "error-locacion", "error-fecha");
        assertThat(db.queryForObject("SELECT COUNT(*) FROM documentos", Integer.class)).isEqualTo(documentos);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM binarios", Integer.class)).isEqualTo(binarios);
    }

    @Test
    @DisplayName("V-11a · los enlaces internos llevan a una locación o a otro documento")
    void enlacesInternos() throws Exception {
        long destino = locacionPrueba("Destino de enlace");
        long vinculado = documentoPrueba(destino, "Archivo relacionado", "1992", "destino.pdf", "Texto.");
        long documento = documentoPrueba(1, "Fuente de enlaces", null, "fuente.pdf",
                "Consulta [la locación](/locaciones/" + destino + ") y [el documento](/documentos/" + vinculado + ").");
        String ruta = "/documentos/" + documento;
        assertThat(texto(get(ruta))).contains("la locación</a>", "el documento</a>", ruta + "/vinculo")
                .doesNotContain("[la locación]");
        assertThat(cabecera(get(ruta + "/vinculo?destino=locaciones%2F" + destino), "Location"))
                .isEqualTo("/locaciones/" + destino);
        assertThat(cabecera(get(ruta + "/vinculo?destino=documentos%2F" + vinculado), "Location"))
                .isEqualTo("/documentos/" + vinculado);
    }

    @Test
    @DisplayName("V-11b · un enlace a un destino inactivo vuelve al documento con un aviso")
    void enlaceInactivo() throws Exception {
        long documento = documentoPrueba(1, "Fuente con destino inactivo", null, "fuente.pdf",
                "[Locación retirada](/locaciones/3)");
        String ruta = "/documentos/" + documento;
        var r = get(ruta + "/vinculo?destino=locaciones%2F3");
        assertThat(r.statusCode()).isEqualTo(303);
        assertThat(cabecera(r, "Location")).isEqualTo(ruta + "?vinculo=ausente");
        assertThat(texto(get(cabecera(r, "Location")))).contains("El recurso vinculado ya no se encuentra disponible",
                "Fuente con destino inactivo");
    }

    @Test
    @DisplayName("V-12a · filtrar por nombre o reseña cambia el conjunto de marcadores")
    void buscarLocaciones() throws Exception {
        assertThat(texto(get("/?q=publica"))).contains("Locación pública</a>", "data-lat=\"21.16\"")
                .doesNotContain("Segunda locación</a>");
        assertThat(texto(get("/?q=otra+resena"))).contains("Segunda locación</a>", "data-lat=\"21.19\"")
                .doesNotContain("Locación pública</a>");
    }

    @Test
    @DisplayName("V-12b · buscar sin coincidencias conserva el filtro anterior")
    void conservarFiltro() throws Exception {
        String pagina = texto(get("/?q=SinCoincidencias&anterior=Segunda"));
        assertThat(pagina).contains("No se encontraron locaciones que coincidan con la búsqueda",
                "Segunda locación</a>", "data-lat=\"21.19\"").doesNotContain("Locación pública</a>");
    }

    private long locacionPrueba(String nombre) {
        return db.queryForObject("""
                INSERT INTO locaciones (nombre,descripcion,latitud,longitud) VALUES (?,'Locación de prueba.',21.5,-87)
                RETURNING id
                """, Long.class, nombre);
    }

    private long documentoPrueba(long locacion, String titulo, String fecha, String nombre, String descripcion) {
        long binario = db.queryForObject("""
                INSERT INTO binarios (nombre,tipo,contenido) VALUES (?,'application/pdf',?) RETURNING id
                """, Long.class, nombre, PDF);
        return insertarDocumento(locacion, binario, titulo, fecha, descripcion);
    }

    private long insertarDocumento(long locacion, long binario, String titulo, String fecha, String descripcion) {
        return db.queryForObject("""
                INSERT INTO documentos (locacion_id,binario_id,titulo,fecha_historica,descripcion)
                VALUES (?,?,?,?,?) RETURNING id
                """, Long.class, locacion, binario, titulo, fecha, descripcion);
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
        return multipart("/aportar", Map.of("locacion", "1", "descripcion", "Aporte de prueba"), nombre, contenido, null);
    }

    private HttpResponse<byte[]> multipart(String ruta, Map<String, String> campos, String nombre, byte[] contenido, String sesion)
            throws Exception {
        var cuerpo = new ByteArrayOutputStream();
        for (var campo : campos.entrySet()) {
            cuerpo.writeBytes(("--" + LIMITE_MULTIPART + "\r\nContent-Disposition: form-data; name=\"" + campo.getKey() +
                    "\"\r\n\r\n" + campo.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        if (nombre != null) {
            cuerpo.writeBytes(("--" + LIMITE_MULTIPART + "\r\nContent-Disposition: form-data; name=\"archivo\"; filename=\"" +
                    nombre + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            cuerpo.writeBytes(contenido);
            cuerpo.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        cuerpo.writeBytes(("--" + LIMITE_MULTIPART + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return CLIENTE.send(solicitud(ruta, sesion)
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
