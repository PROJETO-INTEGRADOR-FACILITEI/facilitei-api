package psg.facilitei.DTO;

import java.util.Date;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ClienteResumoDTO {
    private Long id;
    private String nome;
    private String avatarUrl;

    // Reputação do cliente: ajuda o profissional a decidir se aceita o pedido.
    /** Média das notas recebidas de profissionais (null = ainda sem avaliações). */
    private Double notaCliente;
    private Long quantidadeAvaliacoes;
    private Long servicosConcluidos;
    private List<AvaliacaoRecente> avaliacoesRecentes;

    public record AvaliacaoRecente(int nota, String comentario, String autorNome, Date data) {}

    public ClienteResumoDTO(Long id, String nome, String avatarUrl) {
        this.id = id;
        this.nome = nome;
        this.avatarUrl = avatarUrl;
    }
}
