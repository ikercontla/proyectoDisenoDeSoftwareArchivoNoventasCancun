package mx.archivonoventas;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

final class RespuestaWeb {
    private RespuestaWeb() {}

    static String pagina(Model modelo, String vista, String titulo, String seccion) {
        modelo.addAttribute("vista", vista).addAttribute("titulo", titulo).addAttribute("seccion", seccion);
        return "layout";
    }

    static String irA(HttpServletResponse res, String destino) {
        res.setStatus(303);
        res.setHeader(HttpHeaders.LOCATION, destino);
        return null;
    }

    static void formatosDeCarga(Model modelo) {
        modelo.addAttribute("formatosAceptados", Adjunto.extensionesAceptadas())
                .addAttribute("ayudaFormatos", Adjunto.ayudaFormatos());
    }

    static int estadoCarga(MultipartException error) {
        return error instanceof MaxUploadSizeExceededException ? 413 : 422;
    }

    static String error(ErrorAplicacion error, Model modelo, HttpServletResponse res) {
        res.setStatus(error.estado());
        modelo.addAttribute("mensaje", error.getMessage());
        return pagina(modelo, "error", "Error " + error.estado(), "");
    }
}
