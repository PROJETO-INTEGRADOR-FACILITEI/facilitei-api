package psg.facilitei.Services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;
import org.springframework.web.socket.CloseStatus;
import psg.facilitei.Security.AuthenticatedPrincipal;

class ChatPresenceRegistryTest {
    private final UsernamePasswordAuthenticationToken ana = UsernamePasswordAuthenticationToken.authenticated(
            new AuthenticatedPrincipal(2L, "cliente", "Ana"), null, List.of());

    private Message<byte[]> frame(StompCommand command, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId("s1");
        accessor.setSubscriptionId("sub-0");
        if (destination != null) accessor.setDestination(destination);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void sabeQuemEstaDentroDaConversaAteSairOuDesconectar() {
        ChatPresenceRegistry registry = new ChatPresenceRegistry();
        registry.onSubscribe(new SessionSubscribeEvent(this, frame(StompCommand.SUBSCRIBE, "/topics/chat/10"), ana));

        assertTrue(registry.isInChat(2L, 10L));
        assertFalse(registry.isInChat(2L, 11L));
        assertFalse(registry.isInChat(1L, 10L));

        registry.onUnsubscribe(new SessionUnsubscribeEvent(this, frame(StompCommand.UNSUBSCRIBE, null), ana));
        assertFalse(registry.isInChat(2L, 10L));

        registry.onSubscribe(new SessionSubscribeEvent(this, frame(StompCommand.SUBSCRIBE, "/topics/chat/10"), ana));
        registry.onDisconnect(new SessionDisconnectEvent(this, frame(StompCommand.DISCONNECT, null), "s1",
                CloseStatus.NORMAL, ana));
        assertFalse(registry.isInChat(2L, 10L));
    }
}
