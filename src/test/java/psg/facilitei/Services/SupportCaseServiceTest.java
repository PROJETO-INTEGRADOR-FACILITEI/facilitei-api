package psg.facilitei.Services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

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
    private SupportCaseService service;

    @BeforeEach
    void setUp() {
        service = new SupportCaseService(cases, messages, history, users, services,
                clients, workers, adminAccess, notifications);
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

    private Usuario user(Long id) {
        Usuario user = new Usuario();
        user.setId(id);
        user.setNome("Teste");
        return user;
    }
}
