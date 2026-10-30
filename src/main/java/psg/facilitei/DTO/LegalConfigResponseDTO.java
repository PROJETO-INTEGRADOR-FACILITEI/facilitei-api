package psg.facilitei.DTO;

import java.time.LocalDate;
import java.util.Map;
import psg.facilitei.Config.LegalProperties.OperatorType;
import psg.facilitei.Config.LegalProperties.PaymentsMode;
import psg.facilitei.Domain.LegalDocumentType;

public record LegalConfigResponseDTO(
        OperatorType operatorType,
        String displayName,
        String legalName,
        String taxId,
        String businessAddress,
        String contactEmail,
        boolean profileComplete,
        PaymentsMode paymentsMode,
        boolean adultOnly,
        Map<LegalDocumentType, DocumentMetadata> documents) {

    public record DocumentMetadata(
            String version,
            LocalDate effectiveDate,
            boolean requiresReaccept,
            String sha256) {
    }
}
