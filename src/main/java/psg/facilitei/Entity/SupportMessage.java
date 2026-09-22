package psg.facilitei.Entity;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "support_message", indexes =
        @Index(name = "idx_support_message_case_created", columnList = "case_id,created_at"))
@Getter
@Setter
@NoArgsConstructor
public class SupportMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private SupportCase supportCase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Usuario author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "internal_note", nullable = false)
    private boolean internalNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
