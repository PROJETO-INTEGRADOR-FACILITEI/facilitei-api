package psg.facilitei.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import psg.facilitei.DTO.AvaliacaoTrabalhadorRequestDTO;
import psg.facilitei.DTO.AvaliacaoTrabalhadorResponseDTO;
import psg.facilitei.Services.AvaliacaoTrabalhadorService;
import psg.facilitei.Security.AccessControlService;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/avaliacoes-trabalhador") // URL corrigida para bater com o Front
public class AvaliacaoTrabalhadorController {

    @Autowired
    private AvaliacaoTrabalhadorService avaliacaoTrabalhadorService;

    @Autowired
    private AccessControlService access;

    @PostMapping
    public ResponseEntity<AvaliacaoTrabalhadorResponseDTO> criar(@Valid @RequestBody AvaliacaoTrabalhadorRequestDTO dto) {
        access.requireCliente(dto.getClienteId());
        if (dto.getServicoId() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "O serviço é obrigatório.");
        }
        access.requireServiceClient(dto.getServicoId());
        access.requireServiceParties(dto.getServicoId(), dto.getClienteId(), dto.getTrabalhadorId());
        return ResponseEntity.ok(avaliacaoTrabalhadorService.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<AvaliacaoTrabalhadorResponseDTO>> listar(@RequestParam Long trabalhadorId) {
        return ResponseEntity.ok(avaliacaoTrabalhadorService.listarPorTrabalhador(trabalhadorId));
    }
}
