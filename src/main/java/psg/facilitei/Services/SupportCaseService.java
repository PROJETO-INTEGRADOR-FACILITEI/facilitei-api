package psg.facilitei.Services;

import static psg.facilitei.DTO.SupportCaseDTOs.*;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import psg.facilitei.Entity.Servico;
import psg.facilitei.Entity.SupportCase;
import psg.facilitei.Entity.SupportCaseHistory;
import psg.facilitei.Entity.SupportMessage;
import psg.facilitei.Entity.Usuario;
import psg.facilitei.Entity.Enum.NotificationType;
import psg.facilitei.Entity.Enum.SupportCaseType;
import psg.facilitei.Entity.Enum.SupportCategory;
import psg.facilitei.Entity.Enum.SupportEventType;
import psg.facilitei.Entity.Enum.SupportPriority;
import psg.facilitei.Entity.Enum.SupportStatus;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Exceptions.ResourceNotFoundException;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.ServicoRepository;
import psg.facilitei.Repository.SupportCaseHistoryRepository;
import psg.facilitei.Repository.SupportCaseRepository;
import psg.facilitei.Repository.SupportMessageRepository;
import psg.facilitei.Repository.TrabalhadorRepository;
import psg.facilitei.Repository.UsuarioRepository;
import psg.facilitei.Security.AdminAccessService;
import psg.facilitei.Util.HtmlSanitizer;

@Service
public class SupportCaseService {
    private static final EnumSet<SupportStatus> OPEN_STATUSES = EnumSet.of(
            SupportStatus.ABERTO, SupportStatus.EM_ANALISE, SupportStatus.AGUARDANDO_USUARIO);
    private static final DateTimeFormatter PROTOCOL_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final SupportCaseRepository cases;
    private final SupportMessageRepository messages;
    private final SupportCaseHistoryRepository history;
    private final UsuarioRepository users;
    private final ServicoRepository services;
    private final ClienteRepository clients;
    private final TrabalhadorRepository workers;
    private final AdminAccessService adminAccess;
    private final NotificationService notifications;

    public SupportCaseService(SupportCaseRepository cases,
                              SupportMessageRepository messages,
                              SupportCaseHistoryRepository history,
                              UsuarioRepository users,
                              ServicoRepository services,
                              ClienteRepository clients,
                              TrabalhadorRepository workers,
                              AdminAccessService adminAccess,
                              NotificationService notifications) {
        this.cases = cases;
        this.messages = messages;
        this.history = history;
        this.users = users;
        this.services = services;
        this.clients = clients;
        this.workers = workers;
        this.adminAccess = adminAccess;
        this.notifications = notifications;
    }

