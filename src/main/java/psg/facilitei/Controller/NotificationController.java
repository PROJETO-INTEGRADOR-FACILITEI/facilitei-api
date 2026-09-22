package psg.facilitei.Controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import psg.facilitei.DTO.NotificationResponseDTO;
import psg.facilitei.DTO.PushConfigResponseDTO;
import psg.facilitei.DTO.PushSubscriptionRequestDTO;
import psg.facilitei.Security.AccessControlService;
import psg.facilitei.Security.AuthenticatedPrincipal;
import psg.facilitei.Services.NotificationService;
import psg.facilitei.Services.WebPushService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notifications;
    private final WebPushService webPush;
    private final AccessControlService access;

    public NotificationController(NotificationService notifications,
                                  WebPushService webPush,
                                  AccessControlService access) {
        this.notifications = notifications;
        this.webPush = webPush;
        this.access = access;
    }

    @GetMapping
    public List<NotificationResponseDTO> list() {
        return notifications.list(access.current().id());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", notifications.unreadCount(access.current().id()));
    }

    @PatchMapping("/{id}/read")
    public NotificationResponseDTO markRead(@PathVariable Long id) {
        return notifications.markRead(id, access.current().id());
    }

    @PatchMapping("/read-all")
    public Map<String, Integer> markAllRead() {
        return Map.of("updated", notifications.markAllRead(access.current().id()));
    }

    @GetMapping("/push/config")
    public PushConfigResponseDTO pushConfig() {
        return webPush.config(access.current().id());
    }

    @PostMapping("/push/subscriptions")
    public ResponseEntity<Void> subscribe(@Valid @RequestBody PushSubscriptionRequestDTO request) {
        AuthenticatedPrincipal actor = access.current();
        webPush.subscribe(actor.id(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/push/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@Valid @RequestBody PushSubscriptionRequestDTO request) {
        webPush.unsubscribe(access.current().id(), request);
        return ResponseEntity.noContent().build();
    }
}
