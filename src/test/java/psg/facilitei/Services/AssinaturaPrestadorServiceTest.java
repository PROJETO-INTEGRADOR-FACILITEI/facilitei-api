package psg.facilitei.Services;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Repository.AssinaturaPrestadorRepository;
import psg.facilitei.Repository.EventoAbacatePayRepository;
import psg.facilitei.Repository.TrabalhadorRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@ActiveProfiles("test")
class AssinaturaPrestadorServiceTest {
    @Autowired private AssinaturaPrestadorRepository assinaturas;
    @Autowired private EventoAbacatePayRepository eventos;
    @Autowired private TrabalhadorRepository trabalhadores;
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void pagamentoRenovacaoECancelamentoControlamAcessoSemDuplicarEvento() throws Exception {
        AbacatePayClient client = mock(AbacatePayClient.class);
        when(client.criarCheckout(anyLong()))
                .thenReturn(new AbacatePayClient.Checkout("bill_test", "https://app.abacatepay.com/pay/bill_test", 2990));
        when(client.productId()).thenReturn("prod_monthly");
        when(client.statusCheckout("bill_test")).thenReturn("PENDING");
        AssinaturaPrestadorService service = new AssinaturaPrestadorService(assinaturas, eventos, client, trabalhadores);

        Trabalhador worker = new Trabalhador();
        worker.setNome("Ana");
        worker.setEmail("ana@example.com");
        worker.setSenha("senha");
        worker = trabalhadores.save(worker);

        assertEquals("PENDING", service.iniciar(worker).status());
        assertEquals("PENDING", service.iniciar(worker).status());
        verify(client, times(1)).criarCheckout(worker.getId());
        assertFalse(service.podeUsarPlataforma(worker.getId()));

        Instant paidAt = Instant.now();
        service.aplicarEvento(evento("log_first", "subscription.completed", paidAt, true));
        assertTrue(service.podeUsarPlataforma(worker.getId()));
        Instant firstUntil = service.consultar(worker.getId()).ativaAte();

        service.aplicarEvento(evento("log_first", "subscription.completed", paidAt, true));
        assertEquals(firstUntil, service.consultar(worker.getId()).ativaAte());

        var renovacao = (com.fasterxml.jackson.databind.node.ObjectNode)
                evento("log_second", "subscription.renewed", paidAt.plusSeconds(30L * 86400), false);
        ((com.fasterxml.jackson.databind.node.ObjectNode) renovacao.path("data").path("subscription"))
                .put("amount", 3990);
        ((com.fasterxml.jackson.databind.node.ObjectNode) renovacao.path("data").path("payment"))
                .put("amount", 3990);
        service.aplicarEvento(renovacao);
        assertTrue(service.consultar(worker.getId()).ativaAte().isAfter(firstUntil));
        assertEquals(3990L, service.consultar(worker.getId()).valorCentavos());

        service.aplicarEvento(evento("log_cancel", "subscription.cancelled", paidAt, false));
        assertFalse(service.podeUsarPlataforma(worker.getId()));
        assertEquals("CANCELLED", service.consultar(worker.getId()).status());
        service.aplicarEvento(evento("log_late", "subscription.renewed", paidAt.plusSeconds(60L * 86400), false));
        assertFalse(service.podeUsarPlataforma(worker.getId()));
    }

    private com.fasterxml.jackson.databind.JsonNode evento(String id, String type, Instant time,
                                                               boolean initial) throws Exception {
        String checkout = initial
                ? "\"checkout\":{\"id\":\"bill_test\",\"items\":[{\"id\":\"prod_monthly\"}]},"
                : "";
        return json.readTree("{\"id\":\"" + id + "\",\"event\":\"" + type + "\",\"data\":{" + checkout
                + "\"subscription\":{\"id\":\"subs_test\",\"status\":\"ACTIVE\",\"frequency\":\"MONTHLY\",\"amount\":2990},"
                + "\"payment\":{\"status\":\"PAID\",\"amount\":2990,\"updatedAt\":\"" + time + "\"}}}");
    }
}
