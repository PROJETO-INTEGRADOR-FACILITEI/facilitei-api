package psg.facilitei.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequestDTO {
    @NotBlank(message = "O token é obrigatório")
    private String token;

    @NotBlank(message = "A nova senha é obrigatória")
    @Size(min = 12, max = 72, message = "A senha deve ter entre 12 e 72 caracteres")
    private String novaSenha;
}
