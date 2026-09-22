package psg.facilitei.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import psg.facilitei.Entity.SupportMessage;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {
    List<SupportMessage> findBySupportCaseIdOrderByCreatedAtAsc(Long caseId);
    List<SupportMessage> findBySupportCaseIdAndInternalNoteFalseOrderByCreatedAtAsc(Long caseId);
}
