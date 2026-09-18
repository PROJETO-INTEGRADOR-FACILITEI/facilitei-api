package psg.facilitei.Services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AbacatePayWebhookVerifier {
    private final String secret;
    private final String signingKey;

    public AbacatePayWebhookVerifier(@Value("${abacatepay.webhook-secret:}") String secret,
                                     @Value("${abacatepay.webhook-public-key:}") String signingKey) {
        this.secret = secret;
        this.signingKey = signingKey;
    }

    public void verify(String suppliedSecret, String signature, byte[] rawBody) {
        if (secret.isBlank() || signingKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Webhook não configurado.");
        }
        if (suppliedSecret == null || signature == null ||
                !MessageDigest.isEqual(secret.getBytes(StandardCharsets.UTF_8),
                        suppliedSecret.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Webhook inválido.");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(rawBody);
            byte[] received = Base64.getDecoder().decode(signature);
            if (!MessageDigest.isEqual(expected, received)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Webhook inválido.");
            }
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Webhook inválido.");
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("Não foi possível validar o webhook.", ex);
        }
    }
}
