package psg.facilitei.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import psg.facilitei.DTO.EnderecoRequestDTO;
import psg.facilitei.DTO.TrabalhadorRequestDTO;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Repository.AvaliacaoTrabalhadorRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class TrabalhadorServicePasswordPolicyTest {

    @Mock
    private PasswordHashService passwordHashService;
    @Mock
    private AvaliacaoTrabalhadorRepository avaliacaoTrabalhadorRepository;
    @Spy
    private PasswordPolicyService passwordPolicyService = new PasswordPolicyService();
    @InjectMocks
    private TrabalhadorService service;

    @Test
    void toEntity_comSenhaComum_naoCriaTrabalhador() {
        TrabalhadorRequestDTO dto = new TrabalhadorRequestDTO();
        dto.setNome("Profissional");
        dto.setEmail("profissional@example.com");
        dto.setSenha("senha12345678");
        dto.setEndereco(new EnderecoRequestDTO(
                "Rua A", "1", "Centro", "Recife", "PE", "50000000"));

        assertThrows(BusinessRuleException.class, () -> service.toEntity(dto));
    }
}
