package psg.facilitei.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import psg.facilitei.Entity.ServicoOferecido;

public interface ServicoOferecidoRepository extends JpaRepository<ServicoOferecido, Long> {
    List<ServicoOferecido> findByTrabalhadorIdAndAtivoTrueOrderByCreatedAtDesc(Long trabalhadorId);

    Optional<ServicoOferecido> findByIdAndTrabalhadorId(Long id, Long trabalhadorId);

    long countByTrabalhadorIdAndAtivoTrue(Long trabalhadorId);
}
