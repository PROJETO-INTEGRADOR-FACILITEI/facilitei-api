package psg.facilitei.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import psg.facilitei.Entity.Portfolio;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    Optional<Portfolio> findByTrabalhadorId(Long trabalhadorId);

    boolean existsByTrabalhadorId(Long trabalhadorId);

    boolean existsByIdAndTrabalhadorId(Long id, Long trabalhadorId);
}
