package psg.facilitei.Controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import psg.facilitei.DTO.LegalConfigResponseDTO;
import psg.facilitei.DTO.LegalAcceptanceRequestDTO;
import psg.facilitei.DTO.LegalDocumentResponseDTO;
import psg.facilitei.DTO.LegalStatusResponseDTO;
import psg.facilitei.Domain.LegalDocumentType;
import psg.facilitei.Security.AccessControlService;
import psg.facilitei.Services.LegalAcceptanceService;
import psg.facilitei.Services.LegalDocumentService;

@RestController
@RequestMapping("/api/legal")
public class LegalController {

    private static final CacheControl PUBLIC_CACHE = CacheControl
            .maxAge(Duration.ofMinutes(5))
            .cachePublic();

    private final LegalDocumentService legalDocumentService;
    private final LegalAcceptanceService legalAcceptanceService;
    private final AccessControlService access;

    public LegalController(
            LegalDocumentService legalDocumentService,
            LegalAcceptanceService legalAcceptanceService,
            AccessControlService access) {
        this.legalDocumentService = legalDocumentService;
        this.legalAcceptanceService = legalAcceptanceService;
        this.access = access;
    }

    @GetMapping("/config")
    public ResponseEntity<LegalConfigResponseDTO> config() {
        return ResponseEntity.ok()
                .cacheControl(PUBLIC_CACHE)
                .body(legalDocumentService.config());
    }

    @GetMapping("/documents/{type}")
    public ResponseEntity<LegalDocumentResponseDTO> document(@PathVariable String type) {
        return ResponseEntity.ok()
                .cacheControl(PUBLIC_CACHE)
                .body(legalDocumentService.document(type));
    }

    @GetMapping("/status")
    public LegalStatusResponseDTO status() {
        var principal = access.current();
        return legalAcceptanceService.status(principal.id(), principal.role());
    }

    @PostMapping("/acceptances")
    public LegalStatusResponseDTO accept(
            @Valid @RequestBody LegalAcceptanceRequestDTO request,
            HttpServletRequest httpRequest) {
        var principal = access.current();
        String context = request.documents() != null
                && request.documents().size() == 1
                && request.documents().getFirst().type() == LegalDocumentType.SUBSCRIPTION
                ? "CHECKOUT" : "REACCEPT";
        return legalAcceptanceService.accept(
                principal.id(), principal.role(), context, request,
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
    }
}
