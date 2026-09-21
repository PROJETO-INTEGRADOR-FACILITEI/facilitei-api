package psg.facilitei.Controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import psg.facilitei.DTO.AvaliacaoServicoRequestDTO;
import psg.facilitei.DTO.AvaliacaoServicoResponseDTO;
import psg.facilitei.Services.AvaliacaoServicoService;
import psg.facilitei.Security.AccessControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/avaliacoes-servico")
@Tag(name = "Avaliações", description = "Endpoints relacionados às avaliações de serviço")
public class AvaliacaoServicoController {

    @Autowired
    private AvaliacaoServicoService service;

    @Autowired
    private AccessControlService access;

    @PostMapping("/Criar")
    public ResponseEntity<AvaliacaoServicoResponseDTO> criarAvaliacao(@Valid @RequestBody AvaliacaoServicoRequestDTO requestDTO) {
        access.requireCliente(requestDTO.getClienteId());
        access.requireServiceClient(requestDTO.getServicoId());
        return ResponseEntity.status(201).body(service.create(requestDTO));
    }

    @GetMapping("/{servicoId}")
    public ResponseEntity<List<AvaliacaoServicoResponseDTO>> listarAvaliacoesPorServico(@PathVariable Long servicoId) {
        access.requireServiceParticipant(servicoId);
        return ResponseEntity.ok(service.buscarAvaliacoesPorServico(servicoId));
    }

    // NOVO ENDPOINT: Busca todas as avaliações recebidas por um trabalhador
    @GetMapping("/trabalhador/{trabalhadorId}")
    public ResponseEntity<List<AvaliacaoServicoResponseDTO>> listarPorTrabalhador(@PathVariable Long trabalhadorId) {
        return ResponseEntity.ok(service.buscarAvaliacoesPorTrabalhador(trabalhadorId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarAvaliacao(@PathVariable Long id) {
        access.requireServiceReviewAuthor(id);
        service.deletarAvaliacao(id);
        return ResponseEntity.noContent().build();
    }
}
