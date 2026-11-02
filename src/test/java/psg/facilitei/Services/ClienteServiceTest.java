package psg.facilitei.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import psg.facilitei.DTO.ClienteRequestDTO;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Exceptions.ResourceNotFoundException;
import psg.facilitei.Repository.AvaliacaoClienteRepository;
import psg.facilitei.Repository.AvaliacaoServicoRepository;
import psg.facilitei.Repository.AvaliacaoTrabalhadorRepository;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.ServicoRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository repository;
    @Mock
    private AvaliacaoClienteRepository avaliacaoClienteRepository;
    @Mock
    private AvaliacaoServicoRepository avaliacaoServicoRepository;
    @Mock
    private AvaliacaoTrabalhadorRepository avaliacaoTrabalhadorRepository;
    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private TrabalhadorService trabalhadorService;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private PasswordHashService passwordHashService;
    @Mock
    private LegalAcceptanceService legalAcceptanceService;
    @Spy
    private PasswordPolicyService passwordPolicyService = new PasswordPolicyService();

    @InjectMocks
    private ClienteService clienteService;

    @Test
    void delete_existente_removeAvaliacoesVinculadasAntesDeDeletar() {
        when(repository.existsById(1L)).thenReturn(true);

        clienteService.delete(1L);

        verify(avaliacaoTrabalhadorRepository).deleteByClienteId(1L);
        verify(avaliacaoClienteRepository).deleteByClienteId(1L);
        verify(avaliacaoServicoRepository).deleteByClienteId(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void delete_inexistente_lancaResourceNotFoundException() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> clienteService.delete(99L));

        verify(avaliacaoTrabalhadorRepository, org.mockito.Mockito.never()).deleteByClienteId(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void delete_comServicosVinculados_convertFKViolationEmBusinessRuleException() {
        when(repository.existsById(1L)).thenReturn(true);
        doThrow(new DataIntegrityViolationException("FK violation"))
                .when(repository).flush();

        assertThrows(BusinessRuleException.class, () -> clienteService.delete(1L));
    }

    @Test
    void create_comSenhaComum_naoCriaCliente() {
        ClienteRequestDTO dto = new ClienteRequestDTO();
        dto.setSenha("senha12345678");

        assertThrows(BusinessRuleException.class,
                () -> clienteService.create(dto, "127.0.0.1", "test-agent"));
    }
}
