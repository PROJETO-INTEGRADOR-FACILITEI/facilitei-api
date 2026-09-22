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
@RequestMapping("/api/chat") // Endpoint REST para histórico
public class LiveChatController {

    @Autowired
    private MensagemRepository mensagemRepository;

    @Autowired
    private AccessControlService access;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private NotificationService notificationService;

    // 1. Endpoint para enviar mensagem via WebSocket
    // O cliente manda para: /app/chat/{servicoId}
    // O servidor distribui para quem ouve: /topics/chat/{servicoId}
    @MessageMapping("/chat/{servicoId}")
    @SendTo("/topics/chat/{servicoId}")
    public Mensagem sendMessage(@DestinationVariable Long servicoId, ChatInput input, Principal principal) {
        access.requireServiceParticipant(servicoId, principal);
        AuthenticatedPrincipal actor = access.fromPrincipal(principal);
        if (input.message() == null || input.message().isBlank() || input.message().length() > 2000) {
            throw new BusinessRuleException("A mensagem deve ter entre 1 e 2000 caracteres.");
        }
        
        // Salva no banco (Persistência)
        Mensagem msg = new Mensagem();
        msg.setServicoId(servicoId);
        msg.setRemetente(actor.name());
        msg.setConteudo(psg.facilitei.Util.HtmlSanitizer.sanitize(input.message()));
        msg.setTipo(input.type() != null ? input.type() : "TEXTO");
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
        notificationService.notify(
                recipientId, NotificationType.NEW_MESSAGE,
                "Nova mensagem", preview, "/painel/chat/" + servicoId);
        return saved;
    }

    // 2. Endpoint REST para carregar histórico quando abrir a tela
    @GetMapping("/historico/{servicoId}")
    public ResponseEntity<List<Mensagem>> getHistorico(@PathVariable Long servicoId) {
        access.requireServiceParticipant(servicoId);
        return ResponseEntity.ok(mensagemRepository.findByServicoIdOrderByDataEnvioAsc(servicoId));
    }
}
