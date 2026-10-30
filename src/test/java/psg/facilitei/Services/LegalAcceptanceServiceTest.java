package psg.facilitei.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import psg.facilitei.Config.LegalProperties;
import psg.facilitei.DTO.LegalAcceptanceRequestDTO;
import psg.facilitei.DTO.LegalStatusResponseDTO;
import psg.facilitei.Domain.LegalAcceptance;
import psg.facilitei.Domain.LegalDocumentType;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Repository.LegalAcceptanceRepository;

class LegalAcceptanceServiceTest {

    private LegalAcceptanceRepository repository;
    private LegalDocumentService documents;
    private LegalAcceptanceService service;

    @BeforeEach
    void setUp() {
        repository = mock(LegalAcceptanceRepository.class);
        LegalProperties properties = new LegalProperties();
        properties.setContactEmail("contato@facilitei.online");
        documents = new LegalDocumentService(properties);
        documents.loadDocuments();
        service = new LegalAcceptanceService(repository, documents);
    }

    @Test
    void registraAceiteValidoSemGuardarIpOuUserAgentEmClaro() {
        LegalAcceptanceRequestDTO request = currentRequest(true);

        LegalStatusResponseDTO status = service.accept(
                42L, "cliente", "REGISTRATION", request,
                "192.168.10.87", "Mozilla/5.0 teste");

        ArgumentCaptor<LegalAcceptance> captor = ArgumentCaptor.forClass(LegalAcceptance.class);
        verify(repository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertTrue(status.adultConfirmed());
        assertFalse(status.requiresReaccept());
        assertTrue(captor.getAllValues().stream().allMatch(LegalAcceptance::isAdultConfirmed));
        assertTrue(captor.getAllValues().stream().allMatch(value -> value.getIpHash().matches("[0-9a-f]{64}")));
        assertTrue(captor.getAllValues().stream().allMatch(value -> value.getUserAgentHash().matches("[0-9a-f]{64}")));
        assertTrue(captor.getAllValues().stream().noneMatch(value -> value.getIpHash().contains("192.168")));
    }

    @Test
    void rejeitaVersaoOuHashQueNaoCorrespondemAoDocumentoPublicado() {
        var terms = documents.document("terms");
        var privacy = documents.document("privacy");
        LegalAcceptanceRequestDTO request = new LegalAcceptanceRequestDTO(true, List.of(
                new LegalAcceptanceRequestDTO.DocumentAcceptance(
                        LegalDocumentType.TERMS, "0.9.0", terms.sha256()),
                new LegalAcceptanceRequestDTO.DocumentAcceptance(
                        LegalDocumentType.PRIVACY, privacy.version(), privacy.sha256())));

        assertThrows(BusinessRuleException.class, () -> service.accept(
                42L, "cliente", "REGISTRATION", request, "127.0.0.1", "test"));
        verify(repository, never()).save(any());
    }

    @Test
    void rejeitaCadastroSemConfirmacaoDeMaioridade() {
        assertThrows(BusinessRuleException.class, () -> service.accept(
                42L, "cliente", "REGISTRATION", currentRequest(false), "127.0.0.1", "test"));
        verify(repository, never()).save(any());
    }

    @Test
    void repeticaoDoMesmoAceiteEhIdempotente() {
        when(repository.existsByUserIdAndUserRoleAndDocumentTypeAndDocumentVersionAndDocumentSha256(
                any(), any(), any(), any(), any())).thenReturn(true);

        LegalStatusResponseDTO status = service.accept(
                42L, "cliente", "REGISTRATION", currentRequest(true), "127.0.0.1", "test");

        verify(repository, never()).save(any());
        assertFalse(status.requiresReaccept());
    }

    @Test
    void versaoMaterialNovaExigeReaceite() {
        var oldTerms = new LegalAcceptance();
        oldTerms.setUserId(42L);
        oldTerms.setUserRole("cliente");
        oldTerms.setDocumentType(LegalDocumentType.TERMS);
        oldTerms.setDocumentVersion("0.9.0");
        oldTerms.setDocumentSha256("a".repeat(64));
        oldTerms.setAdultConfirmed(true);
        when(repository.findByUserIdAndUserRole(42L, "cliente")).thenReturn(List.of(oldTerms));

        LegalStatusResponseDTO status = service.status(42L, "cliente");

        assertTrue(status.requiresReaccept());
        assertFalse(status.documents().get(LegalDocumentType.TERMS).accepted());
        assertTrue(status.documents().get(LegalDocumentType.TERMS).requiresReaccept());
    }

    private LegalAcceptanceRequestDTO currentRequest(boolean adultConfirmed) {
        var terms = documents.document("terms");
        var privacy = documents.document("privacy");
        return new LegalAcceptanceRequestDTO(adultConfirmed, List.of(
                new LegalAcceptanceRequestDTO.DocumentAcceptance(
                        LegalDocumentType.TERMS, terms.version(), terms.sha256()),
                new LegalAcceptanceRequestDTO.DocumentAcceptance(
                        LegalDocumentType.PRIVACY, privacy.version(), privacy.sha256())));
    }
}
