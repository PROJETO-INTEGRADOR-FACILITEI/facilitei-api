package psg.facilitei.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import psg.facilitei.DTO.AssinaturaPrestadorResponseDTO;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Services.AbacatePayWebhookVerifier;
import psg.facilitei.Services.AssinaturaPrestadorService;
import psg.facilitei.Services.AutenticacaoPrestador;

@RestController
@RequestMapping("/api/assinaturas/prestador")
public class AssinaturaPrestadorController {
    private final AutenticacaoPrestador autenticacao;
    private final AssinaturaPrestadorService assinaturas;
    private final AbacatePayWebhookVerifier verifier;
    private final ObjectMapper objectMapper;

    public AssinaturaPrestadorController(AutenticacaoPrestador autenticacao,
                                        AssinaturaPrestadorService assinaturas,
                                        AbacatePayWebhookVerifier verifier,
                                        ObjectMapper objectMapper) {
        this.autenticacao = autenticacao;
        this.assinaturas = assinaturas;
        this.verifier = verifier;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public AssinaturaPrestadorResponseDTO consultar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest request) {
        Trabalhador trabalhador = autenticar(authorization, request);
        return assinaturas.consultar(trabalhador.getId());
    }

    @PostMapping("/checkout")
    public AssinaturaPrestadorResponseDTO iniciar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest request) {
        return assinaturas.iniciar(autenticar(authorization, request));
    }

    @PostMapping("/cancelar")
    public AssinaturaPrestadorResponseDTO cancelar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest request) {
        return assinaturas.cancelar(autenticar(authorization, request).getId());
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestParam(value = "webhookSecret", required = false) String secret,
                                        @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
                                        @RequestBody byte[] rawBody) {
        verifier.verify(secret, signature, rawBody);
        try {
            JsonNode payload = objectMapper.readTree(rawBody);
            assinaturas.aplicarEvento(payload);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON do webhook inválido.");
        }
        return ResponseEntity.ok().build();
    }

    private Trabalhador autenticar(String authorization, HttpServletRequest request) {
        return autenticacao.autenticar(authorization, request.getSession(false));
    }
}
