package psg.facilitei.Services;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import psg.facilitei.Entity.Servico;
import psg.facilitei.Entity.SolicitacaoServico;
import psg.facilitei.Entity.Usuario;
import psg.facilitei.Entity.Enum.AccountStatus;
import psg.facilitei.Entity.Enum.NotificationType;
import psg.facilitei.Entity.Enum.StatusServico;
import psg.facilitei.Entity.Enum.StatusSolicitacao;
import psg.facilitei.Exceptions.AccountSuspendedException;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Exceptions.ResourceNotFoundException;
import psg.facilitei.Repository.ServicoRepository;
import psg.facilitei.Repository.SolicitacaoServicoRepository;
import psg.facilitei.Repository.UsuarioRepository;
import psg.facilitei.Repository.WebPushSubscriptionRepository;
import psg.facilitei.Security.AdminAccessService;
import psg.facilitei.Util.HtmlSanitizer;

/**
 * Punições administrativas: suspensão por prazo definido ou indeterminado,
 * reativação e exclusão (anonimização) de contas.
 */
@Service
public class AccountModerationService {
    public static final String SUSPENDED_CODE = "ACCOUNT_SUSPENDED";
    public static final int MAX_DAYS = 3650;
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").withZone(ZONE);
    public static final List<StatusServico> OPEN_SERVICE = List.of(
            StatusServico.SOLICITADO, StatusServico.PENDENTE, StatusServico.EM_ANDAMENTO,
            StatusServico.PENDENTE_APROVACAO, StatusServico.AGUARDANDO_CONTATO,
            StatusServico.EM_ANALISE, StatusServico.APROVADO, StatusServico.PAUSADO);

    private final UsuarioRepository users;
    private final ServicoRepository servicos;
    private final SolicitacaoServicoRepository solicitacoes;
    private final NotificationService notifications;
    private final EmailService emails;
    private final AdminAccessService adminAccess;
    private final WebPushSubscriptionRepository pushSubscriptions;

    public AccountModerationService(UsuarioRepository users, ServicoRepository servicos,
                                    SolicitacaoServicoRepository solicitacoes,
                                    NotificationService notifications, EmailService emails,
                                    AdminAccessService adminAccess,
                                    WebPushSubscriptionRepository pushSubscriptions) {
        this.users = users;
        this.servicos = servicos;
        this.solicitacoes = solicitacoes;
        this.notifications = notifications;
        this.emails = emails;
        this.adminAccess = adminAccess;
        this.pushSubscriptions = pushSubscriptions;
    }

    /** Lança {@link AccountSuspendedException} se a conta estiver bloqueada; libera suspensões vencidas. */
    @Transactional
    public void requireNotBlocked(Usuario usuario) {
        if (usuario.hasExpiredSuspension()) {
            clearSuspension(usuario);
            users.save(usuario);
            return;
        }
        if (usuario.isBlockedNow()) throw toException(usuario);
    }

    public static AccountSuspendedException toException(Usuario usuario) {
        boolean deleted = usuario.getAccountStatus() == AccountStatus.DELETED;
        return new AccountSuspendedException(describe(usuario), usuario.getSuspensionReason(),
                deleted ? null : usuario.getSuspendedUntil(), deleted);
    }

    public static String describe(Usuario usuario) {
        if (usuario.getAccountStatus() == AccountStatus.DELETED) {
            return "Esta conta foi excluída pela administração do Facilitei.";
        }
        String prazo = usuario.getSuspendedUntil() == null
                ? "por tempo indeterminado"
                : "até " + FORMAT.format(usuario.getSuspendedUntil());
        String motivo = usuario.getSuspensionReason() == null || usuario.getSuspensionReason().isBlank()
                ? "" : " Motivo: " + usuario.getSuspensionReason();
        return "Sua conta está suspensa " + prazo + "." + motivo;
    }

