package psg.facilitei.Services;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import psg.facilitei.DTO.AssinaturaPrestadorResponseDTO;
import psg.facilitei.Entity.AssinaturaPrestador;
import psg.facilitei.Entity.EventoPagamento;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Repository.AssinaturaPrestadorRepository;
import psg.facilitei.Repository.EventoPagamentoRepository;
import psg.facilitei.Repository.TrabalhadorRepository;

@Service
public class AssinaturaPrestadorService {
    private static final String PROVIDER = "MERCADOPAGO";
    private static final Set<String> SUPPORTED_TOPICS = Set.of(
            "subscription_preapproval", "subscription_authorized_payment");

    private final AssinaturaPrestadorRepository assinaturas;
    private final EventoPagamentoRepository eventos;
    private final MercadoPagoClient mercadoPago;
    private final TrabalhadorRepository trabalhadores;

    public AssinaturaPrestadorService(AssinaturaPrestadorRepository assinaturas,
                                      EventoPagamentoRepository eventos,
                                      MercadoPagoClient mercadoPago,
                                      TrabalhadorRepository trabalhadores) {
        this.assinaturas = assinaturas;
        this.eventos = eventos;
        this.mercadoPago = mercadoPago;
        this.trabalhadores = trabalhadores;
    }

    @Transactional(readOnly = true)
    public AssinaturaPrestadorResponseDTO consultar(Long trabalhadorId) {
        return assinaturas.findByTrabalhadorId(trabalhadorId)
                .map(this::resposta)
                .orElse(new AssinaturaPrestadorResponseDTO(
                        "NONE", false, null, null, null, mercadoPago.isConfigured()));
    }

