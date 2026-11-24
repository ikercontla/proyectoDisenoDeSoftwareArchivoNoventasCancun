package mx.archivonoventas;

import static mx.archivonoventas.RespuestaWeb.formatosDeCarga;
import static mx.archivonoventas.RespuestaWeb.irA;
import static mx.archivonoventas.RespuestaWeb.pagina;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

@Controller
class Pantallas {
    private static final Logger LOG = LoggerFactory.getLogger(Pantallas.class);
    private static final String PANEL = "/administracion/propuestas";
    private static final String FOLIO = "/administracion/propuestas/{folio:[a-f0-9-]{36}}";
    private final Archivo archivo;
    private final Originales originales;
    private final Propuestas propuestas;
    private final Sesiones sesiones;
    private final PaquetesZip paquetes;
    private final String mapaClave;
    private final String mapaId;
    private final boolean cookieSegura;

    Pantallas(Archivo archivo, Originales originales, Propuestas propuestas, Sesiones sesiones, PaquetesZip paquetes,
              @Value("${archivo.maps-key}") String mapaClave, @Value("${archivo.maps-id}") String mapaId,
              @Value("${archivo.cookie-segura}") boolean cookieSegura) {
        this.archivo = archivo;
        this.originales = originales;
        this.propuestas = propuestas;
        this.sesiones = sesiones;
        this.paquetes = paquetes;
        this.mapaClave = mapaClave;
        this.mapaId = mapaId;
        this.cookieSegura = cookieSegura;
    }

    @GetMapping("/")
    String inicio(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String anterior, Model modelo) {
        var resultado = BusquedaLocaciones.buscar(archivo.locaciones(), q, anterior);
        modelo.addAttribute("locaciones", resultado.locaciones()).addAttribute("mapaClave", mapaClave)
                .addAttribute("consulta", q).addAttribute("filtroActivo", resultado.filtroActivo())
                .addAttribute("sinCoincidencias", resultado.sinCoincidencias())
                .addAttribute("mapaId", mapaId).addAttribute("usaMapa", !mapaClave.isBlank());
        return pagina(modelo, "inicio", "Explorar el archivo", "mapa");
    }

    @GetMapping("/locaciones/{id:[0-9]+}")
    String locacion(@PathVariable long id, Model modelo, HttpServletResponse res) {
        Locacion locacion = archivo.locacion(id);
        if (locacion == null) {
            res.setStatus(404);
            modelo.addAttribute("volverMapa", true);
            return pagina(modelo, "locacionAusente", "Locación inexistente", "");
        }
        var documentos = archivo.documentos(id);
        modelo.addAttribute("locacion", locacion).addAttribute("documentos", documentos)
                .addAttribute("cronologia", Cronologia.de(documentos));
        return pagina(modelo, "locacion", locacion.nombre(), "");
    }

