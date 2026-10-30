package psg.facilitei.Services;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import psg.facilitei.Config.LegalProperties;
import psg.facilitei.DTO.LegalConfigResponseDTO;
import psg.facilitei.DTO.LegalDocumentResponseDTO;
import psg.facilitei.Domain.LegalDocumentType;

@Service
public class LegalDocumentService {

    private final LegalProperties properties;
    private final Map<LegalDocumentType, LegalDocumentResponseDTO> documents =
            new EnumMap<>(LegalDocumentType.class);

    public LegalDocumentService(LegalProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void loadDocuments() {
        for (LegalDocumentType type : LegalDocumentType.values()) {
            String markdown = readResource(type.resourcePath());
            LegalProperties.Document metadata = metadata(type);
            documents.put(type, new LegalDocumentResponseDTO(
                    type,
                    type.title(),
                    metadata.getVersion(),
                    metadata.getEffectiveDate(),
                    metadata.isRequiresReaccept(),
                    sha256(markdown),
                    markdown));
        }
    }

    public LegalConfigResponseDTO config() {
        Map<LegalDocumentType, LegalConfigResponseDTO.DocumentMetadata> metadata =
                new EnumMap<>(LegalDocumentType.class);
        documents.forEach((type, document) -> metadata.put(type,
                new LegalConfigResponseDTO.DocumentMetadata(
                        document.version(), document.effectiveDate(),
                        document.requiresReaccept(), document.sha256())));

        return new LegalConfigResponseDTO(
                properties.getOperatorType(),
                properties.getDisplayName(),
                blankToNull(properties.getLegalName()),
                blankToNull(properties.getTaxId()),
                blankToNull(properties.getBusinessAddress()),
                blankToNull(properties.getContactEmail()),
                properties.isProfileComplete(),
                properties.getPaymentsMode(),
                true,
                Map.copyOf(metadata));
    }

    public LegalDocumentResponseDTO document(String slug) {
        LegalDocumentType type = LegalDocumentType.fromSlug(slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Documento jurídico não encontrado."));
        return documents.get(type);
    }

    private LegalProperties.Document metadata(LegalDocumentType type) {
        return switch (type) {
            case TERMS -> properties.getDocuments().getTerms();
            case PRIVACY -> properties.getDocuments().getPrivacy();
            case SUBSCRIPTION -> properties.getDocuments().getSubscription();
        };
    }

    private String readResource(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível carregar o documento jurídico " + path, exception);
        }
    }

    private String sha256(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível.", exception);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