    public static Map<String, Object> suspensionBody(AccountSuspendedException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 403);
        body.put("code", SUSPENDED_CODE);
        body.put("message", ex.getReason());
        body.put("reason", ex.getPunishmentReason());
        body.put("until", ex.getUntil() == null ? null : ex.getUntil().toString());
        body.put("permanent", ex.getUntil() == null);
        body.put("deleted", ex.isDeleted());
        return body;
    }

    @Transactional
    public Usuario suspend(Long userId, Long adminId, Integer durationDays, String reasonValue) {
        Usuario user = moderatable(userId, adminId);
        if (user.getAccountStatus() == AccountStatus.DELETED) {
            throw new BusinessRuleException("A conta já foi excluída.");
        }
        String reason = requireReason(reasonValue);
        Instant now = Instant.now();
        Instant until = null;
        if (durationDays != null) {
            if (durationDays < 1 || durationDays > MAX_DAYS) {
                throw new BusinessRuleException("A duração deve ser entre 1 e " + MAX_DAYS + " dias.");
            }
            until = now.plus(Duration.ofDays(durationDays));
        }
        user.setAccountStatus(AccountStatus.SUSPENDED);
        user.setSuspendedAt(now);
        user.setSuspendedUntil(until);
        user.setSuspensionReason(reason);
        users.save(user);
        notifications.notify(user.getId(), NotificationType.SYSTEM, "Sua conta foi suspensa",
                describe(user), "/conta-suspensa");
        emails.enviarEmailDecisaoConta(user.getEmail(), true, describe(user));
        return user;
    }

    @Transactional
    public Usuario reactivate(Long userId, Long adminId, String reasonValue) {
        Usuario user = moderatable(userId, adminId);
        if (user.getAccountStatus() == AccountStatus.DELETED) {
            throw new BusinessRuleException("Contas excluídas não podem ser reativadas.");
        }
        String reason = requireReason(reasonValue);
        clearSuspension(user);
        users.save(user);
        notifications.notify(user.getId(), NotificationType.SYSTEM, "Sua conta foi reativada", reason, "/painel");
        emails.enviarEmailDecisaoConta(user.getEmail(), false, reason);
        return user;
    }

    /**
     * Exclui a conta: bloqueia o acesso de forma definitiva, cancela os trabalhos em aberto e
     * anonimiza os dados pessoais. O histórico (serviços, avaliações, atendimentos) é mantido.
     */
    @Transactional
    public Usuario delete(Long userId, Long adminId, String reasonValue) {
        Usuario user = moderatable(userId, adminId);
        if (user.getAccountStatus() == AccountStatus.DELETED) {
            throw new BusinessRuleException("A conta já foi excluída.");
        }
        String reason = requireReason(reasonValue);
        String originalEmail = user.getEmail();
        cancelOpenWork(user.getId(), "Conta excluída pela administração.");

        user.setAccountStatus(AccountStatus.DELETED);
        user.setSuspendedAt(Instant.now());
        user.setSuspendedUntil(null);
        user.setSuspensionReason(reason);
        user.setNome("Usuário removido");
        user.setEmail("removido-" + user.getId() + "@facilitei.invalid");
        user.setTelefone(null);
        user.setUrlFoto(null);
        user.setSenha("!" + UUID.randomUUID());
        users.save(user);
        pushSubscriptions.deleteAll(pushSubscriptions.findByUserId(user.getId()));
        emails.enviarEmailDecisaoConta(originalEmail, true, "Sua conta foi excluída. Motivo: " + reason);
        return user;
    }

    /** Reativa automaticamente quem cumpriu a punição temporária. */
    @Scheduled(fixedDelayString = "${app.moderation.expiry-check-ms:60000}")
    @Transactional
    public void releaseExpiredSuspensions() {
        List<Usuario> expired = users.findByAccountStatusAndSuspendedUntilBefore(
                AccountStatus.SUSPENDED, Instant.now());
        for (Usuario user : expired) {
            clearSuspension(user);
            users.save(user);
            notifications.notify(user.getId(), NotificationType.SYSTEM, "Sua suspensão terminou",
                    "O prazo da punição acabou e sua conta foi reativada.", "/painel");
        }
    }

    private void cancelOpenWork(Long userId, String reason) {
        for (Servico servico : servicos.findByClienteIdOrTrabalhadorId(userId, userId)) {
            if (OPEN_SERVICE.contains(servico.getStatusServico())) {
                servico.setStatusServico(StatusServico.CANCELADO);
                servico.setMotivoCancelamento(reason);
                servicos.save(servico);
            }
        }
        for (SolicitacaoServico solicitacao : solicitacoes.findByClienteIdOrTrabalhadorId(userId, userId)) {
            if (solicitacao.getStatusSolicitacao() == StatusSolicitacao.PENDENTE) {
                solicitacao.setStatusSolicitacao(StatusSolicitacao.CANCELADA);
                solicitacao.setMotivoCancelamento(reason);
                solicitacoes.save(solicitacao);
            }
        }
    }

    private Usuario moderatable(Long userId, Long adminId) {
        if (userId.equals(adminId)) {
            throw new BusinessRuleException("Você não pode aplicar punições na própria conta.");
        }
        Usuario user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        if (adminAccess.isAdminEmail(user.getEmail())) {
            throw new BusinessRuleException("Contas administrativas não podem ser punidas.");
        }
        return user;
    }

    private static void clearSuspension(Usuario user) {
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setSuspendedAt(null);
        user.setSuspendedUntil(null);
        user.setSuspensionReason(null);
    }

    private static String requireReason(String value) {
        String reason = value == null ? "" : value.trim();
        if (reason.isEmpty()) throw new BusinessRuleException("Informe o motivo da decisão.");
        if (reason.length() > 2000) reason = reason.substring(0, 2000);
        return HtmlSanitizer.sanitize(reason);
    }
}
