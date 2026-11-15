package psg.facilitei.Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Serviço criado pelo próprio profissional, com título, descrição e preço livres. */
@Entity
@Table(name = "servico_oferecido")
@Data
@NoArgsConstructor
public class ServicoOferecido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trabalhador_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Trabalhador trabalhador;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(precision = 10, scale = 2)
    private BigDecimal preco;

    /** Categoria usada na busca. Opcional: o serviço pode não se encaixar nas categorias fixas. */
    @Column(name = "tipo_servico")
    private String tipoServico;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
