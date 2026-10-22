package psg.facilitei.Controller;

import java.sql.Connection;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Healthcheck usado pela plataforma de deploy (Railway). Responde 200 somente
 * quando a aplicação consegue abrir uma conexão válida com o banco; caso
 * contrário responde 503, impedindo que um deploy sem banco receba tráfego.
 */
@RestController
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    private static final int DB_VALIDATION_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(DB_VALIDATION_TIMEOUT_SECONDS)) {
                return ResponseEntity.ok(Map.of("status", "UP"));
            }
            log.warn("Healthcheck: conexão com o banco retornou inválida");
        } catch (Exception e) {
            log.warn("Healthcheck: falha ao conectar no banco: {}", e.getMessage());
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", "DOWN"));
    }
}
