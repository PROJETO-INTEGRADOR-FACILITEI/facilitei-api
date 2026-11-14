package psg.facilitei.Services;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;
import psg.facilitei.Security.AuthenticatedPrincipal;

/**
 * Sabe quem está com a tela de uma conversa aberta (inscrito no tópico do chat).
 * Quem está dentro da conversa não recebe notificação de nova mensagem: as
 * notificações valem só para quem está fora do chat.
 */
@Component
public class ChatPresenceRegistry {
    private static final Pattern CHAT_TOPIC = Pattern.compile("/topics/chat/(\\d+)");

    private record Presence(Long userId, Long servicoId) {}

    /** chave: sessionId + ":" + subscriptionId */
    private final Map<String, Presence> subscriptions = new ConcurrentHashMap<>();

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        Long userId = userId(event.getUser());
        if (destination == null || userId == null) return;
        Matcher matcher = CHAT_TOPIC.matcher(destination);
        if (!matcher.matches()) return;
        subscriptions.put(key(accessor), new Presence(userId, Long.valueOf(matcher.group(1))));
    }

    @EventListener
    public void onUnsubscribe(SessionUnsubscribeEvent event) {
        subscriptions.remove(key(StompHeaderAccessor.wrap(event.getMessage())));
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String prefix = event.getSessionId() + ":";
        subscriptions.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public boolean isInChat(Long userId, Long servicoId) {
        return subscriptions.values().stream()
                .anyMatch(p -> p.userId().equals(userId) && p.servicoId().equals(servicoId));
    }

    private static String key(StompHeaderAccessor accessor) {
        return accessor.getSessionId() + ":" + accessor.getSubscriptionId();
    }

    private static Long userId(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof AuthenticatedPrincipal actor) {
            return actor.id();
        }
        return null;
    }
}
