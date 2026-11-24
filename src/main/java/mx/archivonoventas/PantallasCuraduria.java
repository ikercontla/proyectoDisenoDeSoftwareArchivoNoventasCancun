package mx.archivonoventas;

import static mx.archivonoventas.RespuestaWeb.formatosDeCarga;
import static mx.archivonoventas.RespuestaWeb.irA;
import static mx.archivonoventas.RespuestaWeb.pagina;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

@Controller
class PantallasCuraduria {
    private static final String ALTA_LOCACION = "/administracion/locaciones/nueva";
    private static final String PUBLICAR_DOCUMENTO = "/administracion/documentos/nuevo";
    private final Archivo archivo;
    private final Curaduria curaduria;

    PantallasCuraduria(Archivo archivo, Curaduria curaduria) {
        this.archivo = archivo;
        this.curaduria = curaduria;
    }

    @GetMapping(ALTA_LOCACION)
    String nuevaLocacion(Model modelo) {
        return formularioLocacion(modelo, Map.of(), Map.of());
    }

    @PostMapping(ALTA_LOCACION)
    String guardarLocacion(@RequestParam(defaultValue = "") String nombre, @RequestParam(defaultValue = "") String descripcion,
                           @RequestParam(defaultValue = "") String latitud, @RequestParam(defaultValue = "") String longitud,
                           Model modelo, HttpServletResponse res) {
        try {
            return irA(res, "/locaciones/" + curaduria.registrarLocacion(nombre, descripcion, latitud, longitud));
        } catch (CamposInvalidos error) {
            res.setStatus(422);
            return formularioLocacion(modelo, Map.of("nombre", nombre, "descripcion", descripcion,
                    "latitud", latitud, "longitud", longitud), error.campos());
        }
    }

    @GetMapping(PUBLICAR_DOCUMENTO)
    String nuevoDocumento(Model modelo) {
        return formularioPublicacion(modelo, Map.of(), Map.of());
    }

    @PostMapping(PUBLICAR_DOCUMENTO)
    String publicarDocumento(@RequestParam(defaultValue = "") String locacion, @RequestParam(defaultValue = "") String titulo,
                             @RequestParam(defaultValue = "") String fecha, @RequestParam(defaultValue = "") String descripcion,
                             @RequestParam(required = false) MultipartFile archivo, Model modelo, HttpServletResponse res)
            throws IOException {
        try {
            return irA(res, "/documentos/" + curaduria.publicar(locacion, titulo, fecha, descripcion, archivo));
        } catch (CamposInvalidos error) {
            res.setStatus(422);
            return formularioPublicacion(modelo, Map.of("locacion", locacion, "titulo", titulo,
                    "fecha", fecha, "descripcion", descripcion), error.campos());
        }
    }

    @ExceptionHandler(ErrorAplicacion.class)
    String error(ErrorAplicacion error, Model modelo, HttpServletResponse res) {
        return RespuestaWeb.error(error, modelo, res);
    }

    @ExceptionHandler(MultipartException.class)
    String adjuntoInvalido(MultipartException error, Model modelo, HttpServletResponse res) {
        res.setStatus(RespuestaWeb.estadoCarga(error));
        return formularioPublicacion(modelo, Map.of(), Map.of("archivo", Adjunto.ARCHIVO_INVALIDO));
    }

    private static String formularioLocacion(Model modelo, Map<String, String> valores, Map<String, String> errores) {
        modelo.addAttribute("valores", valores).addAttribute("errores", errores);
        return pagina(modelo, "altaLocacion", "Registrar locación", "admin");
    }

    private String formularioPublicacion(Model modelo, Map<String, String> valores, Map<String, String> errores) {
        modelo.addAttribute("valores", valores).addAttribute("errores", errores).addAttribute("locaciones", archivo.locaciones());
        formatosDeCarga(modelo);
        return pagina(modelo, "publicarDocumento", "Publicar documento", "admin");
    }
}
