package psg.facilitei.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import psg.facilitei.Domain.LegalDocumentType;

public record LegalAcceptanceRequestDTO(
        @NotNull(message = "Confirme que você possui 18 anos ou mais.")
        Boolean adultConfirmed,
        @NotEmpty(message = "Informe os documentos jurídicos aceitos.")
        List<@Valid DocumentAcceptance> documents) {

    public record DocumentAcceptance(
            @NotNull(message = "Informe o tipo do documento.") LegalDocumentType type,
            @NotNull(message = "Informe a versão do documento.") String version,
            @NotNull(message = "Informe o hash do documento.") String sha256) {
    }
}
