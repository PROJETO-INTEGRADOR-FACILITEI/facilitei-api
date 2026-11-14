package psg.facilitei.Controller;

import jakarta.validation.Valid;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import psg.facilitei.DTO.AdminDTOs.*;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Servico;
import psg.facilitei.Entity.SolicitacaoServico;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Entity.Usuario;
import psg.facilitei.Entity.Enum.AccountStatus;
import psg.facilitei.Entity.Enum.StatusServico;
import psg.facilitei.Entity.Enum.StatusSolicitacao;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.ServicoRepository;
import psg.facilitei.Repository.SolicitacaoServicoRepository;
import psg.facilitei.Repository.TrabalhadorRepository;
import psg.facilitei.Repository.UsuarioRepository;
import psg.facilitei.Security.AccessControlService;
import psg.facilitei.Security.AdminAccessService;
import psg.facilitei.Services.AccountModerationService;
import psg.facilitei.Services.ServicoService;

/**
 * Gestão completa do sistema pelo administrador: usuários (suspender por prazo,
 * reativar, excluir), serviços e solicitações. Protegido por ROLE_ADMIN em /api/admin/**.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminManagementController {
    private final UsuarioRepository users;
    private final ClienteRepository clientes;
    private final TrabalhadorRepository trabalhadores;
    private final ServicoRepository servicos;
    private final SolicitacaoServicoRepository solicitacoes;
    private final AccountModerationService moderation;
    private final ServicoService servicoService;
    private final AdminAccessService adminAccess;
    private final AccessControlService access;

    public AdminManagementController(UsuarioRepository users, ClienteRepository clientes,
                                     TrabalhadorRepository trabalhadores, ServicoRepository servicos,
                                     SolicitacaoServicoRepository solicitacoes,
                                     AccountModerationService moderation, ServicoService servicoService,
                                     AdminAccessService adminAccess, AccessControlService access) {
        this.users = users;
        this.clientes = clientes;
        this.trabalhadores = trabalhadores;
        this.servicos = servicos;
        this.solicitacoes = solicitacoes;
        this.moderation = moderation;
        this.servicoService = servicoService;
        this.adminAccess = adminAccess;
        this.access = access;
    }

    @GetMapping("/overview")
    @Transactional(readOnly = true)
    public Overview overview() {
        List<Servico> all = servicos.findAll();
        long abertos = all.stream()
                .filter(s -> AccountModerationService.OPEN_SERVICE.contains(s.getStatusServico())).count();
        long concluidos = all.stream().filter(s -> s.getStatusServico() == StatusServico.FINALIZADO).count();
        long cancelados = all.stream().filter(s -> s.getStatusServico() == StatusServico.CANCELADO).count();
        List<Usuario> allUsers = users.findAll();
        return new Overview(
                allUsers.size(), clientes.count(), trabalhadores.count(),
                allUsers.stream().filter(Usuario::isBlockedNow)
                        .filter(u -> u.getAccountStatus() == AccountStatus.SUSPENDED).count(),
                allUsers.stream().filter(u -> u.getAccountStatus() == AccountStatus.DELETED).count(),
                abertos, concluidos, cancelados,
                solicitacoes.findAll().stream()
                        .filter(s -> s.getStatusSolicitacao() == StatusSolicitacao.PENDENTE).count());
    }

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public UserPage users(@RequestParam(required = false) String search,
                          @RequestParam(required = false) AccountStatus status,
                          @RequestParam(required = false) String role,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size) {
        String cleanSearch = search == null || search.isBlank() ? null : search.trim();
        String cleanRole = "cliente".equals(role) || "trabalhador".equals(role) ? role : null;
        Page<Usuario> result = users.searchAdmin(cleanSearch, status, cleanRole,
                PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100),
                        Sort.by(Sort.Direction.DESC, "id")));
        return new UserPage(result.getContent().stream().map(this::toSummary).toList(),
                result.getTotalElements(), result.getTotalPages(), result.getNumber(), result.getSize());
    }

    @GetMapping("/users/{id}")
    @Transactional(readOnly = true)
    public UserSummary user(@PathVariable Long id) {
        return toSummary(users.findById(id).orElseThrow(() ->
                new psg.facilitei.Exceptions.ResourceNotFoundException("Usuário não encontrado.")));
    }

    @PostMapping("/users/{id}/suspend")
    public UserSummary suspend(@PathVariable Long id, @Valid @RequestBody SuspendRequest request) {
        return toSummary(moderation.suspend(id, access.current().id(), request.durationDays(), request.reason()));
    }

    @PostMapping("/users/{id}/reactivate")
    public UserSummary reactivate(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return toSummary(moderation.reactivate(id, access.current().id(), request.reason()));
    }

    @PostMapping("/users/{id}/delete")
    public UserSummary delete(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return toSummary(moderation.delete(id, access.current().id(), request.reason()));
    }

    @GetMapping("/servicos")
    @Transactional(readOnly = true)
    public List<ServiceSummary> services(@RequestParam(required = false) StatusServico status,
                                         @RequestParam(required = false) Long userId) {
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        List<Servico> list = userId != null
                ? servicos.findByClienteIdOrTrabalhadorId(userId, userId)
                : status != null ? servicos.findByStatusServico(status, sort) : servicos.findAll(sort);
        return list.stream()
                .filter(s -> status == null || s.getStatusServico() == status)
                .sorted(Comparator.comparing(Servico::getId).reversed())
                .limit(500)
                .map(AdminManagementController::toService).toList();
    }

    @PostMapping("/servicos/{id}/cancel")
    public ServiceSummary cancelService(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        servicoService.cancelarPeloAdmin(id, request.reason());
        return services(null, null).stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow();
    }

    @GetMapping("/solicitacoes")
    @Transactional(readOnly = true)
    public List<RequestSummary> requests(@RequestParam(required = false) StatusSolicitacao status) {
        return solicitacoes.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .filter(s -> status == null || s.getStatusSolicitacao() == status)
                .limit(500)
                .map(s -> new RequestSummary(s.getId(), s.getTitulo(), s.getDescricao(), s.getTipoServico(),
                        s.getStatusSolicitacao(), s.getDataSolicitacao(),
                        s.getCliente().getId(), s.getCliente().getNome(),
                        s.getTrabalhador().getId(), s.getTrabalhador().getNome()))
                .toList();
    }

    private UserSummary toSummary(Usuario user) {
        String role = user instanceof Trabalhador ? "trabalhador" : user instanceof Cliente ? "cliente" : "usuario";
        long total = servicos.findByClienteIdOrTrabalhadorId(user.getId(), user.getId()).size();
        return new UserSummary(user.getId(), user.getNome(), user.getEmail(), user.getTelefone(), role,
                user.getUrlFoto(), user.getAccountStatus(), user.isBlockedNow(), user.getSuspendedAt(),
                user.getSuspendedUntil(), user.getSuspensionReason(), adminAccess.isAdminEmail(user.getEmail()),
                total);
    }

    private static ServiceSummary toService(Servico s) {
        return new ServiceSummary(s.getId(), s.getTitulo(), s.getDescricao(), s.getTipoServico(),
                s.getStatusServico(), s.getCliente().getId(), s.getCliente().getNome(),
                s.getTrabalhador().getId(), s.getTrabalhador().getNome(), s.getMotivoCancelamento());
    }
}
