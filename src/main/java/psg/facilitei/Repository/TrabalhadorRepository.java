package psg.facilitei.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import psg.facilitei.Entity.Trabalhador;

public interface TrabalhadorRepository extends JpaRepository<Trabalhador, Long>, JpaSpecificationExecutor<Trabalhador> {
    Optional<Trabalhador> findByEmail(String email);
}
