package psg.facilitei.Services;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import psg.facilitei.Config.LegalProperties;

@Component
public class PaymentEnvironmentGuard {
    private static final Logger log = LoggerFactory.getLogger(PaymentEnvironmentGuard.class);

    private final LegalProperties legal;
    private final String testAccessToken;
    private final String liveAccessToken;
    private final String legacyAccessToken;
    private final long monthlyAmountCents;
    private final AtomicBoolean legacyWarningLogged = new AtomicBoolean();

    public PaymentEnvironmentGuard(
            LegalProperties legal,
            @Value("${mercadopago.test-access-token:}") String testAccessToken,
            @Value("${mercadopago.live-access-token:}") String liveAccessToken,
            @Value("${mercadopago.access-token:}") String legacyAccessToken,
            @Value("${mercadopago.monthly-amount-cents:0}") long monthlyAmountCents) {
        this.legal = legal;
        this.testAccessToken = normalize(testAccessToken);
        this.liveAccessToken = normalize(liveAccessToken);
        this.legacyAccessToken = normalize(legacyAccessToken);
        this.monthlyAmountCents = monthlyAmountCents;
    }

    @PostConstruct
    void validateStartup() {
        if (legal.getPaymentsMode() == LegalProperties.PaymentsMode.LIVE) {
            try {
                validateConfiguration();
            } catch (IllegalStateException exception) {
                throw new IllegalStateException(
                        "LEGAL_PAYMENTS_MODE=LIVE recusado: " + exception.getMessage(), exception);
            }
        }
    }

    public void requireCheckoutReady() {
        try {
            validateConfiguration();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Cobrança indisponível: " + exception.getMessage());
        }
    }

    public boolean isCheckoutEnabled() {
        try {
            validateConfiguration();
            return true;
        } catch (IllegalStateException exception) {
            return false;
        }
    }

    public String accessToken() {
        requireCheckoutReady();
        return selectedToken();
    }

    public String paymentMode() {
        return legal.getPaymentsMode().name();
    }

    public boolean isTestEnvironment() {
        return legal.getPaymentsMode() == LegalProperties.PaymentsMode.TEST;
    }

    private void validateConfiguration() {
        if (monthlyAmountCents <= 0) {
            throw new IllegalStateException("o valor mensal precisa ser maior que zero.");
        }
        if (legal.getPaymentsMode() == LegalProperties.PaymentsMode.LIVE && !legal.isProfileComplete()) {
            throw new IllegalStateException(
                    "a identidade legal do operador precisa de nome legal, documento fiscal, endereço e e-mail.");
        }

        String token = selectedToken();
        if (isTestEnvironment() && !token.startsWith("TEST-")) {
            throw new IllegalStateException("configure uma credencial de teste do Mercado Pago.");
        }
        if (!isTestEnvironment() && !token.startsWith("APP_USR-")) {
            throw new IllegalStateException("configure MERCADOPAGO_LIVE_ACCESS_TOKEN com uma credencial LIVE.");
        }
    }

    private String selectedToken() {
        if (!isTestEnvironment()) {
            if (liveAccessToken.isBlank()) {
                throw new IllegalStateException(
                        "MERCADOPAGO_LIVE_ACCESS_TOKEN não foi configurado; a credencial antiga não é aceita em LIVE.");
            }
            return liveAccessToken;
        }
        if (!testAccessToken.isBlank()) return testAccessToken;
        if (!legacyAccessToken.isBlank()) {
            if (legacyWarningLogged.compareAndSet(false, true)) {
                log.warn("MERCADOPAGO_ACCESS_TOKEN está obsoleta e só é aceita temporariamente em TEST; "
                        + "migre para MERCADOPAGO_TEST_ACCESS_TOKEN.");
            }
            return legacyAccessToken;
        }
        throw new IllegalStateException("MERCADOPAGO_TEST_ACCESS_TOKEN não foi configurado.");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
