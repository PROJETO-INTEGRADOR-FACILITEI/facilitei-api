package psg.facilitei.Security;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import psg.facilitei.Repository.AvaliacaoClienteRepository;
import psg.facilitei.Repository.AvaliacaoServicoRepository;
import psg.facilitei.Repository.PortfolioRepository;
import psg.facilitei.Repository.ServicoRepository;
import psg.facilitei.Repository.SolicitacaoServicoRepository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {
    @Mock ServicoRepository servicos;
    @Mock SolicitacaoServicoRepository solicitacoes;
    @Mock PortfolioRepository portfolios;
    @Mock AvaliacaoServicoRepository avaliacoesServico;
    @Mock AvaliacaoClienteRepository avaliacoesCliente;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void clienteNaoPodeAcessarServicoDeOutraPessoa() {
        autenticar(7L, "cliente");
        when(servicos.existsByIdAndClienteId(99L, 7L)).thenReturn(false);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> access().requireServiceParticipant(99L));

        assertEquals(403, erro.getStatusCode().value());
    }

    @Test
    void participantePodeAcessarSeuServico() {
        autenticar(7L, "cliente");
        when(servicos.existsByIdAndClienteId(99L, 7L)).thenReturn(true);

        assertDoesNotThrow(() -> access().requireServiceParticipant(99L));
    }

    @Test
    void trabalhadorNaoPodeRemoverPortfolioDeOutroProfissional() {
        autenticar(3L, "trabalhador");
        when(portfolios.existsByIdAndTrabalhadorId(22L, 3L)).thenReturn(false);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> access().requirePortfolioOwner(22L));

        assertEquals(403, erro.getStatusCode().value());
    }

    @Test
    void requisicaoSemSessaoRecebeNaoAutorizado() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> access().requireCliente(1L));

        assertEquals(401, erro.getStatusCode().value());
    }

    private AccessControlService access() {
        return new AccessControlService(servicos, solicitacoes, portfolios,
                avaliacoesServico, avaliacoesCliente);
    }

    private void autenticar(Long id, String role) {
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(id, role, "Teste");
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
