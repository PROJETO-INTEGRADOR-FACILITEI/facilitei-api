package psg.facilitei.Services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Repository.AssinaturaPrestadorRepository;
import psg.facilitei.Repository.EventoPagamentoRepository;
import psg.facilitei.Repository.TrabalhadorRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@ActiveProfiles("test")
class AssinaturaPrestadorServiceTest {
    @Autowired private AssinaturaPrestadorRepository assinaturas;
    @Autowired private EventoPagamentoRepository eventos;
    @Autowired private TrabalhadorRepository trabalhadores;
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void pagamentoRenovacaoECancelamentoControlamAcessoSemDuplicarEvento() throws Exception {
        MercadoPagoClient client = mock(MercadoPagoClient.class);
        when(client.isConfigured()).thenReturn(true);
        when(client.createSubscription(anyLong(), eq("ana@example.com")))
                .thenReturn(new MercadoPagoClient.Subscription(
                        "pre_test", "https://www.mercadopago.com.br/subscriptions/checkout", 2990));
        when(client.externalReference(anyLong())).thenAnswer(call -> "facilitei-worker-" + call.getArgument(0));
        AssinaturaPrestadorService service = new AssinaturaPrestadorService(
                assinaturas, eventos, client, trabalhadores);

        Trabalhador worker = new Trabalhador();
        worker.setNome("Ana");
        worker.setEmail("ana@example.com");
        worker.setSenha("senha");
        worker = trabalhadores.save(worker);

        JsonNode subscription = subscription(worker.getId(), "authorized", "2026-10-22T12:00:00Z");
        when(client.getSubscription("pre_test")).thenReturn(subscription);
        when(client.getAuthorizedPayment("pay_first"))
                .thenReturn(payment("pay_first", "2026-09-22T12:00:00Z", 29.90, "approved"));

        assertEquals("PENDING", service.iniciar(worker).status());
        assertFalse(service.podeUsarPlataforma(worker.getId()));

        service.aplicarEvento("notification-1", "subscription_authorized_payment", "pay_first");
        assertTrue(service.podeUsarPlataforma(worker.getId()));
        Instant firstUntil = service.consultar(worker.getId()).ativaAte();

        service.aplicarEvento("notification-1", "subscription_authorized_payment", "pay_first");
        verify(client, times(1)).getAuthorizedPayment("pay_first");
        assertEquals(firstUntil, service.consultar(worker.getId()).ativaAte());

        when(client.getAuthorizedPayment("pay_second"))
                .thenReturn(payment("pay_second", "2026-10-22T12:00:00Z", 29.90, "approved"));
        when(client.getSubscription("pre_test"))
                .thenReturn(subscription(worker.getId(), "authorized", "2026-11-22T12:00:00Z"));
        service.aplicarEvento("notification-2", "subscription_authorized_payment", "pay_second");
        assertTrue(service.consultar(worker.getId()).ativaAte().isAfter(firstUntil));

        service.cancelar(worker.getId());
        verify(client).cancel("pre_test");
        assertEquals("CANCELLED", service.consultar(worker.getId()).status());

        when(client.getAuthorizedPayment("pay_late"))
                .thenReturn(payment("pay_late", "2026-11-22T12:00:00Z", 29.90, "approved"));
        service.aplicarEvento("notification-3", "subscription_authorized_payment", "pay_late");
        assertFalse(service.podeUsarPlataforma(worker.getId()));
    }

    private JsonNode subscription(Long workerId, String status, String nextPaymentDate) throws Exception {
        return json.readTree("""
                {"id":"pre_test","status":"%s","external_reference":"facilitei-worker-%d",
                 "next_payment_date":"%s","auto_recurring":{"frequency":1,
                 "frequency_type":"months","transaction_amount":29.90,"currency_id":"BRL"}}
                """.formatted(status, workerId, nextPaymentDate));
    }

    private JsonNode payment(String id, String debitDate, double amount, String status) throws Exception {
        return json.readTree(String.format(Locale.ROOT, """
                {"id":"%s","preapproval_id":"pre_test","transaction_amount":"%.2f",
                 "debit_date":"%s","payment":{"status":"%s"}}
                """, id, amount, debitDate, status));
    }
}
