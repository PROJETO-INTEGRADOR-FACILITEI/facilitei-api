package psg.facilitei.Services;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import psg.facilitei.DTO.AssinaturaPrestadorResponseDTO;
import psg.facilitei.Entity.AssinaturaPrestador;
import psg.facilitei.Entity.EventoAbacatePay;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Repository.AssinaturaPrestadorRepository;
import psg.facilitei.Repository.EventoAbacatePayRepository;
import psg.facilitei.Repository.TrabalhadorRepository;

@Service
public class AssinaturaPrestadorService {
    private static final Set<String> EVENTS = Set.of(
            "subscription.completed", "subscription.renewed",
            "subscription.payment_failed", "subscription.cancelled");

    private final AssinaturaPrestadorRepository assinaturas;
    private final EventoAbacatePayRepository eventos;
    private final AbacatePayClient abacatePay;
    private final TrabalhadorRepository trabalhadores;

    public AssinaturaPrestadorService(AssinaturaPrestadorRepository assinaturas,
                                     EventoAbacatePayRepository eventos,
                                     AbacatePayClient abacatePay,
                                     TrabalhadorRepository trabalhadores) {
        this.assinaturas = assinaturas;
        this.eventos = eventos;
        this.abacatePay = abacatePay;
        this.trabalhadores = trabalhadores;
    }

    @Transactional(readOnly = true)
    public AssinaturaPrestadorResponseDTO consultar(Long trabalhadorId) {
        return assinaturas.findByTrabalhadorId(trabalhadorId)
                .map(this::resposta)
                .orElse(new AssinaturaPrestadorResponseDTO(
                        "NONE", false, null, null, null, cobrancaHabilitada()));
    }

