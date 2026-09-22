package psg.facilitei.DTO;

import java.time.Instant;
import psg.facilitei.Entity.Enum.NotificationType;

public record NotificationResponseDTO(
        Long id,
        NotificationType type,
        String title,
        String message,
        String actionUrl,
        Instant createdAt,
        boolean read) {
}
