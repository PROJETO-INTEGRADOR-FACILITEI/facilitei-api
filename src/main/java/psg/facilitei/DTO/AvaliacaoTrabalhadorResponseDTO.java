package psg.facilitei.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class AvaliacaoTrabalhadorResponseDTO {
    private Long id;
    private Long trabalhadorId;
    private Long clienteId;
    private int nota;
    private String comentario;
    private Date data;
    private List<String> fotos;
}
