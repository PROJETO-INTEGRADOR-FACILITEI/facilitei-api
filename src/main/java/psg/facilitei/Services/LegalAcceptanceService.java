package psg.facilitei.Services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import psg.facilitei.DTO.LegalAcceptanceRequestDTO;
import psg.facilitei.DTO.LegalDocumentResponseDTO;
import psg.facilitei.DTO.LegalStatusResponseDTO;
import psg.facilitei.Domain.LegalAcceptance;
import psg.facilitei.Domain.LegalDocumentType;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Repository.LegalAcceptanceRepository;

@Service
public class LegalAcceptanceService {

    private static final Set<LegalDocumentType> GLOBAL_DOCUMENTS =
            EnumSet.of(LegalDocumentType.TERMS, LegalDocumentType.PRIVACY);

    private final LegalAcceptanceRepository repository;
    private final LegalDocumentService documents;

    public LegalAcceptanceService(
            LegalAcceptanceRepository repository,
            LegalDocumentService documents) {
        this.repository = repository;
        this.documents = documents;
    }

    @Transactional
    public LegalStatusResponseDTO accept(
            Long userId,
            String userRole,
            String context,
            LegalAcceptanceRequestDTO request,
            String remoteAddress,
            String userAgent) {
        validateIdentity(userId, userRole);
        if (request == null || !Boolean.TRUE.equals(request.adultConfirmed())) {
            throw new BusinessRuleException("É necessário confirmar que você possui 18 anos ou mais.");
        }
        if (request.documents() == null || request.documents().isEmpty()) {
            throw new BusinessRuleException("É necessário aceitar os documentos jurídicos vigentes.");
        }

        Map<LegalDocumentType, LegalAcceptanceRequestDTO.DocumentAcceptance> requested =
                new EnumMap<>(LegalDocumentType.class);
        for (var acceptance : request.documents()) {
            if (acceptance == null || acceptance.type() == null) {
                throw new BusinessRuleException("Documento jurídico inválido.");
            }
            if (requested.put(acceptance.type(), acceptance) != null) {
                throw new BusinessRuleException("O mesmo documento jurídico foi informado mais de uma vez.");
            }
            validateCurrentDocument(acceptance);
        }
        if ("REGISTRATION".equals(context) && !requested.keySet().containsAll(GLOBAL_DOCUMENTS)) {
            throw new BusinessRuleException("Aceite os Termos de Uso e a Política de Privacidade para criar a conta.");
        }

        Instant acceptedAt = Instant.now();
        String ipHash = sha256(normalizeIpPrefix(remoteAddress));
        String userAgentHash = sha256(normalizeMetadata(userAgent));
        List<LegalAcceptance> accepted = new ArrayList<>(repository.findByUserIdAndUserRole(userId, userRole));

        for (var requestDocument : requested.values()) {
            boolean exists = repository
                    .existsByUserIdAndUserRoleAndDocumentTypeAndDocumentVersionAndDocumentSha256(
                            userId, userRole, requestDocument.type(),
                            requestDocument.version(), requestDocument.sha256());
            if (exists) {
                accepted.add(toTransientAcceptance(userId, userRole, context,
                        requestDocument, acceptedAt, ipHash, userAgentHash));
                continue;
            }

            LegalAcceptance entity = toTransientAcceptance(userId, userRole, context,
                    requestDocument, acceptedAt, ipHash, userAgentHash);
            try {
                repository.save(entity);
                accepted.add(entity);
            } catch (DataIntegrityViolationException duplicateRace) {
                if (!repository.existsByUserIdAndUserRoleAndDocumentTypeAndDocumentVersionAndDocumentSha256(
                        userId, userRole, requestDocument.type(),
                        requestDocument.version(), requestDocument.sha256())) {
                    throw duplicateRace;
                }
                accepted.add(entity);
            }
        }
        return toStatus(accepted);
    }

