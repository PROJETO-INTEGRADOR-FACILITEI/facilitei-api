package psg.facilitei.Entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "evento_abacatepay")
public class EventoAbacatePay {
    @Id
    @Column(length = 100)
    private String id;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    protected EventoAbacatePay() {}
    public EventoAbacatePay(String id) { this.id = id; }
}
