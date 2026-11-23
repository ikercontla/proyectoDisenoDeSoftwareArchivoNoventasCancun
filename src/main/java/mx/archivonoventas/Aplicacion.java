package mx.archivonoventas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

@SpringBootApplication
public class Aplicacion {
    public static void main(String[] args) {
        SpringApplication aplicacion = new SpringApplication(Aplicacion.class);
        String comando = args.length > 0 ? args[0] : "";
        if (!comando.equals("demo") && !comando.equals("admin")) {
            aplicacion.run(args);
            return;
        }

        aplicacion.setWebApplicationType(WebApplicationType.NONE);
        try (var contexto = aplicacion.run(args)) {
            if (comando.equals("demo")) {
                contexto.getBean(Archivo.class).cargarDemo();
                System.out.println("Datos de demostración disponibles.");
            } else {
                contexto.getBean(Sesiones.class).crearAdministrador(
                        System.getenv("ADMIN_USER"), System.getenv("ADMIN_PASSWORD"));
                System.out.println("Cuenta de administración creada.");
            }
        }
    }

    @Bean
    DataSource dataSource(@Value("${archivo.base-datos}") String ruta) throws IOException {
        Path archivo = Path.of(ruta).toAbsolutePath().normalize();
        Files.createDirectories(archivo.getParent());
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        config.setBusyTimeout(4000);
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE);
        SQLiteDataSource origen = new SQLiteDataSource(config);
        origen.setUrl("jdbc:sqlite:" + archivo);
        return origen;
    }
}
