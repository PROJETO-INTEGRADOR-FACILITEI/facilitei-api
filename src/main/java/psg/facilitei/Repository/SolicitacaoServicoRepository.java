package psg.facilitei.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import psg.facilitei.Entity.SolicitacaoServico;

import java.util.Optional;

@Repository
public interface SolicitacaoServicoRepository extends JpaRepository<SolicitacaoServico, Long>{

    Optional<SolicitacaoServico> findByServicoId(Long servicoId);
}
