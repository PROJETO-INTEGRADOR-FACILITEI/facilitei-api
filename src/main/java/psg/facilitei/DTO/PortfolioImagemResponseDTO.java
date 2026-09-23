package psg.facilitei.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import psg.facilitei.Entity.Enum.TipoServico;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Imagem armazenada no portfolio")
public class PortfolioImagemResponseDTO {

    @Schema(description = "ID da imagem", example = "10")
    private Long id;

    @Schema(description = "URL pública da imagem armazenada no S3")
    private String url;

    @Schema(description = "Especialidade usada para agrupar a imagem", example = "ELETRICISTA")
    private TipoServico tipoServico;
}
