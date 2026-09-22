package psg.facilitei.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import psg.facilitei.Entity.SupportCaseHistory;
import psg.facilitei.Entity.Enum.SupportEventType;

public interface SupportCaseHistoryRepository extends JpaRepository<SupportCaseHistory, Long> {
    List<SupportCaseHistory> findBySupportCaseIdOrderByCreatedAtAsc(Long caseId);
    List<SupportCaseHistory> findBySupportCaseIdAndEventTypeNotOrderByCreatedAtAsc(
            Long caseId, SupportEventType excludedType);
}
