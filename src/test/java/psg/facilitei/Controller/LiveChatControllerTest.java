package psg.facilitei.Controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import psg.facilitei.Controller.domain.ChatInput;
import psg.facilitei.Entity.Mensagem;
import psg.facilitei.Repository.MensagemRepository;
import psg.facilitei.Repository.ServicoRepository;
import psg.facilitei.Services.NotificationService;
import psg.facilitei.Entity.Servico;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Trabalhador;
import java.util.Optional;
import psg.facilitei.Security.AccessControlService;
import psg.facilitei.Security.AuthenticatedPrincipal;
import java.security.Principal;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveChatControllerTest {

    @Mock
    private MensagemRepository mensagemRepository;

    @Mock
    private AccessControlService access;
    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LiveChatController liveChatController;

    @Test
    void sendMessage_persisteMensagemComServicoIdEDadosDoInput() {
        ChatInput input = new ChatInput(42L, "cliente1", "Olá, tudo bem?", "TEXTO", null);

        when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Principal principal = () -> "cliente1";
        when(access.fromPrincipal(principal))
                .thenReturn(new AuthenticatedPrincipal(1L, "cliente", "cliente1"));
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setId(2L);
        Servico servico = new Servico();
        servico.setCliente(cliente);
        servico.setTrabalhador(trabalhador);
        when(servicoRepository.findById(42L)).thenReturn(Optional.of(servico));
        Mensagem result = liveChatController.sendMessage(42L, input, principal);

        ArgumentCaptor<Mensagem> captor = ArgumentCaptor.forClass(Mensagem.class);
        verify(mensagemRepository).save(captor.capture());

        assertEquals(42L, captor.getValue().getServicoId());
        assertEquals("cliente1", captor.getValue().getRemetente());
        assertEquals("Olá, tudo bem?", captor.getValue().getConteudo());
        assertEquals("TEXTO", captor.getValue().getTipo());
        assertEquals(42L, result.getServicoId());
    }

    @Test
    void getHistorico_retornaMensagensOrdenadasDoRepository() {
        Mensagem msg = new Mensagem();
        msg.setId(1L);
        msg.setServicoId(7L);
        msg.setRemetente("trabalhador1");
        msg.setConteudo("Chego em 10 minutos");

        when(mensagemRepository.findByServicoIdOrderByDataEnvioAsc(7L)).thenReturn(List.of(msg));

        ResponseEntity<List<Mensagem>> response = liveChatController.getHistorico(7L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("Chego em 10 minutos", response.getBody().get(0).getConteudo());
    }
}
