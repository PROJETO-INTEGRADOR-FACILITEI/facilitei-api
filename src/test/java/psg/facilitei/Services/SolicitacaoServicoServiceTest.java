package psg.facilitei.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import psg.facilitei.DTO.SolicitacaoServicoRequestDTO;
import psg.facilitei.DTO.SolicitacaoServicoResponseDTO;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Enum.StatusSolicitacao;
import psg.facilitei.Entity.Enum.TipoServico;
import psg.facilitei.Entity.SolicitacaoServico;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Exceptions.ResourceNotFoundException;
import psg.facilitei.Repository.SolicitacaoServicoRepository;
import psg.facilitei.Repository.TrabalhadorRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitacaoServicoServiceTest {

    @Mock
    private SolicitacaoServicoRepository solicitacaoServicoRepository;
    @Mock
    private ClienteService clienteService;
    @Mock
    private TrabalhadorRepository trabalhadorRepository;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SolicitacaoServicoService solicitacaoServicoService;

    @Test
    void criar_semStatusInformado_defineStatusComoPendente() {
        SolicitacaoServicoRequestDTO dto = new SolicitacaoServicoRequestDTO();
        dto.setClienteId(1L);
        dto.setTrabalhadorId(2L);
        dto.setDescricao("Preciso de um eletricista");
        dto.setTipoServico(TipoServico.ELETRICISTA);

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setId(2L);

        when(clienteService.buscarEntidadePorId(1L)).thenReturn(cliente);
        when(trabalhadorRepository.findById(2L)).thenReturn(Optional.of(trabalhador));
        when(solicitacaoServicoRepository.save(any(SolicitacaoServico.class))).thenAnswer(invocation -> {
            SolicitacaoServico s = invocation.getArgument(0);
            s.setId(7L);
            return s;
        });

        SolicitacaoServicoResponseDTO result = solicitacaoServicoService.criar(dto);

        assertEquals(StatusSolicitacao.PENDENTE.name(), result.getStatus());
        assertEquals(1L, result.getClienteId());
        assertEquals(7L, result.getId());
    }

    @Test
    void criar_comTrabalhadorInexistente_lancaResourceNotFoundException() {
        SolicitacaoServicoRequestDTO dto = new SolicitacaoServicoRequestDTO();
        dto.setClienteId(1L);
        dto.setTrabalhadorId(99L);

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        when(clienteService.buscarEntidadePorId(1L)).thenReturn(cliente);
        when(trabalhadorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> solicitacaoServicoService.criar(dto));
    }

    @Test
    void buscarPorId_inexistente_lancaResourceNotFoundException() {
        when(solicitacaoServicoRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> solicitacaoServicoService.buscarPorId(5L));
    }

    @Test
    void deletar_inexistente_lancaResourceNotFoundException() {
        when(solicitacaoServicoRepository.existsById(3L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> solicitacaoServicoService.deletar(3L));
    }
}
