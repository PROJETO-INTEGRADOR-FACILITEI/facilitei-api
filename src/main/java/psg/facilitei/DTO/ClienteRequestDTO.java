package psg.facilitei.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteRequestDTO {

    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
    private String nome;
    @NotBlank(message = "O email é obrigatório.")
    @Email(message = "Email inválido.")
    private String email;
    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 12, max = 72, message = "A senha deve ter entre 12 e 72 caracteres.")
    private String senha;
    @NotNull(message = "O Endereço é obrigatorio..")
    @Valid
    private EnderecoRequestDTO endereco;
    private String telefone;
    private String avatarUrl;
}
