package psg.facilitei.Services;

import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import psg.facilitei.DTO.NotificationResponseDTO;
import psg.facilitei.Entity.AppNotification;
import psg.facilitei.Entity.Enum.NotificationType;
import psg.facilitei.Exceptions.ResourceNotFoundException;
import psg.facilitei.Repository.AppNotificationRepository;
import psg.facilitei.Repository.UsuarioRepository;

@Service
public class NotificationService {
    private final AppNotificationRepository notifications;
    private final UsuarioRepository users;
    private final ApplicationEventPublisher events;

    public NotificationService(AppNotificationRepository notifications,
                               UsuarioRepository users,
                               ApplicationEventPublisher events) {
        this.notifications = notifications;
        this.users = users;
        this.events = events;
    }

    @Transactional
    public NotificationResponseDTO notify(Long recipientId, NotificationType type,
                                          String title, String message, String actionUrl) {
        AppNotification notification = new AppNotification();
        notification.setRecipient(users.findById(recipientId).orElseThrow(() ->
                new ResourceNotFoundException("Destinatário da notificação não encontrado.")));
        notification.setType(type);
        notification.setTitle(limit(title, 120));
        notification.setMessage(limit(message, 500));
        notification.setActionUrl(limit(actionUrl, 500));
        notification.setCreatedAt(Instant.now());
        AppNotification saved = notifications.save(notification);
        events.publishEvent(new NotificationCreatedEvent(saved.getId()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> list(Long recipientId) {
        return notifications.findTop30ByRecipientIdOrderByCreatedAtDesc(recipientId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long recipientId) {
        return notifications.countByRecipientIdAndReadAtIsNull(recipientId);
    }

    @Transactional
    public NotificationResponseDTO markRead(Long notificationId, Long recipientId) {
        AppNotification notification = notifications.findByIdAndRecipientId(notificationId, recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada."));
        if (notification.getReadAt() == null) notification.setReadAt(Instant.now());
        return toResponse(notification);
    }

    @Transactional
    public int markAllRead(Long recipientId) {
        return notifications.markAllRead(recipientId, Instant.now());
    }

    @Transactional(readOnly = true)
    public AppNotification findEntity(Long id) {
        return notifications.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Notificação não encontrada."));
    }

    private NotificationResponseDTO toResponse(AppNotification notification) {
        return new NotificationResponseDTO(
                notification.getId(), notification.getType(), notification.getTitle(),
                notification.getMessage(), notification.getActionUrl(),
                notification.getCreatedAt(), notification.isRead());
    }

    private String limit(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
