package psg.facilitei.Controller;

import static psg.facilitei.DTO.SupportCaseDTOs.*;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import psg.facilitei.Entity.Enum.SupportCaseType;
import psg.facilitei.Entity.Enum.SupportPriority;
import psg.facilitei.Entity.Enum.SupportStatus;
import psg.facilitei.Security.AccessControlService;
import psg.facilitei.Services.SupportCaseService;

@RestController
@RequestMapping("/api/admin/support")
public class AdminSupportController {
    private final SupportCaseService support;
    private final AccessControlService access;

    public AdminSupportController(SupportCaseService support, AccessControlService access) {
        this.support = support;
        this.access = access;
    }

    @GetMapping("/metrics")
    public AdminMetricsResponse metrics() {
        return support.metrics();
    }

    @GetMapping("/cases")
    public PageResponse<SummaryResponse> cases(
            @RequestParam(required = false) SupportStatus status,
            @RequestParam(required = false) SupportCaseType type,
            @RequestParam(required = false) SupportPriority priority,
            @RequestParam(required = false) Long assignedAdminId,
            @RequestParam(defaultValue = "false") boolean unassigned,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return support.searchAdmin(status, type, priority, assignedAdminId, unassigned, search,
                PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100),
                        Sort.by(Sort.Direction.DESC, "updatedAt")));
    }

    @GetMapping("/cases/{id}")
    public DetailResponse detail(@PathVariable Long id) {
        return support.getAdmin(id);
    }

    @PatchMapping("/cases/{id}")
    public DetailResponse update(@PathVariable Long id, @Valid @RequestBody AdminUpdateRequest request) {
        return support.updateAdmin(id, access.current().id(), request);
    }

    @PostMapping("/cases/{id}/messages")
    public DetailResponse message(@PathVariable Long id, @Valid @RequestBody MessageRequest request) {
        return support.addAdminMessage(id, access.current().id(), request);
    }
}
