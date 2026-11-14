package psg.facilitei.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Servico;
import psg.facilitei.Entity.Usuario;
import psg.facilitei.Entity.Enum.AccountStatus;
import psg.facilitei.Entity.Enum.StatusServico;
import psg.facilitei.Exceptions.AccountSuspendedException;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Repository.ServicoRepository;
import psg.facilitei.Repository.SolicitacaoServicoRepository;
import psg.facilitei.Repository.UsuarioRepository;
import psg.facilitei.Repository.WebPushSubscriptionRepository;
import psg.facilitei.Security.AdminAccessService;

@ExtendWith(MockitoExtension.class)
class AccountModerationServiceTest {
    @Mock private UsuarioRepository users;
    @Mock private ServicoRepository servicos;
    @Mock private SolicitacaoServicoRepository solicitacoes;
    @Mock private NotificationService notifications;
    @Mock private EmailService emails;
    @Mock private AdminAccessService adminAccess;
    @Mock private WebPushSubscriptionRepository push;

    @InjectMocks private AccountModerationService service;

    private Cliente cliente(long id) {
        Cliente cliente = new Cliente();
        cliente.setId(id);
        cliente.setNome("Ana Souza");
        cliente.setEmail("ana@example.com");
        return cliente;
    }

    @Test
    void suspendeComPrazoEMostraDataNaMensagem() {
        Cliente ana = cliente(5L);
        when(users.findById(5L)).thenReturn(Optional.of(ana));

        service.suspend(5L, 1L, 7, "Linguagem ofensiva no chat");

        assertEquals(AccountStatus.SUSPENDED, ana.getAccountStatus());
        assertTrue(ana.isBlockedNow());
        assertTrue(ana.getSuspendedUntil().isAfter(Instant.now().plus(Duration.ofDays(6))));
        AccountSuspendedException ex = AccountModerationService.toException(ana);
        assertTrue(ex.getReason().contains("até"));
        assertTrue(ex.getReason().contains("Linguagem ofensiva no chat"));
    }

    @Test
    void suspensaoSemPrazoEhPorTempoIndeterminado() {
        Cliente ana = cliente(5L);
        when(users.findById(5L)).thenReturn(Optional.of(ana));

        service.suspend(5L, 1L, null, "Fraude");

        assertNull(ana.getSuspendedUntil());
        assertTrue(AccountModerationService.describe(ana).contains("por tempo indeterminado"));
    }

    @Test
    void suspensaoVencidaDeixaDeBloquearEEhLiberada() {
        Cliente ana = cliente(5L);
        ana.setAccountStatus(AccountStatus.SUSPENDED);
        ana.setSuspendedUntil(Instant.now().minusSeconds(5));
        assertFalse(ana.isBlockedNow());

        when(users.findByAccountStatusAndSuspendedUntilBefore(any(), any())).thenReturn(List.of(ana));
        service.releaseExpiredSuspensions();

        assertEquals(AccountStatus.ACTIVE, ana.getAccountStatus());
        assertNull(ana.getSuspensionReason());
    }

    @Test
    void excluirAnonimizaECancelaServicosAbertos() {
        Cliente ana = cliente(5L);
        Servico aberto = new Servico();
        aberto.setStatusServico(StatusServico.EM_ANDAMENTO);
        Servico concluido = new Servico();
        concluido.setStatusServico(StatusServico.FINALIZADO);
        when(users.findById(5L)).thenReturn(Optional.of(ana));
        when(servicos.findByClienteIdOrTrabalhadorId(anyLong(), anyLong())).thenReturn(List.of(aberto, concluido));
        when(solicitacoes.findByClienteIdOrTrabalhadorId(anyLong(), anyLong())).thenReturn(List.of());

        service.delete(5L, 1L, "Pedido do titular");

        assertEquals(AccountStatus.DELETED, ana.getAccountStatus());
        assertTrue(ana.isBlockedNow());
        assertEquals("removido-5@facilitei.invalid", ana.getEmail());
        assertEquals(StatusServico.CANCELADO, aberto.getStatusServico());
        assertEquals(StatusServico.FINALIZADO, concluido.getStatusServico());
        verify(emails).enviarEmailDecisaoConta(any(), org.mockito.ArgumentMatchers.eq(true), any());
    }

    @Test
    void naoPuneAdministradorNemAPropriaConta() {
        assertThrows(BusinessRuleException.class, () -> service.suspend(1L, 1L, 3, "teste"));

        Usuario admin = cliente(2L);
        when(users.findById(2L)).thenReturn(Optional.of(admin));
        when(adminAccess.isAdminEmail("ana@example.com")).thenReturn(true);
        assertThrows(BusinessRuleException.class, () -> service.suspend(2L, 1L, 3, "teste"));
    }

    @Test
    void exigeMotivoEPrazoValido() {
        Cliente ana = cliente(5L);
        when(users.findById(5L)).thenReturn(Optional.of(ana));
        assertThrows(BusinessRuleException.class, () -> service.suspend(5L, 1L, 3, "  "));
        assertThrows(BusinessRuleException.class, () -> service.suspend(5L, 1L, 0, "motivo"));
    }
}
