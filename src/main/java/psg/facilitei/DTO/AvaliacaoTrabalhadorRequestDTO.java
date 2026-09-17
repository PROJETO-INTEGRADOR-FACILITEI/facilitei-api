package psg.facilitei.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class AvaliacaoTrabalhadorRequestDTO {
    @NotNull(message = "O ID do cliente é obrigatório.")
    private Long clienteId;
    @NotNull(message = "O ID do trabalhador é obrigatório.")
    private Long trabalhadorId;
    private Long servicoId;
    @Min(value = 1, message = "A nota deve ser maior ou igual a 1.")
    @Max(value = 5, message = "A nota deve ser menor ou igual a 5.")
    private int nota;
    private String comentario;
    private List<String> fotos;
}