    @Transactional
    public AssinaturaPrestadorResponseDTO iniciar(Trabalhador trabalhador) {
        trabalhadores.lockById(trabalhador.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Prestador não encontrado."));
        AssinaturaPrestador assinatura = assinaturas.findByTrabalhadorId(trabalhador.getId())
                .orElseGet(() -> {
                    AssinaturaPrestador nova = new AssinaturaPrestador();
                    nova.setTrabalhador(trabalhador);
                    return nova;
                });

        if ("ACTIVE".equals(assinatura.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma assinatura ativa para este prestador.");
        }
        if ("PENDING".equals(assinatura.getStatus()) && assinatura.getCheckoutId() != null) {
            String statusRemoto = abacatePay.statusCheckout(assinatura.getCheckoutId());
            if ("PENDING".equals(statusRemoto)) return resposta(assinatura);
            if ("PAID".equals(statusRemoto)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Pagamento confirmado; aguardando confirmação da assinatura.");
            }
            if (!Set.of("EXPIRED", "CANCELLED", "REFUNDED").contains(statusRemoto)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Status do checkout não reconhecido.");
            }
        }

        AbacatePayClient.Checkout checkout = abacatePay.criarCheckout(trabalhador.getId());
        assinatura.setCheckoutId(checkout.id());
        assinatura.setCheckoutUrl(checkout.url());
        assinatura.setAmountCents(checkout.amountCents());
        assinatura.setSubscriptionId(null);
        assinatura.setActiveUntil(null);
        assinatura.setStatus("PENDING");
        return resposta(assinaturas.save(assinatura));
    }

    @Transactional
    public AssinaturaPrestadorResponseDTO cancelar(Long trabalhadorId) {
        AssinaturaPrestador assinatura = assinaturas.findByTrabalhadorId(trabalhadorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Assinatura não encontrada."));
        if (!"ACTIVE".equals(assinatura.getStatus()) || assinatura.getSubscriptionId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não há assinatura ativa para cancelar.");
        }
        abacatePay.cancelar(assinatura.getSubscriptionId());
        assinatura.setStatus("CANCELLED");
        assinatura.setActiveUntil(null);
        return resposta(assinaturas.save(assinatura));
    }

    @Transactional(readOnly = true)
    public boolean podeUsarPlataforma(Long trabalhadorId) {
        return assinaturas.existsByTrabalhadorIdAndStatusAndActiveUntilAfter(
                trabalhadorId, "ACTIVE", Instant.now());
    }

    @Transactional
    public void aplicarEvento(JsonNode payload) {
        String id = payload.path("id").asText();
        String event = payload.path("event").asText();
        if (id.isBlank() || !id.startsWith("log_") || event.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evento inválido.");
        }
        if (!EVENTS.contains(event) || eventos.existsById(id)) return;

        JsonNode data = payload.path("data");
        String subscriptionId = data.path("subscription").path("id").asText();
        AssinaturaPrestador assinatura;
        if ("subscription.completed".equals(event)) {
            String checkoutId = data.path("checkout").path("id").asText();
            assinatura = assinaturas.findByCheckoutId(checkoutId).orElse(null);
        } else {
            assinatura = assinaturas.findBySubscriptionId(subscriptionId).orElse(null);
        }
        if (assinatura == null) {
            // Um checkout substituído ou uma assinatura já removida não pode alterar acesso.
            eventos.save(new EventoAbacatePay(id));
            return;
        }

        if ("subscription.cancelled".equals(event)) {
            assinatura.setStatus("CANCELLED");
            assinatura.setActiveUntil(null);
        } else if ("subscription.completed".equals(event) || "subscription.renewed".equals(event)) {
            if ("CANCELLED".equals(assinatura.getStatus())) {
                eventos.save(new EventoAbacatePay(id));
                return;
            }
            validarPagamento(data, assinatura, "subscription.completed".equals(event));
            if ("subscription.completed".equals(event)) assinatura.setSubscriptionId(subscriptionId);
            assinatura.setAmountCents(data.path("subscription").path("amount").asLong());
            Instant pagoEm;
            try {
                pagoEm = Instant.parse(data.path("payment").path("updatedAt").asText());
            } catch (RuntimeException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data de pagamento inválida.");
            }
            Instant novaValidade = pagoEm.atZone(ZoneOffset.UTC).plusMonths(1).toInstant();
            if (assinatura.getActiveUntil() == null || novaValidade.isAfter(assinatura.getActiveUntil())) {
                assinatura.setActiveUntil(novaValidade);
            }
            assinatura.setStatus("ACTIVE");
        }
        assinaturas.save(assinatura);
        eventos.save(new EventoAbacatePay(id));
    }

    private void validarPagamento(JsonNode data, AssinaturaPrestador assinatura, boolean primeiraCobranca) {
        JsonNode subscription = data.path("subscription");
        JsonNode payment = data.path("payment");
        if (!"ACTIVE".equals(subscription.path("status").asText())
                || !"MONTHLY".equals(subscription.path("frequency").asText())
                || !"PAID".equals(payment.path("status").asText())
                || subscription.path("amount").asLong(-1) <= 0
                || payment.path("amount").asLong(-1) != subscription.path("amount").asLong(-1)
                || (primeiraCobranca && subscription.path("amount").asLong(-1) != assinatura.getAmountCents())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Dados do pagamento não correspondem ao plano mensal.");
        }
        if (primeiraCobranca) {
            JsonNode items = data.path("checkout").path("items");
            if (items.size() != 1 || !abacatePay.productId().equals(items.get(0).path("id").asText())
                    || subscription.path("id").asText().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Produto ou assinatura inválida.");
            }
        }
    }

    private AssinaturaPrestadorResponseDTO resposta(AssinaturaPrestador assinatura) {
        boolean ativa = "ACTIVE".equals(assinatura.getStatus())
                && assinatura.getActiveUntil() != null
                && assinatura.getActiveUntil().isAfter(Instant.now());
        return new AssinaturaPrestadorResponseDTO(
                ativa || !"ACTIVE".equals(assinatura.getStatus()) ? assinatura.getStatus() : "EXPIRED",
                ativa, assinatura.getActiveUntil(), assinatura.getCheckoutUrl(), assinatura.getAmountCents(),
                cobrancaHabilitada());
    }

    private boolean cobrancaHabilitada() {
        return !abacatePay.productId().isBlank();
    }
}
