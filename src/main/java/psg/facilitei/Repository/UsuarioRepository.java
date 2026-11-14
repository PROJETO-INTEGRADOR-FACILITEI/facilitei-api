package psg.facilitei.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import psg.facilitei.Entity.Usuario;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    java.util.List<Usuario> findByAccountStatusAndSuspendedUntilBefore(
            psg.facilitei.Entity.Enum.AccountStatus status, java.time.Instant instant);

    @org.springframework.data.jpa.repository.Query("""
            select u from Usuario u
            where (:search is null
                   or lower(u.nome) like lower(concat('%', :search, '%'))
                   or lower(u.email) like lower(concat('%', :search, '%')))
              and (:status is null or u.accountStatus = :status)
              and (:role is null
                   or (:role = 'cliente' and type(u) = psg.facilitei.Entity.Cliente)
                   or (:role = 'trabalhador' and type(u) = psg.facilitei.Entity.Trabalhador))
            """)
    org.springframework.data.domain.Page<Usuario> searchAdmin(
            @org.springframework.data.repository.query.Param("search") String search,
            @org.springframework.data.repository.query.Param("status") psg.facilitei.Entity.Enum.AccountStatus status,
            @org.springframework.data.repository.query.Param("role") String role,
            org.springframework.data.domain.Pageable pageable);
}
