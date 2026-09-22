package psg.facilitei.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import psg.facilitei.DTO.NotificationResponseDTO;
import psg.facilitei.Entity.AppNotification;
import psg.facilitei.Entity.Usuario;
import psg.facilitei.Entity.Enum.NotificationType;
import psg.facilitei.Repository.AppNotificationRepository;
import psg.facilitei.Repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock AppNotificationRepository notifications;
    @Mock UsuarioRepository users;
    @Mock ApplicationEventPublisher events;
    @InjectMocks NotificationService service;

    @Test
    void notify_persisteEPublicaEventoParaPush() {
        Usuario recipient = new Usuario();
        recipient.setId(9L);
        when(users.findById(9L)).thenReturn(Optional.of(recipient));
        when(notifications.save(any(AppNotification.class))).thenAnswer(invocation -> {
            AppNotification value = invocation.getArgument(0);
            value.setId(41L);
            return value;
        });

        NotificationResponseDTO result = service.notify(
                9L, NotificationType.NEW_REQUEST, "Novo pedido", "Mensagem", "/painel");

        assertEquals(41L, result.id());
        assertEquals(NotificationType.NEW_REQUEST, result.type());
        assertFalse(result.read());
        ArgumentCaptor<NotificationCreatedEvent> event = ArgumentCaptor.forClass(NotificationCreatedEvent.class);
        verify(events).publishEvent(event.capture());
        assertEquals(41L, event.getValue().notificationId());
    }
}
