package psg.facilitei.Services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MercadoPagoWebhookVerifier {
    private static final Duration MAX_AGE = Duration.ofMinutes(10);
    private final String secret;
    private final Clock clock;

    @Autowired
    public MercadoPagoWebhookVerifier(@Value("${mercadopago.webhook-secret:}") String secret) {
        this(secret, Clock.systemUTC());
    }

    MercadoPagoWebhookVerifier(String secret, Clock clock) {
        this.secret = secret == null ? "" : secret.trim();
        this.clock = clock;
    }

    public void verify(String signature, String requestId, String dataId) {
        if (secret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Webhook do Mercado Pago ainda não configurado.");
        }
        if (signature == null || requestId == null || dataId == null || dataId.isBlank()) unauthorized();

        String timestamp = null;
        String suppliedHash = null;
        for (String part : signature.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length != 2) continue;
            if ("ts".equals(pair[0])) timestamp = pair[1];
            if ("v1".equals(pair[0])) suppliedHash = pair[1].toLowerCase(Locale.ROOT);
        }
        if (timestamp == null || suppliedHash == null || !suppliedHash.matches("[0-9a-f]{64}")) unauthorized();
        validateFreshness(timestamp);

        String manifest = "id:" + dataId.toLowerCase(Locale.ROOT)
                + ";request-id:" + requestId + ";ts:" + timestamp + ";";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            byte[] supplied = HexFormat.of().parseHex(suppliedHash);
            if (!MessageDigest.isEqual(expected, supplied)) unauthorized();
        } catch (java.security.GeneralSecurityException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Webhook inválido.", ex);
        }
    }

    private void validateFreshness(String timestamp) {
        try {
            long raw = Long.parseLong(timestamp);
            Instant signedAt = timestamp.length() > 10 ? Instant.ofEpochMilli(raw) : Instant.ofEpochSecond(raw);
            Duration age = Duration.between(signedAt, clock.instant()).abs();
            if (age.compareTo(MAX_AGE) > 0) unauthorized();
        } catch (NumberFormatException ex) {
            unauthorized();
        }
    }

    private static void unauthorized() {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Webhook inválido.");
    }
}
