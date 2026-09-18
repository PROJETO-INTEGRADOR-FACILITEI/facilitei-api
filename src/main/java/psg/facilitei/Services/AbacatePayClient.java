package psg.facilitei.Services;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AbacatePayClient {
    private final RestClient restClient;
    private final String apiKey;
    private final String productId;
    private final String checkoutReturnUrl;

    public AbacatePayClient(@Value("${abacatepay.base-url:https://api.abacatepay.com/v2}") String baseUrl,
                            @Value("${abacatepay.api-key:}") String apiKey,
                            @Value("${abacatepay.product-id:}") String productId,
                            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(15_000);
        this.restClient = RestClient.builder().baseUrl(baseUrl)
                .requestFactory(requestFactory).build();
        this.apiKey = apiKey;
        this.productId = productId;
        this.checkoutReturnUrl = frontendUrl.replaceAll("/+$", "") + "/dashboard/assinatura";
    }

    public String productId() { return productId; }

    public Checkout criarCheckout(Long trabalhadorId) {
        if (apiKey.isBlank() || productId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Assinaturas ainda não configuradas.");
        }
        String externalId = "facilitei-worker-" + trabalhadorId + "-" + UUID.randomUUID();
        JsonNode data = post("/subscriptions/create", Map.of(
                "items", List.of(Map.of("id", productId, "quantity", 1)),
                "methods", List.of("CARD"),
                "externalId", externalId,
                "returnUrl", checkoutReturnUrl,
                "completionUrl", checkoutReturnUrl));
        String id = data.path("id").asText();
        String url = data.path("url").asText();
        long amount = data.path("amount").asLong();
        if (id.isBlank() || !url.startsWith("https://") || amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Resposta inválida da AbacatePay.");
        }
        return new Checkout(id, url, amount);
    }

    public String statusCheckout(String checkoutId) {
        JsonNode response = get("/subscriptions/list?id={id}", checkoutId);
        JsonNode data = response.path("data");
        if (!data.isArray() || data.isEmpty() || !checkoutId.equals(data.get(0).path("id").asText())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Checkout não encontrado na AbacatePay.");
        }
        return data.get(0).path("status").asText();
    }

    public void cancelar(String subscriptionId) {
        post("/subscriptions/cancel", Map.of("id", subscriptionId));
    }

    private JsonNode post(String path, Object body) {
        requireKey();
        try {
            JsonNode response = restClient.post().uri(path)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body).retrieve().body(JsonNode.class);
            if (response == null || !response.path("success").asBoolean(false)
                    || response.path("data").isMissingNode() || response.path("data").isNull()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "AbacatePay recusou a operação.");
            }
            return response.path("data");
        } catch (org.springframework.web.client.RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível comunicar com a AbacatePay.", ex);
        }
    }

    private JsonNode get(String path, String id) {
        requireKey();
        try {
            JsonNode response = restClient.get().uri(path, id)
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve().body(JsonNode.class);
            if (response == null || !response.path("success").asBoolean(false)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Não foi possível consultar a AbacatePay.");
            }
            return response;
        } catch (org.springframework.web.client.RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível comunicar com a AbacatePay.", ex);
        }
    }

    private void requireKey() {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AbacatePay ainda não configurada.");
        }
    }

    public record Checkout(String id, String url, long amountCents) {}
}
