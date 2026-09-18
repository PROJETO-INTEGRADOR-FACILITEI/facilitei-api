package psg.facilitei.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para resposta de informações de Portfolio")
public class PortfolioResponseDTO {

    @Schema(description = "ID único do portfolio", example = "1")
    private Long id;

    @Schema(description = "ID do trabalhador dono do portfolio", example = "1")
    private Long trabalhadorId;

    @Schema(description = "Imagens do portfolio")
    private List<PortfolioImagemResponseDTO> imagens;
}
