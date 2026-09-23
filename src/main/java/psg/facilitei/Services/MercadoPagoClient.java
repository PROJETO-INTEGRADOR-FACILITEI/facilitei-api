package psg.facilitei.Services;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MercadoPagoClient {
    private final RestClient restClient;
    private final String accessToken;
    private final long monthlyAmountCents;
    private final String reason;
    private final String returnUrl;

    public MercadoPagoClient(@Value("${mercadopago.base-url:https://api.mercadopago.com}") String baseUrl,
                             @Value("${mercadopago.access-token:}") String accessToken,
                             @Value("${mercadopago.monthly-amount-cents:0}") long monthlyAmountCents,
                             @Value("${mercadopago.reason:Facilitei - plano profissional}") String reason,
                             @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(15_000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.accessToken = accessToken == null ? "" : accessToken.trim();
        this.monthlyAmountCents = monthlyAmountCents;
        this.reason = reason;
        this.returnUrl = frontendUrl.replaceAll("/+$", "") + "/painel/assinatura";
    }

    public Subscription createSubscription(Long trabalhadorId, String payerEmail) {
        requireConfiguration();
        Map<String, Object> recurring = Map.of(
                "frequency", 1,
                "frequency_type", "months",
                "transaction_amount", BigDecimal.valueOf(monthlyAmountCents, 2),
                "currency_id", "BRL");
        Map<String, Object> body = Map.of(
                "reason", reason,
                "external_reference", externalReference(trabalhadorId),
                "payer_email", payerEmail,
                "auto_recurring", recurring,
                "back_url", returnUrl,
                "status", "pending");
        JsonNode response = post("/preapproval", body, UUID.randomUUID().toString());
        String id = response.path("id").asText();
        String checkoutUrl = response.path("init_point").asText();
        if (id.isBlank() || !checkoutUrl.startsWith("https://")) {
            throw badGateway("O Mercado Pago retornou uma assinatura inválida.");
        }
        return new Subscription(id, checkoutUrl, monthlyAmountCents);
    }

    public JsonNode getSubscription(String subscriptionId) {
        return get("/preapproval/{id}", subscriptionId);
    }

    public JsonNode getAuthorizedPayment(String paymentId) {
        return get("/authorized_payments/{id}", paymentId);
    }

    public void cancel(String subscriptionId) {
        put("/preapproval/{id}", subscriptionId, Map.of("status", "canceled"));
    }

    public boolean isConfigured() {
        return !accessToken.isBlank() && monthlyAmountCents > 0;
    }

    public long monthlyAmountCents() {
        return monthlyAmountCents;
    }

    public String externalReference(Long trabalhadorId) {
        return "facilitei-worker-" + trabalhadorId;
    }

    private JsonNode post(String path, Object body, String idempotencyKey) {
        requireConfiguration();
        try {
            JsonNode response = restClient.post().uri(path)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .body(body).retrieve().body(JsonNode.class);
            if (response == null) throw badGateway("O Mercado Pago retornou uma resposta vazia.");
            return response;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível comunicar com o Mercado Pago.", ex);
        }
    }

    private JsonNode get(String path, String id) {
        requireConfiguration();
        try {
            JsonNode response = restClient.get().uri(path, id)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve().body(JsonNode.class);
            if (response == null) throw badGateway("O Mercado Pago retornou uma resposta vazia.");
            return response;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível consultar o Mercado Pago.", ex);
        }
    }

    private void put(String path, String id, Object body) {
        requireConfiguration();
        try {
            restClient.put().uri(path, id)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(body).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível atualizar a assinatura no Mercado Pago.", ex);
        }
    }

    private void requireConfiguration() {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Assinaturas do Mercado Pago ainda não configuradas.");
        }
    }

    private ResponseStatusException badGateway(String message) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
    }

    public record Subscription(String id, String checkoutUrl, long amountCents) {}
}
