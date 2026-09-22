package psg.facilitei.DTO;

import java.time.Instant;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import psg.facilitei.Entity.Enum.SupportCaseType;
import psg.facilitei.Entity.Enum.SupportCategory;
import psg.facilitei.Entity.Enum.SupportEventType;
import psg.facilitei.Entity.Enum.SupportPriority;
import psg.facilitei.Entity.Enum.SupportStatus;

public final class SupportCaseDTOs {
    private SupportCaseDTOs() {}

    public record CreateRequest(
            @NotNull SupportCaseType type,
            @NotNull SupportCategory category,
            @NotBlank @Size(max = 160) String subject,
            @NotBlank @Size(max = 3000) String description,
            Long serviceId,
            Long reportedUserId,
            @Size(max = 5) List<@Size(max = 500) String> evidenceUrls) {}

    public record MessageRequest(
            @NotBlank @Size(max = 2000) String message,
            boolean internalNote) {}

    public record AdminUpdateRequest(
            SupportStatus status,
            SupportPriority priority,
            Boolean assignToMe,
            Boolean unassign,
            @Size(max = 2000) String resolution) {}

    public record MessageResponse(
            Long id,
            Long authorId,
            String authorName,
            boolean admin,
            boolean internalNote,
            String message,
            Instant createdAt) {}

    public record HistoryResponse(
            Long id,
            SupportEventType eventType,
            String description,
            Long actorId,
            String actorName,
            Instant createdAt) {}

    public record SummaryResponse(
            Long id,
            String protocol,
            SupportCaseType type,
            SupportCategory category,
            SupportStatus status,
            SupportPriority priority,
            String subject,
            Long serviceId,
            String serviceTitle,
            Long reporterId,
            String reporterName,
            Long assignedAdminId,
            String assignedAdminName,
            Instant createdAt,
            Instant updatedAt) {}

    public record DetailResponse(
            Long id,
            String protocol,
            SupportCaseType type,
            SupportCategory category,
            SupportStatus status,
            SupportPriority priority,
            String subject,
            String description,
            Long serviceId,
            String serviceTitle,
            Long reporterId,
            String reporterName,
            Long reportedUserId,
            String reportedUserName,
            Long assignedAdminId,
            String assignedAdminName,
            List<String> evidenceUrls,
            String resolution,
            Instant createdAt,
            Instant updatedAt,
            Instant closedAt,
            List<MessageResponse> messages,
            List<HistoryResponse> history) {}

    public record PageResponse<T>(
            List<T> content,
            long totalElements,
            int totalPages,
            int page,
            int size,
            boolean first,
            boolean last) {}

    public record AdminMetricsResponse(
            long users,
            long clients,
            long workers,
            long services,
            long openCases,
            long openDisputes,
            long urgentCases,
            long unassignedCases,
            long createdLast24Hours) {}
}
