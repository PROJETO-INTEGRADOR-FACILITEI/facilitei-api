package psg.facilitei.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import psg.facilitei.Entity.SupportCase;
import psg.facilitei.Entity.Enum.SupportCaseType;
import psg.facilitei.Entity.Enum.SupportPriority;
import psg.facilitei.Entity.Enum.SupportStatus;

public interface SupportCaseRepository extends JpaRepository<SupportCase, Long> {
    Page<SupportCase> findByReporterIdOrderByUpdatedAtDesc(Long reporterId, Pageable pageable);
    Optional<SupportCase> findByIdAndReporterId(Long id, Long reporterId);

    boolean existsByReporterIdAndServiceIdAndTypeAndStatusIn(
            Long reporterId, Long serviceId, SupportCaseType type, Collection<SupportStatus> statuses);

    @Query("""
            select c from SupportCase c
            where (:status is null or c.status = :status)
              and (:type is null or c.type = :type)
              and (:priority is null or c.priority = :priority)
              and (:assignedAdminId is null or c.assignedAdmin.id = :assignedAdminId)
              and (:unassigned = false or c.assignedAdmin is null)
              and (:search is null or lower(c.protocol) like lower(concat('%', :search, '%'))
                   or lower(c.subject) like lower(concat('%', :search, '%'))
                   or lower(c.reporter.nome) like lower(concat('%', :search, '%')))
            """)
    Page<SupportCase> searchAdmin(
            @Param("status") SupportStatus status,
            @Param("type") SupportCaseType type,
            @Param("priority") SupportPriority priority,
            @Param("assignedAdminId") Long assignedAdminId,
            @Param("unassigned") boolean unassigned,
            @Param("search") String search,
            Pageable pageable);

    long countByStatusIn(Collection<SupportStatus> statuses);
    long countByTypeAndStatusIn(SupportCaseType type, Collection<SupportStatus> statuses);
    long countByPriorityAndStatusIn(SupportPriority priority, Collection<SupportStatus> statuses);
    long countByAssignedAdminIsNullAndStatusIn(Collection<SupportStatus> statuses);
    long countByCreatedAtAfter(Instant createdAfter);
}
