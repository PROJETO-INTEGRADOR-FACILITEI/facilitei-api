package psg.facilitei.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "portfolio_imagens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioImagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Portfolio portfolio;

    @Column(name = "url_imagem", length = 500, nullable = false)
    private String url;

    @Column(name = "public_id", length = 255)
    private String publicId;
}
