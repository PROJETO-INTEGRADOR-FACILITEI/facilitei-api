package psg.facilitei.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import psg.facilitei.Entity.Profissao;

public interface ProfissaoRepository extends JpaRepository<Profissao, String> {
    List<Profissao> findAllByOrderByPersonalizadaAscOrdemAscNomeAsc();

    Optional<Profissao> findFirstByNomeIgnoreCase(String nome);
}
