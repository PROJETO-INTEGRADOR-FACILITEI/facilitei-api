package psg.facilitei.Entity;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor; 
import lombok.Data;
import lombok.NoArgsConstructor; 
import lombok.EqualsAndHashCode; 
import lombok.ToString; 
import psg.facilitei.Entity.Enum.AccountStatus;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Data 
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "telefone")
    private String telefone;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "senha", nullable = false)
    private String senha;

    @Column(name = "url_foto")
    private String urlFoto;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "suspension_reason", length = 2000)
    private String suspensionReason;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "endereco_id", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude 
    @ToString.Exclude 
    private Endereco endereco;


}
