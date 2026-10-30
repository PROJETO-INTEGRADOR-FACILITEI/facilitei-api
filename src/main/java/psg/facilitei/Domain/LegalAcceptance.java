package psg.facilitei.Domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "legal_acceptance", uniqueConstraints = @UniqueConstraint(
        name = "uk_legal_acceptance_document",
        columnNames = {"user_id", "user_role", "document_type", "document_version", "document_sha256"}))
public class LegalAcceptance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_role", nullable = false, length = 30)
    private String userRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private LegalDocumentType documentType;

    @Column(name = "document_version", nullable = false, length = 50)
    private String documentVersion;

    @Column(name = "document_sha256", nullable = false, length = 64)
    private String documentSha256;

    @Column(name = "acceptance_context", nullable = false, length = 30)
    private String acceptanceContext;

    @Column(name = "accepted_at", nullable = false, updatable = false)
    private Instant acceptedAt;

    @Column(name = "ip_hash", nullable = false, length = 64, updatable = false)
    private String ipHash;

    @Column(name = "user_agent_hash", nullable = false, length = 64, updatable = false)
    private String userAgentHash;

    @Column(name = "adult_confirmed", nullable = false, updatable = false)
    private boolean adultConfirmed;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
    public LegalDocumentType getDocumentType() { return documentType; }
    public void setDocumentType(LegalDocumentType documentType) { this.documentType = documentType; }
    public String getDocumentVersion() { return documentVersion; }
    public void setDocumentVersion(String documentVersion) { this.documentVersion = documentVersion; }
    public String getDocumentSha256() { return documentSha256; }
    public void setDocumentSha256(String documentSha256) { this.documentSha256 = documentSha256; }
    public String getAcceptanceContext() { return acceptanceContext; }
    public void setAcceptanceContext(String acceptanceContext) { this.acceptanceContext = acceptanceContext; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }
    public String getIpHash() { return ipHash; }
    public void setIpHash(String ipHash) { this.ipHash = ipHash; }
    public String getUserAgentHash() { return userAgentHash; }
    public void setUserAgentHash(String userAgentHash) { this.userAgentHash = userAgentHash; }
    public boolean isAdultConfirmed() { return adultConfirmed; }
    public void setAdultConfirmed(boolean adultConfirmed) { this.adultConfirmed = adultConfirmed; }
}
