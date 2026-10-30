package psg.facilitei.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import psg.facilitei.Entity.AssinaturaPrestador;

public interface AssinaturaPrestadorRepository extends JpaRepository<AssinaturaPrestador, Long> {
    Optional<AssinaturaPrestador> findByTrabalhadorId(Long trabalhadorId);
    Optional<AssinaturaPrestador> findByCheckoutId(String checkoutId);
    Optional<AssinaturaPrestador> findBySubscriptionId(String subscriptionId);
    boolean existsByTrabalhadorIdAndStatusAndActiveUntilAfter(Long trabalhadorId, String status, Instant now);

    @Query("""
            select a.trabalhador.id
            from AssinaturaPrestador a
            where a.status = 'ACTIVE' and a.activeUntil > :now
            """)
    List<Long> findActiveTrabalhadorIds(@Param("now") Instant now);
}
