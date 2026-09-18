package psg.facilitei.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import psg.facilitei.DTO.PortfolioRequestDTO;
import psg.facilitei.DTO.PortfolioResponseDTO;
import psg.facilitei.Services.PortfolioService;
import psg.facilitei.Entity.Enum.TipoServico;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
@Tag(name = "Portfolio", description = "Operações relacionadas ao portfolio de fotos dos trabalhadores")
public class PortfolioController {

    @Autowired
    private PortfolioService portfolioService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cria o portfolio de um trabalhador enviando as imagens para o Cloudinary", responses = {
            @ApiResponse(responseCode = "201", description = "Portfolio criado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PortfolioResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "404", description = "Trabalhador não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<PortfolioResponseDTO> criar(@Valid @ModelAttribute PortfolioRequestDTO dto) {
        PortfolioResponseDTO criado = portfolioService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Busca um portfolio por ID", responses = {
            @ApiResponse(responseCode = "200", description = "Portfolio encontrado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PortfolioResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Portfolio não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<PortfolioResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(portfolioService.buscarPorId(id));
    }

    @GetMapping(value = "/trabalhador/{trabalhadorId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Busca o portfolio de um trabalhador", responses = {
            @ApiResponse(responseCode = "200", description = "Portfolio encontrado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PortfolioResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Portfolio não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<PortfolioResponseDTO> buscarPorTrabalhador(@PathVariable Long trabalhadorId) {
        return ResponseEntity.ok(portfolioService.buscarPorTrabalhador(trabalhadorId));
    }

    @PostMapping(value = "/{id}/imagens", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Adiciona novas imagens a um portfolio existente", responses = {
            @ApiResponse(responseCode = "200", description = "Imagens adicionadas com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PortfolioResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "404", description = "Portfolio não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<PortfolioResponseDTO> adicionarImagens(@PathVariable Long id,
            @RequestParam("imagens") List<MultipartFile> imagens,
            @RequestParam("tipoServico") TipoServico tipoServico) {
        return ResponseEntity.ok(portfolioService.adicionarImagens(id, imagens, tipoServico));
    }

    @DeleteMapping("/{portfolioId}/imagens/{imagemId}")
    @Operation(summary = "Remove uma imagem do portfolio e do Cloudinary", responses = {
            @ApiResponse(responseCode = "204", description = "Imagem removida com sucesso"),
            @ApiResponse(responseCode = "404", description = "Portfolio ou imagem não encontrado")
    })
    public ResponseEntity<Void> removerImagem(@PathVariable Long portfolioId, @PathVariable Long imagemId) {
        portfolioService.removerImagem(portfolioId, imagemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deleta um portfolio por ID", responses = {
            @ApiResponse(responseCode = "204", description = "Portfolio deletado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Portfolio não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        portfolioService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