    @Transactional
    public DetailResponse create(Long reporterId, CreateRequest request) {
        Usuario reporter = getUser(reporterId);
        Servico service = request.serviceId() == null ? null : services.findById(request.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Serviço informado não foi encontrado."));
        validateServiceAccess(reporterId, service);

        if (request.type() == SupportCaseType.DISPUTA && service == null) {
            throw new BusinessRuleException("Uma disputa precisa estar vinculada a um serviço.");
        }
        if (request.type() == SupportCaseType.DISPUTA
                && cases.existsByReporterIdAndServiceIdAndTypeAndStatusIn(
                        reporterId, service.getId(), SupportCaseType.DISPUTA, OPEN_STATUSES)) {
            throw new BusinessRuleException("Já existe uma disputa aberta para este serviço.");
        }

        Usuario reportedUser = resolveReportedUser(request.reportedUserId(), reporterId, service);
        if (reportedUser != null && reportedUser.getId().equals(reporterId)) {
            throw new BusinessRuleException("Você não pode denunciar a própria conta.");
        }
        if (service != null && reportedUser != null && !isServiceParticipant(service, reportedUser.getId())) {
            throw new BusinessRuleException("A pessoa denunciada não participa do serviço informado.");
        }

        Instant now = Instant.now();
        SupportCase supportCase = new SupportCase();
        supportCase.setProtocol(newProtocol(now));
        supportCase.setType(request.type());
        supportCase.setCategory(request.category());
        supportCase.setStatus(SupportStatus.ABERTO);
        supportCase.setPriority(initialPriority(request.category()));
        supportCase.setReporter(reporter);
        supportCase.setReportedUser(reportedUser);
        supportCase.setService(service);
        supportCase.setSubject(clean(request.subject(), 160));
        supportCase.setDescription(clean(request.description(), 3000));
        supportCase.setEvidenceUrls(validateEvidence(request.evidenceUrls()));
        supportCase.setCreatedAt(now);
        supportCase.setUpdatedAt(now);
        supportCase = cases.save(supportCase);

        addHistory(supportCase, reporter, SupportEventType.CRIADO,
                request.type() == SupportCaseType.DISPUTA ? "Disputa aberta pelo usuário." : "Denúncia aberta pelo usuário.");
        notifyAdmins(
                supportCase,
                NotificationType.NEW_SUPPORT_CASE,
                supportCase.getType() == SupportCaseType.DISPUTA ? "Nova disputa" : "Nova denúncia",
                supportCase.getProtocol() + " · " + supportCase.getSubject());
        return toDetail(supportCase, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<SummaryResponse> listMine(Long reporterId, Pageable pageable) {
        return page(cases.findByReporterIdOrderByUpdatedAtDesc(reporterId, pageable).map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public DetailResponse getMine(Long caseId, Long reporterId) {
        return toDetail(cases.findByIdAndReporterId(caseId, reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Atendimento não encontrado.")), false);
    }

    @Transactional
    public DetailResponse addUserMessage(Long caseId, Long reporterId, MessageRequest request) {
        SupportCase supportCase = cases.findByIdAndReporterId(caseId, reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Atendimento não encontrado."));
        ensureOpen(supportCase);
        Usuario actor = getUser(reporterId);
        saveMessage(supportCase, actor, request.message(), false);
        if (supportCase.getStatus() == SupportStatus.AGUARDANDO_USUARIO) {
            changeStatus(supportCase, actor, SupportStatus.EM_ANALISE, null);
        }
        supportCase.setUpdatedAt(Instant.now());
        notifyAssignedOrAdmins(supportCase, "Nova mensagem no atendimento " + supportCase.getProtocol());
        return toDetail(supportCase, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<SummaryResponse> searchAdmin(SupportStatus status,
                                                      SupportCaseType type,
                                                      SupportPriority priority,
                                                      Long assignedAdminId,
                                                      boolean unassigned,
                                                      String search,
                                                      Pageable pageable) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return page(cases.searchAdmin(status, type, priority, assignedAdminId,
                unassigned, normalizedSearch, pageable).map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public DetailResponse getAdmin(Long caseId) {
        return toDetail(getCase(caseId), true);
    }

    @Transactional
    public DetailResponse addAdminMessage(Long caseId, Long adminId, MessageRequest request) {
        SupportCase supportCase = getCase(caseId);
        ensureOpen(supportCase);
        Usuario admin = getUser(adminId);
        saveMessage(supportCase, admin, request.message(), request.internalNote());
        supportCase.setUpdatedAt(Instant.now());

        if (request.internalNote()) {
            addHistory(supportCase, admin, SupportEventType.NOTA_INTERNA, "Nota interna adicionada.");
        } else {
            if (supportCase.getStatus() != SupportStatus.AGUARDANDO_USUARIO) {
                changeStatus(supportCase, admin, SupportStatus.AGUARDANDO_USUARIO, null);
            }
            addHistory(supportCase, admin, SupportEventType.RESPOSTA_ENVIADA, "Resposta enviada ao usuário.");
            notifyReporter(supportCase, "O suporte respondeu ao seu atendimento.");
        }
        return toDetail(supportCase, true);
    }

    @Transactional
    public DetailResponse updateAdmin(Long caseId, Long adminId, AdminUpdateRequest request) {
        SupportCase supportCase = getCase(caseId);
        Usuario admin = getUser(adminId);
        Instant now = Instant.now();

        if (Boolean.TRUE.equals(request.assignToMe())) {
            supportCase.setAssignedAdmin(admin);
            addHistory(supportCase, admin, SupportEventType.ATRIBUIDO,
                    "Atendimento atribuído a " + admin.getNome() + ".");
        } else if (Boolean.TRUE.equals(request.unassign())) {
            supportCase.setAssignedAdmin(null);
            addHistory(supportCase, admin, SupportEventType.ATRIBUIDO, "Atendimento devolvido à fila.");
        }

        if (request.priority() != null && request.priority() != supportCase.getPriority()) {
            supportCase.setPriority(request.priority());
            addHistory(supportCase, admin, SupportEventType.PRIORIDADE_ALTERADA,
                    "Prioridade alterada para " + request.priority().name() + ".");
        }

        if (request.status() != null && request.status() != supportCase.getStatus()) {
            boolean closing = request.status() == SupportStatus.RESOLVIDO
                    || request.status() == SupportStatus.REJEITADO;
            String resolution = cleanNullable(request.resolution(), 2000);
            if (closing && (resolution == null || resolution.isBlank())) {
                throw new BusinessRuleException("Informe a conclusão antes de encerrar o atendimento.");
            }
            if (closing) {
                supportCase.setResolution(resolution);
                supportCase.setClosedAt(now);
            } else {
                supportCase.setResolution(null);
                supportCase.setClosedAt(null);
            }
            changeStatus(supportCase, admin, request.status(), resolution);
            notifyReporter(supportCase, closing
                    ? "Seu atendimento foi concluído pelo suporte."
                    : "O status do seu atendimento foi atualizado.");
        }

        supportCase.setUpdatedAt(now);
        return toDetail(supportCase, true);
    }

    @Transactional(readOnly = true)
    public AdminMetricsResponse metrics() {
        return new AdminMetricsResponse(
                users.count(), clients.count(), workers.count(), services.count(),
                cases.countByStatusIn(OPEN_STATUSES),
                cases.countByTypeAndStatusIn(SupportCaseType.DISPUTA, OPEN_STATUSES),
                cases.countByPriorityAndStatusIn(SupportPriority.URGENTE, OPEN_STATUSES),
                cases.countByAssignedAdminIsNullAndStatusIn(OPEN_STATUSES),
                cases.countByCreatedAtAfter(Instant.now().minusSeconds(24 * 60 * 60)));
    }

    private void validateServiceAccess(Long reporterId, Servico service) {
        if (service != null && !isServiceParticipant(service, reporterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não participa do serviço informado.");
        }
    }

    private boolean isServiceParticipant(Servico service, Long userId) {
        return service.getCliente().getId().equals(userId)
                || service.getTrabalhador().getId().equals(userId);
    }

    private Usuario resolveReportedUser(Long requestedId, Long reporterId, Servico service) {
        if (requestedId != null) return getUser(requestedId);
        if (service == null) return null;
        return service.getCliente().getId().equals(reporterId)
                ? service.getTrabalhador() : service.getCliente();
    }

    private void ensureOpen(SupportCase supportCase) {
        if (!OPEN_STATUSES.contains(supportCase.getStatus())) {
            throw new BusinessRuleException("Este atendimento já foi encerrado. Abra um novo caso se precisar.");
        }
    }

    private void saveMessage(SupportCase supportCase, Usuario author, String value, boolean internal) {
        SupportMessage message = new SupportMessage();
        message.setSupportCase(supportCase);
        message.setAuthor(author);
        message.setMessage(clean(value, 2000));
        message.setInternalNote(internal);
        message.setCreatedAt(Instant.now());
        messages.save(message);
    }

    private void changeStatus(SupportCase supportCase, Usuario actor, SupportStatus status, String note) {
        SupportStatus previous = supportCase.getStatus();
        supportCase.setStatus(status);
        addHistory(supportCase, actor, SupportEventType.STATUS_ALTERADO,
                "Status alterado de " + previous.name() + " para " + status.name()
                        + (note == null || note.isBlank() ? "." : ": " + note));
    }

    private void addHistory(SupportCase supportCase, Usuario actor,
                            SupportEventType type, String description) {
        SupportCaseHistory entry = new SupportCaseHistory();
        entry.setSupportCase(supportCase);
        entry.setActor(actor);
        entry.setEventType(type);
        entry.setDescription(clean(description, 500));
        entry.setCreatedAt(Instant.now());
        history.save(entry);
    }

    private void notifyAdmins(SupportCase supportCase, NotificationType type,
                              String title, String message) {
        for (Usuario admin : adminAccess.configuredAdmins()) {
            if (admin.getId().equals(supportCase.getReporter().getId())) continue;
            notifications.notify(admin.getId(), type, title, message,
                    "/admin/suporte/" + supportCase.getId());
        }
    }

    private void notifyAssignedOrAdmins(SupportCase supportCase, String message) {
        if (supportCase.getAssignedAdmin() != null) {
            notifications.notify(supportCase.getAssignedAdmin().getId(), NotificationType.SUPPORT_UPDATE,
                    "Atualização no suporte", message, "/admin/suporte/" + supportCase.getId());
        } else {
            notifyAdmins(supportCase, NotificationType.SUPPORT_UPDATE,
                    "Atualização no suporte", message);
        }
    }

    private void notifyReporter(SupportCase supportCase, String message) {
        notifications.notify(supportCase.getReporter().getId(), NotificationType.SUPPORT_UPDATE,
                "Atualização no atendimento", message, "/painel/suporte/" + supportCase.getId());
    }

    private DetailResponse toDetail(SupportCase supportCase, boolean adminView) {
        List<SupportMessage> caseMessages = adminView
                ? messages.findBySupportCaseIdOrderByCreatedAtAsc(supportCase.getId())
                : messages.findBySupportCaseIdAndInternalNoteFalseOrderByCreatedAtAsc(supportCase.getId());
        List<SupportCaseHistory> caseHistory = adminView
                ? history.findBySupportCaseIdOrderByCreatedAtAsc(supportCase.getId())
                : history.findBySupportCaseIdAndEventTypeNotOrderByCreatedAtAsc(
                        supportCase.getId(), SupportEventType.NOTA_INTERNA);
        return new DetailResponse(
                supportCase.getId(), supportCase.getProtocol(), supportCase.getType(),
                supportCase.getCategory(), supportCase.getStatus(), supportCase.getPriority(),
                supportCase.getSubject(), supportCase.getDescription(),
                id(supportCase.getService()), name(supportCase.getService()),
                supportCase.getReporter().getId(), supportCase.getReporter().getNome(),
                id(supportCase.getReportedUser()), name(supportCase.getReportedUser()),
                id(supportCase.getAssignedAdmin()), name(supportCase.getAssignedAdmin()),
                List.copyOf(supportCase.getEvidenceUrls()), supportCase.getResolution(),
                supportCase.getCreatedAt(), supportCase.getUpdatedAt(), supportCase.getClosedAt(),
                caseMessages.stream().map(this::toMessage).toList(),
                caseHistory.stream().map(this::toHistory).toList());
    }

    private SummaryResponse toSummary(SupportCase supportCase) {
        return new SummaryResponse(
                supportCase.getId(), supportCase.getProtocol(), supportCase.getType(),
                supportCase.getCategory(), supportCase.getStatus(), supportCase.getPriority(),
                supportCase.getSubject(), id(supportCase.getService()), name(supportCase.getService()),
                supportCase.getReporter().getId(), supportCase.getReporter().getNome(),
                id(supportCase.getAssignedAdmin()), name(supportCase.getAssignedAdmin()),
                supportCase.getCreatedAt(), supportCase.getUpdatedAt());
    }

    private MessageResponse toMessage(SupportMessage message) {
        return new MessageResponse(
                message.getId(), message.getAuthor().getId(), message.getAuthor().getNome(),
                adminAccess.isAdminEmail(message.getAuthor().getEmail()), message.isInternalNote(),
                message.getMessage(), message.getCreatedAt());
    }

    private HistoryResponse toHistory(SupportCaseHistory entry) {
        return new HistoryResponse(entry.getId(), entry.getEventType(), entry.getDescription(),
                entry.getActor().getId(), entry.getActor().getNome(), entry.getCreatedAt());
    }

    private <T> PageResponse<T> page(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize(), page.isFirst(), page.isLast());
    }

    private List<String> validateEvidence(List<String> values) {
        if (values == null) return List.of();
        return values.stream().map(String::trim).filter(value -> !value.isBlank()).map(value -> {
            try {
                URI uri = URI.create(value);
                String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
                if (!"https".equals(scheme)) throw new IllegalArgumentException();
                return value;
            } catch (IllegalArgumentException exception) {
                throw new BusinessRuleException("Uma das evidências possui URL inválida.");
            }
        }).distinct().limit(5).toList();
    }

    private SupportPriority initialPriority(SupportCategory category) {
        return category == SupportCategory.SEGURANCA || category == SupportCategory.FRAUDE_GOLPE
                ? SupportPriority.ALTA : SupportPriority.NORMAL;
    }

    private String newProtocol(Instant now) {
        return "FAC-" + PROTOCOL_DATE.format(now) + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private SupportCase getCase(Long id) {
        return cases.findById(id).orElseThrow(() -> new ResourceNotFoundException("Atendimento não encontrado."));
    }

    private Usuario getUser(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    private String clean(String value, int max) {
        String cleaned = HtmlSanitizer.sanitize(value == null ? "" : value.trim());
        return cleaned.length() <= max ? cleaned : cleaned.substring(0, max);
    }

    private String cleanNullable(String value, int max) {
        if (value == null || value.isBlank()) return null;
        return clean(value, max);
    }

    private Long id(Object value) {
        if (value instanceof Servico service) return service.getId();
        if (value instanceof Usuario user) return user.getId();
        return null;
    }

    private String name(Object value) {
        if (value instanceof Servico service) return service.getTitulo();
        if (value instanceof Usuario user) return user.getNome();
        return null;
    }
}
