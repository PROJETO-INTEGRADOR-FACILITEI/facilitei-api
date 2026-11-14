package psg.facilitei.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import psg.facilitei.Entity.Enum.AccountStatus;
import psg.facilitei.Entity.Enum.StatusServico;
import psg.facilitei.Entity.Enum.StatusSolicitacao;
import psg.facilitei.Entity.Enum.TipoServico;

public final class AdminDTOs {
    private AdminDTOs() {}

    public record UserSummary(
            Long id,
            String nome,
            String email,
            String telefone,
            String role,
            String avatarUrl,
            AccountStatus accountStatus,
            boolean blocked,
            Instant suspendedAt,
            Instant suspendedUntil,
            String suspensionReason,
            boolean admin,
            long totalServicos) {}

    public record UserPage(List<UserSummary> content, long totalElements, int totalPages, int page, int size) {}

    /** durationDays nulo = suspensão por tempo indeterminado. */
    public record SuspendRequest(
            @Min(1) @Max(3650) Integer durationDays,
            @NotBlank @Size(max = 2000) String reason) {}

    public record ReasonRequest(@NotBlank @Size(max = 2000) String reason) {}

    public record ServiceSummary(
            Long id,
            String titulo,
            String descricao,
            TipoServico tipoServico,
            StatusServico statusServico,
            Long clienteId,
            String clienteNome,
            Long trabalhadorId,
            String trabalhadorNome,
            String motivoCancelamento) {}

    public record RequestSummary(
            Long id,
            String titulo,
            String descricao,
            TipoServico tipoServico,
            StatusSolicitacao status,
            LocalDateTime dataSolicitacao,
            Long clienteId,
            String clienteNome,
            Long trabalhadorId,
            String trabalhadorNome) {}

    public record Overview(
            long usuarios,
            long clientes,
            long trabalhadores,
            long suspensos,
            long excluidos,
            long servicosAbertos,
            long servicosConcluidos,
            long servicosCancelados,
            long solicitacoesPendentes) {}
}
