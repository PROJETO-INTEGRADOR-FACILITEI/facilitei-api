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

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<PortfolioImagem> imagens = new ArrayList<>();

    public void adicionarImagem(PortfolioImagem imagem) {
        imagem.setPortfolio(this);
        imagens.add(imagem);
    }

    public void removerImagem(PortfolioImagem imagem) {
        imagens.remove(imagem);
        imagem.setPortfolio(null);
    }

}
