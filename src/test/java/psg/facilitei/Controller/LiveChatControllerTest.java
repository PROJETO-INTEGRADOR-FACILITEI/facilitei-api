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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveChatControllerTest {

    @Mock
    private MensagemRepository mensagemRepository;

    @InjectMocks
    private LiveChatController liveChatController;

    @Test
    void sendMessage_persisteMensagemComServicoIdEDadosDoInput() {
        ChatInput input = new ChatInput(42L, "cliente1", "Olá, tudo bem?", "TEXTO", null);

        when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mensagem result = liveChatController.sendMessage(42L, input);

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
