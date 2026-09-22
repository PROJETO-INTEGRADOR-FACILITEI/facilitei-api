package psg.facilitei.Services;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import psg.facilitei.DTO.PushConfigResponseDTO;
import psg.facilitei.DTO.PushSubscriptionRequestDTO;
import psg.facilitei.Entity.AppNotification;
import psg.facilitei.Entity.WebPushSubscription;
import psg.facilitei.Repository.UsuarioRepository;
import psg.facilitei.Repository.WebPushSubscriptionRepository;

@Service
public class WebPushService {
    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private final WebPushSubscriptionRepository subscriptions;
    private final UsuarioRepository users;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final String publicKey;
    private final String privateKey;
    private final String subject;

    public WebPushService(WebPushSubscriptionRepository subscriptions,
                          UsuarioRepository users,
                          NotificationService notificationService,
                          ObjectMapper objectMapper,
                          @Value("${web-push.public-key:}") String publicKey,
                          @Value("${web-push.private-key:}") String privateKey,
                          @Value("${web-push.subject:mailto:contato@facilitei.app}") String subject) {
        this.subscriptions = subscriptions;
        this.users = users;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public boolean isEnabled() {
        return !publicKey.isBlank() && !privateKey.isBlank();
    }

    @Transactional(readOnly = true)
    public PushConfigResponseDTO config(Long userId) {
        return new PushConfigResponseDTO(isEnabled(), isEnabled() ? publicKey : "",
                !subscriptions.findByUserId(userId).isEmpty());
    }

    @Transactional
    public void subscribe(Long userId, PushSubscriptionRequestDTO request) {
        WebPushSubscription subscription = subscriptions.findByEndpoint(request.endpoint())
                .orElseGet(WebPushSubscription::new);
        Instant now = Instant.now();
        if (subscription.getId() == null) subscription.setCreatedAt(now);
        subscription.setUser(users.getReferenceById(userId));
        subscription.setEndpoint(request.endpoint());
        subscription.setP256dh(request.p256dh());
        subscription.setAuthSecret(request.auth());
        subscription.setUserAgent(request.userAgent());
        subscription.setUpdatedAt(now);
        subscriptions.save(subscription);
    }

    @Transactional
    public void unsubscribe(Long userId, PushSubscriptionRequestDTO request) {
        subscriptions.deleteByEndpointAndUserId(request.endpoint(), userId);
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void sendAfterCommit(NotificationCreatedEvent event) {
        if (!isEnabled()) return;
        AppNotification notification;
        try {
            notification = notificationService.findEntity(event.notificationId());
        } catch (RuntimeException exception) {
            log.warn("Notificação {} não encontrada para envio push.", event.notificationId());
            return;
        }
        List<WebPushSubscription> recipientSubscriptions =
                subscriptions.findByUserId(notification.getRecipient().getId());
        for (WebPushSubscription subscription : recipientSubscriptions) {
            send(subscription, notification);
        }
    }

    private void send(WebPushSubscription subscription, AppNotification notification) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "title", notification.getTitle(),
                    "body", notification.getMessage(),
                    "icon", "/pwa-192x192.png",
                    "badge", "/pwa-64x64.png",
                    "tag", "facilitei-" + notification.getType().name().toLowerCase(),
                    "data", Map.of(
                            "url", notification.getActionUrl() == null ? "/painel" : notification.getActionUrl(),
                            "notificationId", notification.getId(),
                            "type", notification.getType().name())));
            PushService pushService = new PushService(publicKey, privateKey, subject);
            Notification push = new Notification(
                    subscription.getEndpoint(), subscription.getP256dh(),
                    subscription.getAuthSecret(), payload.getBytes(StandardCharsets.UTF_8));
            HttpResponse response = pushService.send(push);
            int status = response.getStatusLine().getStatusCode();
            if (status == 404 || status == 410) subscriptions.deleteById(subscription.getId());
            else if (status >= 400) log.warn("Push recusado com status {} para subscription {}.", status, subscription.getId());
        } catch (Exception exception) {
            log.warn("Falha ao enviar push para subscription {}: {}", subscription.getId(), exception.getMessage());
        }
    }
}
