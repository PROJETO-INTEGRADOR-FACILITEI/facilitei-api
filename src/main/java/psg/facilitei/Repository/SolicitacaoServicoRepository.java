package psg.facilitei.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import psg.facilitei.Entity.SolicitacaoServico;

import java.util.Optional;
import java.util.List;

@Repository
public interface SolicitacaoServicoRepository extends JpaRepository<SolicitacaoServico, Long>{

    Optional<SolicitacaoServico> findByServicoId(Long servicoId);

    List<SolicitacaoServico> findByClienteId(Long clienteId);

    List<SolicitacaoServico> findByTrabalhadorId(Long trabalhadorId);

    boolean existsByIdAndClienteId(Long id, Long clienteId);

    boolean existsByIdAndTrabalhadorId(Long id, Long trabalhadorId);
}
