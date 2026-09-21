package psg.facilitei.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import psg.facilitei.DTO.AvaliacaoClienteRequestDTO;
import psg.facilitei.DTO.AvaliacaoClienteResponseDTO;
import psg.facilitei.Services.AvaliacaoClienteService;
import psg.facilitei.Security.AccessControlService;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/avaliacoes-cliente")
public class AvaliacaoClienteController {

    @Autowired
    private AvaliacaoClienteService avaliacaoClienteService;

    @Autowired
    private AccessControlService access;

    @PostMapping
    public ResponseEntity<AvaliacaoClienteResponseDTO> criarAvaliacao(
            @Valid @RequestBody AvaliacaoClienteRequestDTO dto) {
        access.requireTrabalhador(dto.getTrabalhadorId());
        if (dto.getServicoId() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "O serviço é obrigatório.");
        }
        access.requireServiceWorker(dto.getServicoId());
        access.requireServiceParties(dto.getServicoId(), dto.getClienteId(), dto.getTrabalhadorId());
        AvaliacaoClienteResponseDTO response = avaliacaoClienteService.criarAvaliacao(dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<AvaliacaoClienteResponseDTO>> listarPorCliente(@PathVariable Long clienteId) {
        List<AvaliacaoClienteResponseDTO> avaliacoes = avaliacaoClienteService.listarPorCliente(clienteId);
        return ResponseEntity.ok(avaliacoes);
    }

    @GetMapping("/trabalhador/{trabalhadorId}")
    public ResponseEntity<List<AvaliacaoClienteResponseDTO>> listarPorTrabalhador(@PathVariable Long trabalhadorId) {
        List<AvaliacaoClienteResponseDTO> avaliacoes = avaliacaoClienteService.listarPorTrabalhador(trabalhadorId);
        return ResponseEntity.ok(avaliacoes);
    }

    @DeleteMapping("/{avaliacaoId}")
    public ResponseEntity<Void> deletarAvaliacao(@PathVariable Long avaliacaoId) {
        access.requireClientReviewAuthor(avaliacaoId);
        avaliacaoClienteService.deletarAvaliacao(avaliacaoId);
        return ResponseEntity.noContent().build();
    }
}
