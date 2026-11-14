
package psg.facilitei.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import psg.facilitei.Repository.TrabalhadorRepository;
import psg.facilitei.DTO.TrabalhadorRequestDTO;
import psg.facilitei.DTO.TrabalhadorPageResponseDTO;
import psg.facilitei.DTO.TrabalhadorResponseDTO;
import psg.facilitei.DTO.TrabalhadorUpdateDTO;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Entity.Enum.TipoServico;
import psg.facilitei.Services.TrabalhadorService;
import psg.facilitei.Exceptions.ErrorResponseDTO;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Security.AccessControlService;

@RestController
@RequestMapping("/api/trabalhadores")
@Tag(name = "Trabalhador", description = "Operações relacionadas aos trabalhadores")
public class TrabalhadorController {

        @Autowired
        private TrabalhadorService service;

        @Autowired
        private TrabalhadorRepository repository;

        @Autowired
        private AccessControlService access;

        @Operation(summary = "Cria um novo trabalhador", description = "Registra um trabalhador no sistema com os dados fornecidos.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Trabalhador criado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TrabalhadorResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Dados inválidos ou incompletos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @PostMapping
        public ResponseEntity<TrabalhadorResponseDTO> CreateTrabalhador(
                        @Valid @RequestBody TrabalhadorRequestDTO trabalhadorRequestDTO,
                        HttpServletRequest request) {
                TrabalhadorResponseDTO createdTrabalhador = service.createTrabalhador(
                                trabalhadorRequestDTO,
                                request.getRemoteAddr(),
                                request.getHeader("User-Agent"));
                return ResponseEntity.status(201).body(createdTrabalhador);
        }

        @Operation(summary = "Lista todos os trabalhadores", description = "Retorna uma lista com todos os trabalhadores cadastrados no sistema.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TrabalhadorResponseDTO.class)))
        })
        @GetMapping("/listar")
        public List<TrabalhadorResponseDTO> listarTodos() {
                return service.findAll().stream().map(this::paraVisitante).toList();
        }

        @Operation(summary = "Lista trabalhadores com filtros e paginação")
        @GetMapping("/listar/paginado")
        public TrabalhadorPageResponseDTO listarPaginado(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "12") int size,
                        @RequestParam(required = false) String nome,
                        @RequestParam(required = false) String localizacao,
                        @RequestParam(required = false) List<TipoServico> tiposServico,
                        @RequestParam(defaultValue = "0") Double notaMinima) {
                if (page < 0 || size < 1 || size > 100 || notaMinima < 0 || notaMinima > 5) {
                        throw new BusinessRuleException("Parâmetros de paginação ou nota inválidos");
                }
                TrabalhadorPageResponseDTO resultado =
                                service.findAllPaginado(page, size, nome, localizacao, tiposServico, notaMinima);
                return new TrabalhadorPageResponseDTO(
                                resultado.content().stream().map(this::paraVisitante).toList(),
                                resultado.totalElements(), resultado.totalPages(), resultado.page(),
                                resultado.size(), resultado.first(), resultado.last());
        }

        @Operation(summary = "Atualiza um trabalhador existente", description = "Atualiza os dados de um trabalhador com base no ID fornecido.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Trabalhador atualizado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TrabalhadorResponseDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Trabalhador não encontrado"),
                        @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @PutMapping("/atualizar/{id}")
        public TrabalhadorResponseDTO atualizarTrabalhador(@PathVariable Long id,
                        @Valid @RequestBody TrabalhadorUpdateDTO dto) {
                access.requireTrabalhador(id);
                return service.atualizar(id, dto);
        }

        @Operation(summary = "Deleta um trabalhador", description = "Remove um trabalhador do sistema com base no ID fornecido.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Trabalhador deletado com sucesso"),
                        @ApiResponse(responseCode = "404", description = "Trabalhador não encontrado"),
                        @ApiResponse(responseCode = "500", description = "Erro interno do servidor.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @DeleteMapping("/delete/{id}")
        public ResponseEntity<Void> deletarTrabalhador(@PathVariable Long id) {
                access.requireTrabalhador(id);
                service.delete(id);
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Buscar trabalhador por ID", description = "Retorna os dados de um trabalhador específico com base no ID fornecido.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Trabalhador encontrado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TrabalhadorResponseDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Trabalhador não encontrado"),
                        @ApiResponse(responseCode = "500", description = "Erro interno do servidor.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @GetMapping("buscarPorId/{id}")
        public ResponseEntity<TrabalhadorResponseDTO> buscarPorId(@PathVariable Long id) {
                TrabalhadorResponseDTO dto = service.findById(id);
                return ResponseEntity.ok(paraVisitante(dto));
        }


        /**
         * Perfil público: quem não é o próprio profissional (nem admin) não vê e-mail,
         * telefone nem endereço completo — só bairro, cidade e estado. O contato acontece
         * pelo chat depois que o pedido é aceito.
         */
        private TrabalhadorResponseDTO paraVisitante(TrabalhadorResponseDTO dto) {
                if (dto == null || podeVerDadosPrivados(dto.getId())) return dto;
                TrabalhadorResponseDTO publico = new TrabalhadorResponseDTO();
                publico.setId(dto.getId());
                publico.setNome(dto.getNome());
                publico.setDisponibilidade(dto.getDisponibilidade());
                publico.setServicos(dto.getServicos());
                publico.setServicoPrincipal(dto.getServicoPrincipal());
                publico.setNotaTrabalhador(dto.getNotaTrabalhador());
                publico.setAvaliacoesPorServico(dto.getAvaliacoesPorServico());
                publico.setSobre(dto.getSobre());
                publico.setAvatarUrl(dto.getAvatarUrl());
                if (dto.getEndereco() != null) {
                        psg.facilitei.DTO.EnderecoResponseDTO local = new psg.facilitei.DTO.EnderecoResponseDTO();
                        local.setBairro(dto.getEndereco().getBairro());
                        local.setCidade(dto.getEndereco().getCidade());
                        local.setEstado(dto.getEndereco().getEstado());
                        publico.setEndereco(local);
                }
                return publico;
        }

        private boolean podeVerDadosPrivados(String trabalhadorId) {
                var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !(auth.getPrincipal() instanceof psg.facilitei.Security.AuthenticatedPrincipal actor)) {
                        return false;
                }
                boolean admin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
                return admin || (actor.isTrabalhador() && String.valueOf(actor.id()).equals(trabalhadorId));
        }
}
