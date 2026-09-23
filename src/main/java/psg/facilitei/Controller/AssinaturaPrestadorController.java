package psg.facilitei.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import psg.facilitei.DTO.AssinaturaPrestadorResponseDTO;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Services.MercadoPagoWebhookVerifier;
import psg.facilitei.Services.AssinaturaPrestadorService;
import psg.facilitei.Services.TrabalhadorService;
import psg.facilitei.Security.AccessControlService;
import psg.facilitei.Security.AuthenticatedPrincipal;

@RestController
@RequestMapping("/api/assinaturas/prestador")
public class AssinaturaPrestadorController {
    private final AccessControlService access;
    private final TrabalhadorService trabalhadores;
    private final AssinaturaPrestadorService assinaturas;
    private final MercadoPagoWebhookVerifier verifier;
    private final ObjectMapper objectMapper;

    public AssinaturaPrestadorController(AccessControlService access,
                                        TrabalhadorService trabalhadores,
                                        AssinaturaPrestadorService assinaturas,
                                        MercadoPagoWebhookVerifier verifier,
                                        ObjectMapper objectMapper) {
        this.access = access;
        this.trabalhadores = trabalhadores;
        this.assinaturas = assinaturas;
        this.verifier = verifier;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public AssinaturaPrestadorResponseDTO consultar() {
        return assinaturas.consultar(profissionalAutenticado().id());
    }

    @PostMapping("/checkout")
    public AssinaturaPrestadorResponseDTO iniciar() {
        AuthenticatedPrincipal actor = profissionalAutenticado();
        Trabalhador trabalhador = trabalhadores.buscarEntidadePorId(actor.id());
        return assinaturas.iniciar(trabalhador);
    }

    @PostMapping("/cancelar")
    public AssinaturaPrestadorResponseDTO cancelar() {
        return assinaturas.cancelar(profissionalAutenticado().id());
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestParam(value = "data.id") String dataId,
                                        @RequestParam(value = "type") String type,
                                        @RequestHeader(value = "X-Signature", required = false) String signature,
                                        @RequestHeader(value = "X-Request-Id", required = false) String requestId,
                                        @RequestBody byte[] rawBody) {
        verifier.verify(signature, requestId, dataId);
        try {
            JsonNode payload = objectMapper.readTree(rawBody);
            assinaturas.aplicarEvento(payload.path("id").asText(), type, dataId);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON do webhook inválido.");
        }
        return ResponseEntity.ok().build();
    }

    private AuthenticatedPrincipal profissionalAutenticado() {
        AuthenticatedPrincipal actor = access.current();
        access.requireTrabalhador(actor.id());
        return actor;
    }
}
