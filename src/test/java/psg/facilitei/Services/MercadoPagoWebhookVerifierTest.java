package psg.facilitei.Services;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class MercadoPagoWebhookVerifierTest {
    private static final Instant NOW = Instant.parse("2026-09-22T15:00:00Z");

    @Test
    void validaManifestoOficialERejeitaAssinaturaOuTimestampInvalidos() throws Exception {
        MercadoPagoWebhookVerifier verifier = new MercadoPagoWebhookVerifier(
                "webhook-secret", Clock.fixed(NOW, ZoneOffset.UTC));
        String requestId = "request-123";
        String dataId = "PRE-ABC";
        String timestamp = String.valueOf(NOW.toEpochMilli());
        String manifest = "id:pre-abc;request-id:" + requestId + ";ts:" + timestamp + ";";
        String signature = "ts=" + timestamp + ",v1=" + hmac(manifest);

        assertDoesNotThrow(() -> verifier.verify(signature, requestId, dataId));
        assertThrows(ResponseStatusException.class,
                () -> verifier.verify(signature, "request-alterado", dataId));

        String oldTimestamp = String.valueOf(NOW.minusSeconds(700).getEpochSecond());
        String oldManifest = "id:pre-abc;request-id:" + requestId + ";ts:" + oldTimestamp + ";";
        assertThrows(ResponseStatusException.class,
                () -> verifier.verify("ts=" + oldTimestamp + ",v1=" + hmac(oldManifest), requestId, dataId));
    }

    private String hmac(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("webhook-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
