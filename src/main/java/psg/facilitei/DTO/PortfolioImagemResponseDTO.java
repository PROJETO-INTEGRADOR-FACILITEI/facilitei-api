package psg.facilitei.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Imagem armazenada no portfolio")
public class PortfolioImagemResponseDTO {

    @Schema(description = "ID da imagem", example = "10")
    private Long id;

    @Schema(description = "URL segura da imagem no Cloudinary")
    private String url;
}
