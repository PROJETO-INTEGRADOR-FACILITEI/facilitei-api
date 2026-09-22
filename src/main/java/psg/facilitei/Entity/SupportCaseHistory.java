package psg.facilitei.Entity;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import psg.facilitei.Entity.Enum.SupportEventType;

@Entity
@Table(name = "support_case_history", indexes =
        @Index(name = "idx_support_history_case_created", columnList = "case_id,created_at"))
@Getter
@Setter
@NoArgsConstructor
public class SupportCaseHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private SupportCase supportCase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private Usuario actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private SupportEventType eventType;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
