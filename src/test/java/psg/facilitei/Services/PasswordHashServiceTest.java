package psg.facilitei.Services;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHashServiceTest {
    private final PasswordHashService service =
            new PasswordHashService(new BCryptPasswordEncoder(4));

    @Test
    void hashNuncaArmazenaSenhaEmTextoPuro() {
        String hash = service.hash("uma-senha-segura");

        assertNotEquals("uma-senha-segura", hash);
        assertTrue(service.isHashed(hash));
        assertTrue(service.matches("uma-senha-segura", hash));
        assertFalse(service.matches("senha-errada", hash));
    }

    @Test
    void reconheceSenhaLegadaParaPermitirMigracaoNoLogin() {
        assertTrue(service.matches("senha-legada", "senha-legada"));
        assertFalse(service.matches("outra", "senha-legada"));
        assertFalse(service.isHashed("senha-legada"));
    }
}
