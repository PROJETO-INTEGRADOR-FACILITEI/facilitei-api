package psg.facilitei.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;
import psg.facilitei.Controller.domain.ChatInput;
import psg.facilitei.Entity.Mensagem;
import psg.facilitei.Repository.MensagemRepository;
import psg.facilitei.Repository.ServicoRepository;
import psg.facilitei.Services.NotificationService;
import psg.facilitei.Entity.Enum.NotificationType;

import java.time.LocalDateTime;
import java.util.List;
import java.security.Principal;
import psg.facilitei.Security.AccessControlService;
import psg.facilitei.Security.AuthenticatedPrincipal;
import psg.facilitei.Exceptions.BusinessRuleException;

@RestController
@RequestMapping("/api/chat")
public class LiveChatController {

    @Autowired
    private MensagemRepository mensagemRepository;

    @Autowired
    private AccessControlService access;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired(required = false)
    private psg.facilitei.Services.ChatPresenceRegistry presence;

    // Via WebSocket: o cliente manda em /app/chat/{servicoId} e quem está
    // inscrito em /topics/chat/{servicoId} recebe a mensagem salva.
    @MessageMapping("/chat/{servicoId}")
    @SendTo("/topics/chat/{servicoId}")
    public Mensagem sendMessage(@DestinationVariable Long servicoId, ChatInput input, Principal principal) {
        access.requireServiceParticipant(servicoId, principal);
        AuthenticatedPrincipal actor = access.fromPrincipal(principal);
        if (input.message() == null || input.message().isBlank() || input.message().length() > 2000) {
            throw new BusinessRuleException("A mensagem deve ter entre 1 e 2000 caracteres.");
        }

        Mensagem msg = new Mensagem();
        msg.setServicoId(servicoId);
        msg.setRemetente(actor.name());
        msg.setConteudo(psg.facilitei.Util.HtmlSanitizer.sanitize(input.message()));
        // RESUMO e SISTEMA ficam só para mensagens automáticas, o usuário não pode mandar.
        msg.setTipo("IMAGEM".equalsIgnoreCase(input.type()) ? "IMAGEM" : "TEXTO");
        msg.setUrlArquivo(input.fileUrl());
        msg.setDataEnvio(LocalDateTime.now());
        
        Mensagem saved = mensagemRepository.save(msg);
        var servico = servicoRepository.findById(servicoId).orElseThrow();
        Long recipientId = actor.isCliente()
                ? servico.getTrabalhador().getId()
                : servico.getCliente().getId();
        String preview = "IMAGEM".equalsIgnoreCase(saved.getTipo())
                ? actor.name() + " enviou uma imagem."
                : actor.name() + ": " + saved.getConteudo();
        // Se o destinatário já está com o chat aberto ele vê a mensagem na hora, então não notifica.
        if (presence == null || !presence.isInChat(recipientId, servicoId)) {
            notificationService.notify(
                    recipientId, NotificationType.NEW_MESSAGE,
                    "Nova mensagem", preview, "/painel/chat/" + servicoId);
        }
        return saved;
    }

    // Histórico carregado quando a tela do chat abre
    @GetMapping("/historico/{servicoId}")
    public ResponseEntity<List<Mensagem>> getHistorico(@PathVariable Long servicoId) {
        access.requireServiceParticipant(servicoId);
        return ResponseEntity.ok(mensagemRepository.findByServicoIdOrderByDataEnvioAsc(servicoId));
    }
}
