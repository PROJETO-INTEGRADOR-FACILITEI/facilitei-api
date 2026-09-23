package psg.facilitei.Entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "assinatura_prestador")
public class AssinaturaPrestador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trabalhador_id", nullable = false, unique = true)
    private Trabalhador trabalhador;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider = "MERCADOPAGO";

    @Column(name = "checkout_id", unique = true, length = 100)
    private String checkoutId;

    @Column(name = "checkout_url", length = 500)
    private String checkoutUrl;

    @Column(name = "subscription_id", unique = true, length = 100)
    private String subscriptionId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "amount_cents")
    private Long amountCents;

    @Column(name = "active_until")
    private Instant activeUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Trabalhador getTrabalhador() { return trabalhador; }
    public void setTrabalhador(Trabalhador trabalhador) { this.trabalhador = trabalhador; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getCheckoutId() { return checkoutId; }
    public void setCheckoutId(String checkoutId) { this.checkoutId = checkoutId; }
    public String getCheckoutUrl() { return checkoutUrl; }
    public void setCheckoutUrl(String checkoutUrl) { this.checkoutUrl = checkoutUrl; }
    public String getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(String subscriptionId) { this.subscriptionId = subscriptionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getAmountCents() { return amountCents; }
    public void setAmountCents(Long amountCents) { this.amountCents = amountCents; }
    public Instant getActiveUntil() { return activeUntil; }
    public void setActiveUntil(Instant activeUntil) { this.activeUntil = activeUntil; }
}
