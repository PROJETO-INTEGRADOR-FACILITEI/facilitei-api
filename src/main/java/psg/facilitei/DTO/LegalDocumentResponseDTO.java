package psg.facilitei.DTO;

import java.time.LocalDate;
import psg.facilitei.Domain.LegalDocumentType;

public record LegalDocumentResponseDTO(
        LegalDocumentType type,
        String title,
        String version,
        LocalDate effectiveDate,
        boolean requiresReaccept,
        String sha256,
        String markdown) {
}
