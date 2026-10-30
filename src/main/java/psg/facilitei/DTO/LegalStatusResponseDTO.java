package psg.facilitei.DTO;

import java.util.Map;
import psg.facilitei.Domain.LegalDocumentType;

public record LegalStatusResponseDTO(
        boolean adultConfirmed,
        boolean requiresReaccept,
        Map<LegalDocumentType, DocumentStatus> documents) {

    public record DocumentStatus(
            String version,
            String sha256,
            boolean accepted,
            boolean requiresReaccept) {
    }
}
