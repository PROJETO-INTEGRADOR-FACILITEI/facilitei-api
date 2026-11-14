package psg.facilitei.Exceptions;

import java.time.Instant;

/** Conta bloqueada por punição administrativa. O corpo da resposta mostra o motivo e o prazo ao usuário. */
public class AccountSuspendedException extends org.springframework.web.server.ResponseStatusException {
    private final String reason;
    private final Instant until;
    private final boolean deleted;

    public AccountSuspendedException(String message, String reason, Instant until, boolean deleted) {
        super(org.springframework.http.HttpStatus.FORBIDDEN, message);
        this.reason = reason;
        this.until = until;
        this.deleted = deleted;
    }

    /** Motivo da punição. O texto completo exibido ao usuário (prazo + motivo) fica em {@link #getReason()}. */
    public String getPunishmentReason() { return reason; }
    public Instant getUntil() { return until; }
    public boolean isDeleted() { return deleted; }
}
