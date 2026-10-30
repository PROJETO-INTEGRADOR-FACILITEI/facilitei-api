package psg.facilitei.Controller;

import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import psg.facilitei.DTO.LegalConfigResponseDTO;
import psg.facilitei.DTO.LegalDocumentResponseDTO;
import psg.facilitei.Services.LegalDocumentService;

@RestController
@RequestMapping("/api/legal")
public class LegalController {

    private static final CacheControl PUBLIC_CACHE = CacheControl
            .maxAge(Duration.ofMinutes(5))
            .cachePublic();

    private final LegalDocumentService legalDocumentService;

    public LegalController(LegalDocumentService legalDocumentService) {
        this.legalDocumentService = legalDocumentService;
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
}
