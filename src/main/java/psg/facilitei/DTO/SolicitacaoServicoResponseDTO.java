package psg.facilitei.DTO;

import java.time.LocalDateTime;
import psg.facilitei.Entity.Enum.TipoServico;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "DTO para resposta de informações de Solicitação de Serviço")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitacaoServicoResponseDTO {
    private Long id;
    private Long clienteId;
    private Long trabalhadorId;
    private Long servicoId; // Pode vir nulo se ainda não foi aceito
    private TipoServico tipoServico;
    private String descricao;
    private String status;
    private ClienteResumoDTO cliente;

    @Schema(description = "Título do pedido (serviço personalizado do profissional, quando escolhido)")
    private String titulo;

    @Schema(description = "ID do serviço personalizado do profissional, quando escolhido")
    private Long servicoOferecidoId;

    @Schema(description = "Nome do profissional solicitado")
    private String trabalhadorNome;

    @Schema(description = "Foto do profissional solicitado")
    private String trabalhadorAvatarUrl;

    private LocalDateTime dataSolicitacao;

    private String motivoCancelamento;

    public SolicitacaoServicoResponseDTO(Long id, Long clienteId, Long trabalhadorId, Long servicoId,
                                         TipoServico tipoServico, String descricao, String status,
                                         ClienteResumoDTO cliente) {
        this.id = id;
        this.clienteId = clienteId;
        this.trabalhadorId = trabalhadorId;
        this.servicoId = servicoId;
        this.tipoServico = tipoServico;
        this.descricao = descricao;
        this.status = status;
        this.cliente = cliente;
    }
}