    @Transactional(readOnly = true)
    public LegalStatusResponseDTO status(Long userId, String userRole) {
        validateIdentity(userId, userRole);
        return toStatus(repository.findByUserIdAndUserRole(userId, userRole));
    }

    private void validateIdentity(Long userId, String userRole) {
        if (userId == null || !("cliente".equals(userRole) || "trabalhador".equals(userRole))) {
            throw new BusinessRuleException("Usuário inválido para o registro do aceite jurídico.");
        }
    }

    private void validateCurrentDocument(LegalAcceptanceRequestDTO.DocumentAcceptance acceptance) {
        LegalDocumentResponseDTO current = documents.document(acceptance.type());
        if (!current.version().equals(acceptance.version())
                || !current.sha256().equalsIgnoreCase(normalizeMetadata(acceptance.sha256()))) {
            throw new BusinessRuleException(
                    "O documento " + acceptance.type().slug() + " mudou. Revise a versão atual antes de aceitar.");
        }
    }

    private LegalAcceptance toTransientAcceptance(
            Long userId,
            String userRole,
            String context,
            LegalAcceptanceRequestDTO.DocumentAcceptance request,
            Instant acceptedAt,
            String ipHash,
            String userAgentHash) {
        LegalAcceptance entity = new LegalAcceptance();
        entity.setUserId(userId);
        entity.setUserRole(userRole);
        entity.setDocumentType(request.type());
        entity.setDocumentVersion(request.version());
        entity.setDocumentSha256(request.sha256().toLowerCase(Locale.ROOT));
        entity.setAcceptanceContext(normalizeContext(context));
        entity.setAcceptedAt(acceptedAt);
        entity.setIpHash(ipHash);
        entity.setUserAgentHash(userAgentHash);
        entity.setAdultConfirmed(true);
        return entity;
    }

    private LegalStatusResponseDTO toStatus(List<LegalAcceptance> acceptances) {
        Map<LegalDocumentType, LegalStatusResponseDTO.DocumentStatus> statuses =
                new EnumMap<>(LegalDocumentType.class);
        boolean adultConfirmed = acceptances.stream().anyMatch(LegalAcceptance::isAdultConfirmed);
        boolean requiresReaccept = false;

        for (LegalDocumentType type : LegalDocumentType.values()) {
            LegalDocumentResponseDTO current = documents.document(type);
            boolean accepted = acceptances.stream().anyMatch(value ->
                    value.getDocumentType() == type
                            && current.version().equals(value.getDocumentVersion())
                            && current.sha256().equalsIgnoreCase(value.getDocumentSha256()));
            boolean documentRequiresReaccept = GLOBAL_DOCUMENTS.contains(type)
                    && current.requiresReaccept() && !accepted;
            requiresReaccept = requiresReaccept || documentRequiresReaccept;
            statuses.put(type, new LegalStatusResponseDTO.DocumentStatus(
                    current.version(), current.sha256(), accepted, documentRequiresReaccept));
        }
        return new LegalStatusResponseDTO(adultConfirmed, requiresReaccept, Map.copyOf(statuses));
    }

    private String normalizeContext(String context) {
        String normalized = normalizeMetadata(context).toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? "REACCEPT" : normalized.substring(0, Math.min(30, normalized.length()));
    }

    private String normalizeIpPrefix(String remoteAddress) {
        String ip = normalizeMetadata(remoteAddress);
        if (ip.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            String[] parts = ip.split("\\.");
            return parts[0] + "." + parts[1] + "." + parts[2] + ".0/24";
        }
        if (ip.contains(":")) {
            String[] parts = ip.split(":", -1);
            StringBuilder prefix = new StringBuilder();
            for (int i = 0; i < Math.min(3, parts.length); i++) {
                if (i > 0) prefix.append(':');
                prefix.append(parts[i]);
            }
            return prefix + "::/48";
        }
        return "unknown";
    }

    private String normalizeMetadata(String value) {
        return value == null ? "" : value.trim();
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível.", exception);
        }
    }
}
