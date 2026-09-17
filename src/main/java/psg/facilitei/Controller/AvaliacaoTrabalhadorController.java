package psg.facilitei.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import psg.facilitei.DTO.AvaliacaoTrabalhadorRequestDTO;
import psg.facilitei.DTO.AvaliacaoTrabalhadorResponseDTO;
import psg.facilitei.Services.AvaliacaoTrabalhadorService;

import java.util.List;

@RestController
@RequestMapping("/api/avaliacoes-trabalhador") // URL corrigida para bater com o Front
public class AvaliacaoTrabalhadorController {

    @Autowired
    private AvaliacaoTrabalhadorService avaliacaoTrabalhadorService;

    @PostMapping
    public ResponseEntity<AvaliacaoTrabalhadorResponseDTO> criar(@RequestBody AvaliacaoTrabalhadorRequestDTO dto) {
        return ResponseEntity.ok(avaliacaoTrabalhadorService.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<AvaliacaoTrabalhadorResponseDTO>> listar(@RequestParam Long trabalhadorId) {
        return ResponseEntity.ok(avaliacaoTrabalhadorService.listarPorTrabalhador(trabalhadorId));
    }
}
