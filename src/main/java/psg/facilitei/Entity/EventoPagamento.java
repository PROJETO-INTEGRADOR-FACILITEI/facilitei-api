package psg.facilitei.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "evento_pagamento")
public class EventoPagamento {
    @Id
    @Column(length = 180)
    private String id;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    protected EventoPagamento() {}
    public EventoPagamento(String id) { this.id = id; }
}
