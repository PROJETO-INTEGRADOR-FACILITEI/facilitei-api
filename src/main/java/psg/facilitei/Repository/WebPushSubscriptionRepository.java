package psg.facilitei.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import psg.facilitei.Entity.WebPushSubscription;

public interface WebPushSubscriptionRepository extends JpaRepository<WebPushSubscription, Long> {
    List<WebPushSubscription> findByUserId(Long userId);
    Optional<WebPushSubscription> findByEndpoint(String endpoint);
    long deleteByEndpointAndUserId(String endpoint, Long userId);
}
