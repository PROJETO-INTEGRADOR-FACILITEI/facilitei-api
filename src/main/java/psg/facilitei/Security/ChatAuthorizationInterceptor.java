package psg.facilitei.Security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
public class ChatAuthorizationInterceptor implements ChannelInterceptor {
    private static final Pattern CHAT_DESTINATION =
            Pattern.compile("/(?:app|topics)/chat/(\\d+)");

    private final AccessControlService access;

    public ChatAuthorizationInterceptor(AccessControlService access) {
        this.access = access;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (accessor.getCommand() == StompCommand.CONNECT && accessor.getUser() == null) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException(
                    "Sessão autenticada necessária para o chat.");
        }
        if (accessor.getCommand() == StompCommand.SEND || accessor.getCommand() == StompCommand.SUBSCRIBE) {
            String destination = accessor.getDestination();
            Matcher matcher = destination == null ? null : CHAT_DESTINATION.matcher(destination);
            if (matcher == null || !matcher.matches()) {
                throw new org.springframework.security.access.AccessDeniedException("Destino de chat inválido.");
            }
            access.requireServiceParticipant(Long.valueOf(matcher.group(1)), accessor.getUser());
        }
        return message;
    }
}
