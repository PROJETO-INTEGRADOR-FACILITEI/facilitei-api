package psg.facilitei.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados editáveis do perfil do cliente")
public class ClienteUpdateDTO {

    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
    private String nome;

    @Email(message = "E-mail inválido.")
    private String email;

    private String telefone;

    @Valid
    private EnderecoRequestDTO endereco;

    private String avatarUrl;
}
