package psg.facilitei.Entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import psg.facilitei.Entity.Enum.SupportCaseType;
import psg.facilitei.Entity.Enum.SupportCategory;
import psg.facilitei.Entity.Enum.SupportPriority;
import psg.facilitei.Entity.Enum.SupportStatus;

@Entity
@Table(name = "support_case", indexes = {
        @Index(name = "idx_support_case_reporter_updated", columnList = "reporter_id,updated_at"),
        @Index(name = "idx_support_case_queue", columnList = "status,priority,updated_at"),
        @Index(name = "idx_support_case_assignee", columnList = "assigned_admin_id,status")
})
@Getter
@Setter
@NoArgsConstructor
public class SupportCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String protocol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupportCaseType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SupportCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SupportStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupportPriority priority;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Usuario reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id")
    private Usuario reportedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private Servico service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_admin_id")
    private Usuario assignedAdmin;

    @Column(nullable = false, length = 160)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "support_case_evidence", joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "url", nullable = false, length = 500)
    @OrderColumn(name = "display_order")
    private List<String> evidenceUrls = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    @Column(nullable = false)
    private Long version;
}
