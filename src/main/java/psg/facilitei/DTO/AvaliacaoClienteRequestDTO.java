package psg.facilitei.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Date;

@Data
public class AvaliacaoClienteRequestDTO {

    @Schema(description = "ID do trabalhador que avaliou")
    @NotNull(message = "O ID do trabalhador é obrigatório.")
    private Long trabalhadorId;

    @Schema(description = "Data de publicação")
    private Date data;

    @Schema(description = "ID do cliente avaliado")
    @NotNull(message = "O ID do cliente é obrigatório.")
    private Long clienteId;

    // --- ADICIONE ISTO ---
    @Schema(description = "ID do serviço realizado")
    private Long servicoId;
    // ---------------------

    @Schema(description = "Nota dada pelo trabalhador")
    @Min(value = 1, message = "A nota deve ser maior ou igual a 1.")
    @Max(value = 5, message = "A nota deve ser menor ou igual a 5.")
    private int nota;

    @Schema(description = "Comentário opcional do trabalhador")
    private String comentario;
}