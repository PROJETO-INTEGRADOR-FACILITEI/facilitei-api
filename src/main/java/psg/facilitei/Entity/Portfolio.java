package psg.facilitei.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "portfolio")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "O trabalhador é obrigatório")
    @OneToOne
    @JoinColumn(name = "trabalhador_id", nullable = false, unique = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Trabalhador trabalhador;

    @ElementCollection
    @CollectionTable(name = "portfolio_imagens", joinColumns = @JoinColumn(name = "portfolio_id"))
    @Column(name = "url_imagem", length = 500)
    private List<String> urlsImagens = new ArrayList<>();

}
