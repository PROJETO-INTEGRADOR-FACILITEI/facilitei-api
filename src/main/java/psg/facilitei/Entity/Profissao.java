package psg.facilitei.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Profissão que aparece na lista do cadastro e na busca. As da plataforma vêm prontas;
 * as personalizadas são criadas por profissionais que não se encaixaram em nenhuma.
 * O código (ex.: PEDREIRO, MONTADOR_DE_MOVEIS) é o valor guardado nas outras tabelas.
 */
@Entity
@Table(name = "profissao")
@Getter
@Setter
@NoArgsConstructor
public class Profissao {

    @Id
    @Column(length = 60)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(nullable = false)
    private boolean personalizada;

    @Column(nullable = false)
    private int ordem;

    @Column(name = "criada_por_id")
    private Long criadaPorId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Profissao(String codigo, String nome, boolean personalizada, int ordem, Long criadaPorId) {
        this.codigo = codigo;
        this.nome = nome;
        this.personalizada = personalizada;
        this.ordem = ordem;
        this.criadaPorId = criadaPorId;
    }
}
