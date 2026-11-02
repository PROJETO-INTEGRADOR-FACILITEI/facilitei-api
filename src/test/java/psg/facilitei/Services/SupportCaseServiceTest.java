package psg.facilitei.Services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import psg.facilitei.DTO.SupportCaseDTOs.AdminUpdateRequest;
import psg.facilitei.DTO.SupportCaseDTOs.CreateRequest;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Servico;
import psg.facilitei.Entity.SupportCase;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Entity.Usuario;
import psg.facilitei.Entity.Enum.SupportCaseType;
import psg.facilitei.Entity.Enum.SupportCategory;
import psg.facilitei.Entity.Enum.AccountStatus;
import psg.facilitei.Entity.Enum.SupportStatus;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.ServicoRepository;
import psg.facilitei.Repository.SupportCaseHistoryRepository;
import psg.facilitei.Repository.SupportCaseRepository;
import psg.facilitei.Repository.SupportMessageRepository;
import psg.facilitei.Repository.TrabalhadorRepository;
import psg.facilitei.Repository.UsuarioRepository;
import psg.facilitei.Security.AdminAccessService;

@ExtendWith(MockitoExtension.class)
class SupportCaseServiceTest {
    @Mock SupportCaseRepository cases;
    @Mock SupportMessageRepository messages;
    @Mock SupportCaseHistoryRepository history;
    @Mock UsuarioRepository users;
    @Mock ServicoRepository services;
    @Mock ClienteRepository clients;
    @Mock TrabalhadorRepository workers;
    @Mock AdminAccessService adminAccess;
    @Mock NotificationService notifications;
    @Mock EmailService emails;
    private SupportCaseService service;

    @BeforeEach
    void setUp() {
        service = new SupportCaseService(cases, messages, history, users, services,
                clients, workers, adminAccess, notifications, emails);
    }

    @Test
    void disputaExigeServicoRelacionado() {
        Usuario reporter = user(1L);
        when(users.findById(1L)).thenReturn(Optional.of(reporter));

        assertThrows(BusinessRuleException.class, () -> service.create(1L,
                new CreateRequest(SupportCaseType.DISPUTA, SupportCategory.QUALIDADE_SERVICO,
                        "Serviço incompleto", "O combinado não foi entregue.", null, null, List.of())));
    }

    @Test
    void suporteGeralNaoExigeServicoNemUsuarioReportado() {
        Usuario reporter = user(1L);
        when(users.findById(1L)).thenReturn(Optional.of(reporter));
        when(cases.save(any(SupportCase.class))).thenAnswer(invocation -> {
            SupportCase saved = invocation.getArgument(0);
            saved.setId(30L);
            return saved;
        });

        var detail = service.create(1L,
                new CreateRequest(SupportCaseType.SUPORTE, SupportCategory.CONTA_ACESSO,
                        "Não consigo atualizar meus dados", "Preciso de ajuda com minha conta.",
                        null, null, List.of()));

        assertEquals(SupportCaseType.SUPORTE, detail.type());
        assertEquals(30L, detail.id());
        assertEquals(null, detail.reportedUserId());
    }

    @Test
    void impedeSegundaDisputaAbertaParaMesmoServico() {
        Cliente reporter = new Cliente();
        reporter.setId(1L);
        Trabalhador worker = new Trabalhador();
        worker.setId(2L);
        Servico servico = new Servico();
        servico.setId(10L);
        servico.setCliente(reporter);
        servico.setTrabalhador(worker);
        when(users.findById(1L)).thenReturn(Optional.of(reporter));
        when(services.findById(10L)).thenReturn(Optional.of(servico));
        when(cases.existsByReporterIdAndServiceIdAndTypeAndStatusIn(
                org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(SupportCaseType.DISPUTA), org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> service.create(1L,
                new CreateRequest(SupportCaseType.DISPUTA, SupportCategory.QUALIDADE_SERVICO,
                        "Serviço incompleto", "O combinado não foi entregue.", 10L, null, List.of())));
    }

    @Test
    void encerramentoAdministrativoExigeConclusao() {
        SupportCase supportCase = new SupportCase();
        supportCase.setId(4L);
        supportCase.setStatus(SupportStatus.EM_ANALISE);
        Usuario admin = user(9L);
        when(cases.findById(4L)).thenReturn(Optional.of(supportCase));
        when(users.findById(9L)).thenReturn(Optional.of(admin));

        assertThrows(BusinessRuleException.class, () -> service.updateAdmin(4L, 9L,
                new AdminUpdateRequest(SupportStatus.RESOLVIDO, null, null, null, " ")));
    }

    @Test
    void administradorSuspendeUsuarioReportadoComMotivoAuditavel() {
        Usuario reporter = user(1L);
        Usuario reported = user(2L);
        reported.setEmail("reportado@example.com");
        SupportCase supportCase = new SupportCase();
        supportCase.setId(4L);
        supportCase.setReporter(reporter);
        supportCase.setReportedUser(reported);
        supportCase.setStatus(SupportStatus.EM_ANALISE);
        Usuario admin = user(9L);
        admin.setEmail("admin@example.com");
        when(cases.findById(4L)).thenReturn(Optional.of(supportCase));
        when(users.findById(9L)).thenReturn(Optional.of(admin));
        when(adminAccess.isAdminEmail(reported.getEmail())).thenReturn(false);

        var detail = service.updateReportedUserStatus(
                4L, 9L, true, "Fraude confirmada após análise das evidências.");

        assertEquals(AccountStatus.SUSPENDED, reported.getAccountStatus());
        assertNotNull(reported.getSuspendedAt());
        assertEquals("Fraude confirmada após análise das evidências.", reported.getSuspensionReason());
        assertEquals(AccountStatus.SUSPENDED, detail.reportedUserAccountStatus());
        verify(emails).enviarEmailDecisaoConta(
                "reportado@example.com", true, "Fraude confirmada após análise das evidências.");
    }

    @Test
    void administradorNaoPodeSuspenderOutraContaAdministrativa() {
        Usuario reporter = user(1L);
        Usuario reportedAdmin = user(2L);
        reportedAdmin.setEmail("outro-admin@example.com");
        SupportCase supportCase = new SupportCase();
        supportCase.setId(5L);
        supportCase.setReporter(reporter);
        supportCase.setReportedUser(reportedAdmin);
        Usuario actor = user(9L);
        when(cases.findById(5L)).thenReturn(Optional.of(supportCase));
        when(users.findById(9L)).thenReturn(Optional.of(actor));
        when(adminAccess.isAdminEmail(reportedAdmin.getEmail())).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> service.updateReportedUserStatus(
                5L, 9L, true, "Tentativa indevida."));
    }

    private Usuario user(Long id) {
        Usuario user = new Usuario();
        user.setId(id);
        user.setNome("Teste");
        return user;
    }
}
