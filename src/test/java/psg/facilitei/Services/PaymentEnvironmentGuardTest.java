package psg.facilitei.Services;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import psg.facilitei.Config.LegalProperties;

import static org.junit.jupiter.api.Assertions.*;

class PaymentEnvironmentGuardTest {

    @Test
    void modoTesteAceitaSomenteCredencialDeTeste() {
        LegalProperties legal = new LegalProperties();
        legal.setPaymentsMode(LegalProperties.PaymentsMode.TEST);

        PaymentEnvironmentGuard valid = guard(legal, "TEST-abc", "", "", 2990);
        assertEquals("TEST-abc", valid.accessToken());
        assertTrue(valid.isTestEnvironment());
        assertTrue(valid.isCheckoutEnabled());

        PaymentEnvironmentGuard invalid = guard(legal, "APP_USR-live", "", "", 2990);
        assertFalse(invalid.isCheckoutEnabled());
        assertThrows(ResponseStatusException.class, invalid::requireCheckoutReady);
    }

    @Test
    void modoTestePermiteCredencialLegadaApenasSeForDeTeste() {
        LegalProperties legal = new LegalProperties();
        legal.setPaymentsMode(LegalProperties.PaymentsMode.TEST);

        assertEquals("TEST-legacy", guard(legal, "", "", "TEST-legacy", 2990).accessToken());
        assertFalse(guard(legal, "", "", "APP_USR-live", 2990).isCheckoutEnabled());
    }

    @Test
    void modoLiveFalhaFechadoSemIdentidadeOuCredencialLiveDedicada() {
        LegalProperties incomplete = new LegalProperties();
        incomplete.setPaymentsMode(LegalProperties.PaymentsMode.LIVE);
        PaymentEnvironmentGuard noIdentity = guard(
                incomplete, "TEST-abc", "APP_USR-live", "APP_USR-legacy", 2990);
        assertThrows(IllegalStateException.class, noIdentity::validateStartup);

        LegalProperties complete = completeLiveProperties();
        PaymentEnvironmentGuard legacyOnly = guard(
                complete, "TEST-abc", "", "APP_USR-legacy", 2990);
        assertThrows(IllegalStateException.class, legacyOnly::validateStartup);

        PaymentEnvironmentGuard valid = guard(
                complete, "TEST-abc", "APP_USR-live", "APP_USR-legacy", 2990);
        assertDoesNotThrow(valid::validateStartup);
        assertEquals("APP_USR-live", valid.accessToken());
        assertFalse(valid.isTestEnvironment());
    }

    @Test
    void valorMensalDeveSerPositivoEmQualquerAmbiente() {
        LegalProperties legal = new LegalProperties();
        legal.setPaymentsMode(LegalProperties.PaymentsMode.TEST);
        assertFalse(guard(legal, "TEST-abc", "", "", 0).isCheckoutEnabled());
    }

    private PaymentEnvironmentGuard guard(
            LegalProperties legal,
            String testToken,
            String liveToken,
            String legacyToken,
            long amount) {
        return new PaymentEnvironmentGuard(legal, testToken, liveToken, legacyToken, amount);
    }

    private LegalProperties completeLiveProperties() {
        LegalProperties legal = new LegalProperties();
        legal.setPaymentsMode(LegalProperties.PaymentsMode.LIVE);
        legal.setLegalName("Facilitei Tecnologia Ltda.");
        legal.setTaxId("00.000.000/0001-00");
        legal.setBusinessAddress("Rua Exemplo, 100, Belo Horizonte/MG");
        legal.setContactEmail("contato@facilitei.online");
        return legal;
    }
}
