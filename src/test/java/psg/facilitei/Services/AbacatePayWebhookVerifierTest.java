package psg.facilitei.Services;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class AbacatePayWebhookVerifierTest {
    @Test
    void exigeSecretEAssinaturaDoCorpoOriginal() throws Exception {
        AbacatePayWebhookVerifier verifier = new AbacatePayWebhookVerifier("secret", "signing-key");
        byte[] body = "{\"event\":\"subscription.completed\"}".getBytes(StandardCharsets.UTF_8);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("signing-key".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getEncoder().encodeToString(mac.doFinal(body));

        assertDoesNotThrow(() -> verifier.verify("secret", signature, body));
        assertThrows(ResponseStatusException.class,
                () -> verifier.verify("wrong", signature, body));
        assertThrows(ResponseStatusException.class,
                () -> verifier.verify("secret", signature, "{}".getBytes(StandardCharsets.UTF_8)));
    }
}