    @GetMapping("/locaciones/{id:[0-9]+}/archivo.zip")
    void descargarArchivo(@PathVariable long id, HttpServletResponse res) throws IOException {
        if (archivo.locacion(id) == null) throw new ErrorAplicacion(404, "La locación no existe.");
        var entradas = paquetes.preparar(id);
        res.setContentType("application/zip");
        res.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename("archivo-locacion-" + id + ".zip").build().toString());
        paquetes.escribir(entradas, res.getOutputStream());
    }

    @GetMapping("/documentos/{id:[0-9]+}")
    String detalleDocumento(@PathVariable long id, @RequestParam(defaultValue = "") String vinculo, Model modelo) {
        Documento documento = buscarDocumento(id);
        modelo.addAttribute("documento", documento).addAttribute("locacion", archivo.locacion(documento.locacionId()))
                .addAttribute("segmentos", TextoVinculado.separar(documento.descripcion()));
        if (vinculo.equals("ausente")) modelo.addAttribute("mensaje", "El recurso vinculado ya no se encuentra disponible");
        return pagina(modelo, "documento", documento.titulo(), "");
    }

    @GetMapping("/documentos/{id:[0-9]+}/vinculo")
    String navegarVinculo(@PathVariable long id, @RequestParam(defaultValue = "") String destino, HttpServletResponse res) {
        Documento fuente = buscarDocumento(id);
        for (TextoVinculado segmento : TextoVinculado.separar(fuente.descripcion())) {
            if (destino.equals(segmento.destino()) && destinoDisponible(destino)) return irA(res, "/" + destino);
        }
        return irA(res, "/documentos/" + id + "?vinculo=ausente");
    }

    @GetMapping("/documentos/{id:[0-9]+}/original")
    ResponseEntity<byte[]> documento(@PathVariable long id) {
        Documento documento = buscarDocumento(id);
        Originales.Original original = originales.buscar(documento.binarioId());
        if (original == null) {
            LOG.warn("Original ausente para el documento {}", id);
            throw new ErrorAplicacion(404, PropuestaAporte.DESCARGA_NO_DISPONIBLE);
        }
        return descargar(original);
    }

    @GetMapping("/aportar")
    String aporte(@RequestParam(required = false) String folio, @RequestParam(defaultValue = "") String locacion, Model modelo) {
        if (folio != null && folio.matches("[a-f0-9-]{36}") && propuestas.existe(folio)) {
            modelo.addAttribute("folio", folio);
            return pagina(modelo, "aporteRegistrado", "Aporte recibido", "aportar");
        }
        return formularioAporte(modelo, locacion, "", "", null);
    }

    @PostMapping("/aportar")
    String registrarAporte(@RequestParam(defaultValue = "") String locacion, @RequestParam(defaultValue = "") String descripcion,
                           @RequestParam(defaultValue = "") String fecha, @RequestParam(required = false) MultipartFile archivo,
                           Model modelo, HttpServletResponse res) throws IOException {
        try {
            return irA(res, "/aportar?folio=" + propuestas.recibir(idLocacion(locacion), descripcion, fecha, archivo));
        } catch (ErrorAplicacion e) {
            res.setStatus(e.estado());
            return formularioAporte(modelo, locacion, descripcion, fecha, e.getMessage());
        }
    }

    @GetMapping("/administracion")
    String acceso(HttpServletRequest req, HttpServletResponse res, Model modelo) {
        if (sesiones.usuarioActivo(Web.cookie(req)) != null) return irA(res, PANEL);
        return pagina(modelo, "acceso", "Acceso de administración", "admin");
    }

    @PostMapping("/administracion")
    String entrar(@RequestParam(defaultValue = "") String usuario, @RequestParam(defaultValue = "") String contrasena,
                  HttpServletRequest req, HttpServletResponse res, Model modelo) {
        String admin = sesiones.autenticar(usuario, contrasena);
        if (admin == null) {
            res.setStatus(401);
            modelo.addAttribute("mensaje", "Credenciales no válidas. Verifique sus datos").addAttribute("usuario", usuario);
            return pagina(modelo, "acceso", "Acceso de administración", "admin");
        }
        String token = sesiones.iniciar(admin, Web.cookie(req));
        res.addHeader(HttpHeaders.SET_COOKIE, cookie(token, Sesiones.DURACION));
        return irA(res, PANEL);
    }

    @PostMapping("/administracion/salir")
    String salir(HttpServletRequest req, HttpServletResponse res) {
        sesiones.cerrar(Web.cookie(req));
        res.addHeader(HttpHeaders.SET_COOKIE, cookie("", 0));
        return irA(res, "/administracion");
    }

    @GetMapping(PANEL)
    String panel(Model modelo) {
        modelo.addAttribute("propuestas", propuestas.listar());
        return pagina(modelo, "panel", "Propuestas ciudadanas", "admin");
    }

    @GetMapping(FOLIO)
    String propuesta(@PathVariable String folio, Model modelo) {
        return detalle(modelo, propuestas.buscar(folio), null);
    }

    @GetMapping(FOLIO + "/original")
    ResponseEntity<byte[]> originalAporte(@PathVariable String folio) {
        Originales.Original original = originales.buscar(propuestas.buscar(folio).binarioId());
        if (original == null) throw new ErrorAplicacion(404, "El original del aporte no está disponible.");
        return descargar(original);
    }

    @PostMapping(FOLIO + "/revisar")
    String revisar(@PathVariable String folio, @RequestAttribute(Web.USUARIO) String usuario, HttpServletResponse res) {
        propuestas.revisar(folio, usuario);
        return irA(res, PANEL + "/" + folio);
    }

    @PostMapping(FOLIO + "/aprobar")
    String aprobar(@PathVariable String folio, @RequestAttribute(Web.USUARIO) String usuario,
                   @RequestParam(defaultValue = "") String titulo, @RequestParam(defaultValue = "") String fecha,
                   Model modelo, HttpServletResponse res) {
        try {
            propuestas.aprobar(folio, usuario, titulo, fecha);
            return irA(res, PANEL + "/" + folio);
        } catch (ErrorAplicacion e) {
            res.setStatus(e.estado());
            return detalle(modelo, propuestas.buscar(folio), e.getMessage());
        }
    }

    @PostMapping(FOLIO + "/rechazar")
    String rechazar(@PathVariable String folio, @RequestAttribute(Web.USUARIO) String usuario,
                    @RequestParam(defaultValue = "") String motivo, Model modelo, HttpServletResponse res) {
        try {
            propuestas.rechazar(folio, usuario, motivo);
            return irA(res, PANEL + "/" + folio);
        } catch (ErrorAplicacion e) {
            res.setStatus(e.estado());
            return detalle(modelo, propuestas.buscar(folio), e.getMessage());
        }
    }

    @ExceptionHandler(ErrorAplicacion.class)
    String error(ErrorAplicacion e, Model modelo, HttpServletResponse res) {
        return RespuestaWeb.error(e, modelo, res);
    }

    @ExceptionHandler(MultipartException.class)
    String adjuntoInvalido(MultipartException e, Model modelo, HttpServletResponse res) {
        res.setStatus(RespuestaWeb.estadoCarga(e));
        return formularioAporte(modelo, "", "", "", Adjunto.ARCHIVO_INVALIDO);
    }

    private Documento buscarDocumento(long id) {
        Documento documento = archivo.documentoPublicado(id);
        if (documento == null) throw new ErrorAplicacion(404, "El documento no existe.");
        return documento;
    }

    private boolean destinoDisponible(String destino) {
        var partes = TextoVinculado.DESTINO.matcher(destino);
        if (!partes.matches()) return false;
        long id = idLocacion(partes.group(2));
        if (id == 0) return false;
        return partes.group(1).equals("locaciones") ? archivo.locacion(id) != null : archivo.documentoPublicado(id) != null;
    }

    private String formularioAporte(Model modelo, String locacion, String descripcion, String fecha, String mensaje) {
        modelo.addAttribute("locaciones", archivo.locaciones()).addAttribute("seleccion", locacion)
                .addAttribute("descripcion", descripcion).addAttribute("fecha", fecha).addAttribute("mensaje", mensaje);
        formatosDeCarga(modelo);
        return pagina(modelo, "aporte", "Aportar al archivo", "aportar");
    }

    private static String detalle(Model modelo, PropuestaAporte propuesta, String mensaje) {
        modelo.addAttribute("p", propuesta).addAttribute("mensaje", mensaje)
                .addAttribute("ruta", PANEL + "/" + propuesta.folio());
        return pagina(modelo, "propuesta", propuesta.nombre(), "admin");
    }

    private static long idLocacion(String locacion) {
        try {
            return Long.parseLong(locacion);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String cookie(String token, int duracion) {
        return ResponseCookie.from(Sesiones.COOKIE, token).path("/").maxAge(duracion).httpOnly(true)
                .sameSite("Strict").secure(cookieSegura).build().toString();
    }

    private static ResponseEntity<byte[]> descargar(Originales.Original original) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(original.tipo()))
                .contentLength(original.contenido().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(original.nombre(), StandardCharsets.UTF_8).build().toString())
                .body(original.contenido());
    }
}
