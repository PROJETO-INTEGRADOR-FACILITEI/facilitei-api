package psg.facilitei.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import psg.facilitei.Entity.Enum.TipoServico;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados editáveis do perfil profissional")
public class TrabalhadorUpdateDTO {

    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
    private String nome;

    @Email(message = "E-mail inválido.")
    private String email;

    private String telefone;

    @Valid
    private EnderecoRequestDTO endereco;

    private List<TipoServico> habilidades;
    private TipoServico servicoPrincipal;
    private String disponibilidade;

    @Size(max = 200, message = "A descrição deve ter no máximo 200 caracteres.")
    private String sobre;

    private String avatarUrl;
}
