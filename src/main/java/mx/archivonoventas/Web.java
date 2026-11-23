package mx.archivonoventas;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class Web extends OncePerRequestFilter {
    static final String USUARIO = "archivo.usuario";
    private final Sesiones sesiones;
    private final boolean segura;

    Web(Sesiones sesiones, @Value("${archivo.cookie-segura}") boolean segura) {
        this.sesiones = sesiones;
        this.segura = segura;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain cadena)
            throws ServletException, IOException {
        res.setHeader("Cache-Control", "no-store");
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        String origen = req.getHeader("Origin");
        String esperado = (segura ? "https://" : "http://") + req.getHeader("Host");
        if (req.getMethod().equals("POST") && origen != null && !origen.equals(esperado)) {
            res.sendError(403, "Envía el formulario desde el sitio del archivo.");
            return;
        }

        if (req.getServletPath().startsWith("/administracion/")) {
            String usuario = sesiones.usuarioActivo(cookie(req));
            if (usuario == null) {
                res.setStatus(303);
                res.setHeader("Location", "/administracion");
                return;
            }
            req.setAttribute(USUARIO, usuario);
        }
        cadena.doFilter(req, res);
    }

    static String cookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies).filter(c -> c.getName().equals(Sesiones.COOKIE))
                .map(Cookie::getValue).findFirst().orElse(null);
    }
}
