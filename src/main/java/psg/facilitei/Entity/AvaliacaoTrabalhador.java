package psg.facilitei.Entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "avaliacao_trabalhador")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Nota media do trabalhador e comentários feitos em seus serviços")
public class AvaliacaoTrabalhador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Referenciado pelo mappedBy = "trabalhador" em Trabalhador
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trabalhador_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Trabalhador trabalhador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Cliente cliente;

    @Min(1)
    @Max(5)
    @Column(name = "nota", nullable = false)
    private int nota;

    @Column(name = "data", nullable = false)
    private Date data = new Date(System.currentTimeMillis());

    @Column(name = "comentario", length = 1000)
    private String comentario;

    @ElementCollection
    @CollectionTable(
            name = "avaliacao_trabalhador_fotos",
            joinColumns = @JoinColumn(name = "avaliacao_id")
    )
    @Column(name = "url_foto", length = 500)
    private List<String> fotos;
}