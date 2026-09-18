package psg.facilitei.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import psg.facilitei.Entity.Trabalhador;

public interface TrabalhadorRepository extends JpaRepository<Trabalhador, Long>, JpaSpecificationExecutor<Trabalhador> {
    Optional<Trabalhador> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trabalhador t where t.id = :id")
    Optional<Trabalhador> lockById(@Param("id") Long id);
}
