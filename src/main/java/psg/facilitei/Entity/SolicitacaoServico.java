package psg.facilitei.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import psg.facilitei.Entity.Enum.StatusSolicitacao;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitacao_servico")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitacaoServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Cliente é obrigatório")
    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Cliente cliente;

    // NOVO: Vincula diretamente ao trabalhador solicitado
    @NotNull(message = "Trabalhador é obrigatório")
    @ManyToOne
    @JoinColumn(name = "trabalhador_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Trabalhador trabalhador;

    // ALTERADO: Serviço agora é opcional (só existe após o trabalhador aceitar)
    @OneToOne
    @JoinColumn(name = "servico_id", nullable = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Servico servico;

    @NotNull(message = "O tipo de serviço é obrigatório")
    @Column(name = "tipo_servico", nullable = false)
    private String tipoServico;

    @NotBlank(message = "A descrição da solicitação é obrigatória.")
    @Column(name = "descricao", nullable = false, length = 500)
    private String descricao;

    @Column(name = "data_solicitacao", nullable = false)
    private LocalDateTime dataSolicitacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusSolicitacao statusSolicitacao;

    /** Serviço personalizado do profissional escolhido pelo cliente (opcional). */
    @ManyToOne
    @JoinColumn(name = "servico_oferecido_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private ServicoOferecido servicoOferecido;

    @Column(name = "titulo", length = 150)
    private String titulo;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;
}