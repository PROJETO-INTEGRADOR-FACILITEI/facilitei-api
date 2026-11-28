package psg.facilitei.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponseDTO {

    private String id;
    private String nome;
    private String email;
    
    // Double e não String porque o front chama .toFixed() na nota
    private Double notaCliente; 
    
    private EnderecoResponseDTO endereco;
    private String telefone;
    private String avatarUrl;
}