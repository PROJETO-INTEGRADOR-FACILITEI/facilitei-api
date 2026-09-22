package psg.facilitei.Controller;

import static psg.facilitei.DTO.SupportCaseDTOs.*;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import psg.facilitei.Security.AccessControlService;
import psg.facilitei.Services.SupportCaseService;

@RestController
@RequestMapping("/api/support/cases")
public class SupportController {
    private final SupportCaseService support;
    private final AccessControlService access;

    public SupportController(SupportCaseService support, AccessControlService access) {
        this.support = support;
        this.access = access;
    }

    @PostMapping
    public ResponseEntity<DetailResponse> create(@Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(support.create(access.current().id(), request));
    }

    @GetMapping
    public PageResponse<SummaryResponse> list(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "12") int size) {
        return support.listMine(access.current().id(), PageRequest.of(
                Math.max(0, page), Math.min(Math.max(1, size), 50), Sort.by(Sort.Direction.DESC, "updatedAt")));
    }

    @GetMapping("/{id}")
    public DetailResponse detail(@PathVariable Long id) {
        return support.getMine(id, access.current().id());
    }

    @PostMapping("/{id}/messages")
    public DetailResponse message(@PathVariable Long id, @Valid @RequestBody MessageRequest request) {
        return support.addUserMessage(id, access.current().id(), new MessageRequest(request.message(), false));
    }
}
