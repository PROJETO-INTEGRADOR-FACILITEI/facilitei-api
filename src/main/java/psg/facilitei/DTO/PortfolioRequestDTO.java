package psg.facilitei.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import psg.facilitei.Entity.Enum.TipoServico;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para requisição de criação de Portfolio")
public class PortfolioRequestDTO {

    @NotNull(message = "O ID do trabalhador é obrigatório.")
    @Schema(description = "ID do trabalhador dono do portfolio", example = "1")
    private Long trabalhadorId;

    @NotEmpty(message = "É necessário enviar ao menos uma imagem.")
    @Schema(description = "Imagens do portfolio a serem armazenadas no S3")
    private List<MultipartFile> imagens;

    @NotNull(message = "O tipo de serviço é obrigatório.")
    @Schema(description = "Especialidade à qual as imagens pertencem", example = "ELETRICISTA")
    private TipoServico tipoServico;
}
