package psg.facilitei.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import psg.facilitei.Domain.LegalAcceptance;
import psg.facilitei.Domain.LegalDocumentType;

public interface LegalAcceptanceRepository extends JpaRepository<LegalAcceptance, Long> {
    List<LegalAcceptance> findByUserIdAndUserRole(Long userId, String userRole);

    boolean existsByUserIdAndUserRoleAndDocumentTypeAndDocumentVersionAndDocumentSha256(
            Long userId,
            String userRole,
            LegalDocumentType documentType,
            String documentVersion,
            String documentSha256);
}
