package psg.facilitei.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import psg.facilitei.Entity.ServicoOferecido;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Entity.Enum.TipoServico;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Exceptions.ResourceNotFoundException;
import psg.facilitei.Repository.ServicoOferecidoRepository;
import psg.facilitei.Repository.TrabalhadorRepository;
import psg.facilitei.Security.AccessControlService;
import psg.facilitei.Security.AuthenticatedPrincipal;
import psg.facilitei.Util.HtmlSanitizer;

/** Serviços criados pelo próprio profissional, além das categorias pré-definidas. */
@RestController
@RequestMapping("/api/servicos-oferecidos")
@Tag(name = "Serviços do profissional", description = "Serviços personalizados criados pelo profissional")
public class ServicoOferecidoController {
    static final int LIMITE_POR_PROFISSIONAL = 30;

    public record Request(
            @NotBlank(message = "Informe o nome do serviço.")
            @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.") String titulo,
            @NotBlank(message = "Descreva o serviço.")
            @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.") String descricao,
            @DecimalMin(value = "0.0", message = "O preço não pode ser negativo.")
            @DecimalMax(value = "99999999.99", message = "Preço muito alto.") BigDecimal preco,
            TipoServico tipoServico) {}

    public record Response(Long id, Long trabalhadorId, String titulo, String descricao,
                           BigDecimal preco, TipoServico tipoServico) {}

    private final ServicoOferecidoRepository repository;
    private final TrabalhadorRepository trabalhadores;
    private final AccessControlService access;

    public ServicoOferecidoController(ServicoOferecidoRepository repository,
                                      TrabalhadorRepository trabalhadores,
                                      AccessControlService access) {
        this.repository = repository;
        this.trabalhadores = trabalhadores;
        this.access = access;
    }

    @GetMapping("/trabalhador/{trabalhadorId}")
    @Operation(summary = "Lista os serviços personalizados ativos de um profissional (público)")
    @Transactional(readOnly = true)
    public List<Response> listar(@PathVariable Long trabalhadorId) {
        return repository.findByTrabalhadorIdAndAtivoTrueOrderByCreatedAtDesc(trabalhadorId)
                .stream().map(ServicoOferecidoController::toResponse).toList();
    }

    @PostMapping
    @Operation(summary = "Profissional cria um serviço próprio")
    @Transactional
    public ResponseEntity<Response> criar(@Valid @RequestBody Request request) {
        AuthenticatedPrincipal actor = requireWorker();
        if (repository.countByTrabalhadorIdAndAtivoTrue(actor.id()) >= LIMITE_POR_PROFISSIONAL) {
            throw new BusinessRuleException("Você pode ter até " + LIMITE_POR_PROFISSIONAL + " serviços ativos.");
        }
        Trabalhador trabalhador = trabalhadores.findById(actor.id())
                .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado."));
        ServicoOferecido servico = new ServicoOferecido();
        servico.setTrabalhador(trabalhador);
        aplicar(servico, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(repository.save(servico)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Profissional edita um serviço próprio")
    @Transactional
    public Response atualizar(@PathVariable Long id, @Valid @RequestBody Request request) {
        AuthenticatedPrincipal actor = requireWorker();
        ServicoOferecido servico = repository.findByIdAndTrabalhadorId(id, actor.id())
                .filter(ServicoOferecido::isAtivo)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado."));
        aplicar(servico, request);
        return toResponse(repository.save(servico));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Profissional remove um serviço próprio")
    @Transactional
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        AuthenticatedPrincipal actor = requireWorker();
        ServicoOferecido servico = repository.findByIdAndTrabalhadorId(id, actor.id())
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado."));
        // Desativa em vez de apagar: pedidos antigos continuam apontando para ele.
        servico.setAtivo(false);
        repository.save(servico);
        return ResponseEntity.noContent().build();
    }

    private AuthenticatedPrincipal requireWorker() {
        AuthenticatedPrincipal actor = access.current();
        access.requireTrabalhador(actor.id());
        return actor;
    }

    private static void aplicar(ServicoOferecido servico, Request request) {
        servico.setTitulo(HtmlSanitizer.sanitize(request.titulo().trim()));
        servico.setDescricao(HtmlSanitizer.sanitize(request.descricao().trim()));
        servico.setPreco(request.preco() == null ? null : request.preco().setScale(2, RoundingMode.HALF_UP));
        servico.setTipoServico(request.tipoServico());
    }

    private static Response toResponse(ServicoOferecido s) {
        return new Response(s.getId(), s.getTrabalhador().getId(), s.getTitulo(), s.getDescricao(),
                s.getPreco(), s.getTipoServico());
    }
}