    @Transactional
    public AssinaturaPrestadorResponseDTO iniciar(Trabalhador trabalhador) {
        trabalhadores.lockById(trabalhador.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Prestador não encontrado."));
        AssinaturaPrestador assinatura = assinaturas.findByTrabalhadorId(trabalhador.getId())
                .orElseGet(() -> novaAssinatura(trabalhador));

        if (PROVIDER.equals(assinatura.getProvider()) && "ACTIVE".equals(assinatura.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma assinatura ativa para este prestador.");
        }
        if (PROVIDER.equals(assinatura.getProvider())
                && assinatura.getSubscriptionId() != null
                && Set.of("PENDING", "PAUSED").contains(assinatura.getStatus())) {
            JsonNode remote = mercadoPago.getSubscription(assinatura.getSubscriptionId());
            String status = remote.path("status").asText();
            if (Set.of("pending", "authorized", "paused").contains(status)) {
                syncSubscription(assinatura, remote);
                return resposta(assinaturas.save(assinatura));
            }
        }

        MercadoPagoClient.Subscription remote = mercadoPago.createSubscription(
                trabalhador.getId(), trabalhador.getEmail());
        assinatura.setProvider(PROVIDER);
        assinatura.setCheckoutId(null);
        assinatura.setCheckoutUrl(remote.checkoutUrl());
        assinatura.setSubscriptionId(remote.id());
        assinatura.setAmountCents(remote.amountCents());
        assinatura.setActiveUntil(null);
        assinatura.setStatus("PENDING");
        return resposta(assinaturas.save(assinatura));
    }

    @Transactional
    public AssinaturaPrestadorResponseDTO cancelar(Long trabalhadorId) {
        AssinaturaPrestador assinatura = assinaturas.findByTrabalhadorId(trabalhadorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Assinatura não encontrada."));
        if (!PROVIDER.equals(assinatura.getProvider()) || assinatura.getSubscriptionId() == null
                || !Set.of("ACTIVE", "PENDING", "PAUSED").contains(assinatura.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não há assinatura do Mercado Pago para cancelar.");
        }
        mercadoPago.cancel(assinatura.getSubscriptionId());
        assinatura.setStatus("CANCELLED");
        assinatura.setActiveUntil(null);
        assinatura.setCheckoutUrl(null);
        return resposta(assinaturas.save(assinatura));
    }

    @Transactional(readOnly = true)
    public boolean podeUsarPlataforma(Long trabalhadorId) {
        return assinaturas.existsByTrabalhadorIdAndStatusAndActiveUntilAfter(
                trabalhadorId, "ACTIVE", Instant.now());
    }

    @Transactional
    public void aplicarEvento(String notificationId, String topic, String resourceId) {
        if (notificationId == null || notificationId.isBlank() || resourceId == null || resourceId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evento inválido.");
        }
        String eventKey = topic + ":" + notificationId;
        if (!SUPPORTED_TOPICS.contains(topic) || eventos.existsById(eventKey)) return;

        if ("subscription_preapproval".equals(topic)) {
            applySubscription(resourceId);
        } else {
            applyAuthorizedPayment(resourceId);
        }
        eventos.save(new EventoPagamento(eventKey));
    }

    private void applySubscription(String resourceId) {
        JsonNode remote = mercadoPago.getSubscription(resourceId);
        if (!resourceId.equals(remote.path("id").asText())) invalidRemoteData();
        AssinaturaPrestador assinatura = assinaturas.findBySubscriptionId(resourceId).orElse(null);
        if (assinatura == null || !PROVIDER.equals(assinatura.getProvider())) return;
        syncSubscription(assinatura, remote);
        assinaturas.save(assinatura);
    }

    private void applyAuthorizedPayment(String resourceId) {
        JsonNode payment = mercadoPago.getAuthorizedPayment(resourceId);
        if (!resourceId.equals(payment.path("id").asText())) invalidRemoteData();
        String subscriptionId = payment.path("preapproval_id").asText();
        AssinaturaPrestador assinatura = assinaturas.findBySubscriptionId(subscriptionId).orElse(null);
        if (assinatura == null || !PROVIDER.equals(assinatura.getProvider())) return;

        JsonNode subscription = mercadoPago.getSubscription(subscriptionId);
        validateSubscription(assinatura, subscription);
        long paidAmount = cents(payment.path("transaction_amount"));
        String paymentStatus = payment.path("payment").path("status").asText();
        if (paidAmount != assinatura.getAmountCents()) invalidRemoteData();

        if ("approved".equals(paymentStatus) && !"CANCELLED".equals(assinatura.getStatus())) {
            Instant paidAt = parseInstant(payment.path("debit_date").asText());
            Instant nextPayment = parseOptionalInstant(subscription.path("next_payment_date").asText());
            Instant newExpiration = nextPayment != null && nextPayment.isAfter(paidAt)
                    ? nextPayment : paidAt.atZone(ZoneOffset.UTC).plusMonths(1).toInstant();
            if (assinatura.getActiveUntil() == null || newExpiration.isAfter(assinatura.getActiveUntil())) {
                assinatura.setActiveUntil(newExpiration);
            }
            assinatura.setStatus("ACTIVE");
            assinatura.setCheckoutUrl(null);
        } else if (assinatura.getActiveUntil() == null || !assinatura.getActiveUntil().isAfter(Instant.now())) {
            assinatura.setStatus("EXPIRED");
        }
        assinaturas.save(assinatura);
    }

    private void syncSubscription(AssinaturaPrestador assinatura, JsonNode remote) {
        validateSubscription(assinatura, remote);
        String remoteStatus = remote.path("status").asText();
        if ("canceled".equals(remoteStatus)) {
            assinatura.setStatus("CANCELLED");
            assinatura.setActiveUntil(null);
            assinatura.setCheckoutUrl(null);
        } else if ("paused".equals(remoteStatus)) {
            assinatura.setStatus("PAUSED");
            assinatura.setActiveUntil(null);
        } else if ("pending".equals(remoteStatus) && !"ACTIVE".equals(assinatura.getStatus())) {
            assinatura.setStatus("PENDING");
        }
    }

    private void validateSubscription(AssinaturaPrestador assinatura, JsonNode remote) {
        JsonNode recurring = remote.path("auto_recurring");
        if (!assinatura.getSubscriptionId().equals(remote.path("id").asText())
                || !mercadoPago.externalReference(assinatura.getTrabalhador().getId())
                        .equals(remote.path("external_reference").asText())
                || recurring.path("frequency").asInt() != 1
                || !"months".equals(recurring.path("frequency_type").asText())
                || !"BRL".equals(recurring.path("currency_id").asText())
                || cents(recurring.path("transaction_amount")) != assinatura.getAmountCents()) {
            invalidRemoteData();
        }
    }

    private long cents(JsonNode amount) {
        try {
            java.math.BigDecimal value = amount.isNumber()
                    ? amount.decimalValue() : new java.math.BigDecimal(amount.asText());
            return value.movePointRight(2)
                    .setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException | NumberFormatException ex) {
            invalidRemoteData();
            return -1;
        }
    }

    private Instant parseInstant(String value) {
        Instant parsed = parseOptionalInstant(value);
        if (parsed == null) invalidRemoteData();
        return parsed;
    }

    private Instant parseOptionalInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return OffsetDateTime.parse(value).toInstant().truncatedTo(ChronoUnit.MILLIS);
        } catch (RuntimeException ex) {
            try {
                return Instant.parse(value).truncatedTo(ChronoUnit.MILLIS);
            } catch (RuntimeException ignored) {
                invalidRemoteData();
                return null;
            }
        }
    }

    private AssinaturaPrestador novaAssinatura(Trabalhador trabalhador) {
        AssinaturaPrestador assinatura = new AssinaturaPrestador();
        assinatura.setTrabalhador(trabalhador);
        assinatura.setProvider(PROVIDER);
        return assinatura;
    }

    private AssinaturaPrestadorResponseDTO resposta(AssinaturaPrestador assinatura) {
        boolean ativa = "ACTIVE".equals(assinatura.getStatus())
                && assinatura.getActiveUntil() != null
                && assinatura.getActiveUntil().isAfter(Instant.now());
        return new AssinaturaPrestadorResponseDTO(
                ativa || !"ACTIVE".equals(assinatura.getStatus()) ? assinatura.getStatus() : "EXPIRED",
                ativa, assinatura.getActiveUntil(), assinatura.getCheckoutUrl(), assinatura.getAmountCents(),
                mercadoPago.isConfigured());
    }

    private static void invalidRemoteData() {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Os dados da cobrança não correspondem ao plano do Facilitei.");
    }
}
