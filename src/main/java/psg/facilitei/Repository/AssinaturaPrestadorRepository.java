package psg.facilitei.Repository;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import psg.facilitei.Entity.AssinaturaPrestador;

public interface AssinaturaPrestadorRepository extends JpaRepository<AssinaturaPrestador, Long> {
    Optional<AssinaturaPrestador> findByTrabalhadorId(Long trabalhadorId);
    Optional<AssinaturaPrestador> findByCheckoutId(String checkoutId);
    Optional<AssinaturaPrestador> findBySubscriptionId(String subscriptionId);
    boolean existsByTrabalhadorIdAndStatusAndActiveUntilAfter(Long trabalhadorId, String status, Instant now);
}
