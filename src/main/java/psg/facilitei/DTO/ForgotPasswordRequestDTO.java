package psg.facilitei.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequestDTO {
    @NotBlank(message = "O email é obrigatório")
    @Email(message = "Email inválido")
    private String email;
}
