package psg.facilitei.Services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import psg.facilitei.DTO.ServicoRequestDTO;
import psg.facilitei.DTO.ServicoResponseDTO;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Enum.StatusSolicitacao;
import psg.facilitei.Entity.Enum.StatusServico;
import psg.facilitei.Entity.Enum.TipoServico;
import psg.facilitei.Entity.Servico;
import psg.facilitei.Entity.SolicitacaoServico;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Exceptions.ResourceNotFoundException;
import psg.facilitei.Repository.AvaliacaoServicoRepository;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.ServicoRepository;
import psg.facilitei.Repository.SolicitacaoServicoRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicoServiceTest {

    @Mock
    private ServicoRepository servicoRepository;
    @Spy
    private ModelMapper modelMapper = new ModelMapper();
    @Mock
    private TrabalhadorService trabalhadorService;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private AvaliacaoServicoRepository avaliacaoServicoRepository;
    @Mock
    private SolicitacaoServicoRepository solicitacaoServicoRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ServicoService servicoService;

    @BeforeEach
    void configureModelMapper() {
        // Mirrors psg.facilitei.Config.ModelMapperConfig so this unit test's plain
        // ModelMapper behaves like the Spring-managed bean instead of hitting the
        // Servico.setId() ambiguity between clienteId/trabalhadorId under LOOSE matching.
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.createTypeMap(ServicoRequestDTO.class, Servico.class)
                .addMappings(mapper -> {
                    mapper.skip(Servico::setId);
                    mapper.skip(Servico::setTrabalhador);
                    mapper.skip(Servico::setCliente);
                    mapper.skip(Servico::setSolicitacao);
                });
    }

    @Test
    void buscarPorId_existente_retornaDTO() {
        Servico servico = new Servico();
        servico.setId(1L);
        servico.setTitulo("Reparo elétrico");
        servico.setDescricao("Troca de disjuntor");
        servico.setTipoServico(TipoServico.ELETRICISTA);
        servico.setStatusServico(StatusServico.PENDENTE);

        when(servicoRepository.findById(1L)).thenReturn(Optional.of(servico));

        ServicoResponseDTO dto = servicoService.buscarPorId(1L);

        assertEquals(1L, dto.getId());
        assertEquals("Reparo elétrico", dto.getTitulo());
    }

    @Test
    void buscarPorId_inexistente_lancaResourceNotFoundException() {
        when(servicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicoService.buscarPorId(99L));
    }

    @Test
    void criar_semStatusInformado_defineStatusComoPendente() {
        ServicoRequestDTO dto = new ServicoRequestDTO();
        dto.setTitulo("Pintura de sala");
        dto.setDescricao("Pintura completa");
        dto.setTipoServico(TipoServico.PINTOR);
        dto.setTrabalhadorId(1L);
        dto.setClienteId(2L);

        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setId(1L);
        Cliente cliente = new Cliente();
        cliente.setId(2L);

        when(trabalhadorService.buscarEntidadePorId(1L)).thenReturn(trabalhador);
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(cliente));
        when(servicoRepository.save(any(Servico.class))).thenAnswer(invocation -> {
            Servico s = invocation.getArgument(0);
            s.setId(5L);
            return s;
        });

        ServicoResponseDTO result = servicoService.criar(dto);

        assertEquals(StatusServico.PENDENTE, result.getStatusServico());
    }

    @Test
    void criar_comSolicitacaoPendente_vinculaServicoEMarcaComoAceita() {
        ServicoRequestDTO dto = new ServicoRequestDTO();
        dto.setTitulo("Pintura de sala");
        dto.setDescricao("Pintura completa");
        dto.setTipoServico(TipoServico.PINTOR);
        dto.setTrabalhadorId(1L);
        dto.setClienteId(2L);
        dto.setSolicitacaoId(7L);
        dto.setStatusServico(StatusServico.EM_ANDAMENTO);

        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setId(1L);
        Cliente cliente = new Cliente();
        cliente.setId(2L);
        SolicitacaoServico solicitacao = new SolicitacaoServico();
        solicitacao.setId(7L);
        solicitacao.setTrabalhador(trabalhador);
        solicitacao.setCliente(cliente);
        solicitacao.setTipoServico(TipoServico.PINTOR);
        solicitacao.setStatusSolicitacao(StatusSolicitacao.PENDENTE);

        when(trabalhadorService.buscarEntidadePorId(1L)).thenReturn(trabalhador);
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(cliente));
        when(solicitacaoServicoRepository.findById(7L)).thenReturn(Optional.of(solicitacao));
        when(servicoRepository.save(any(Servico.class))).thenAnswer(invocation -> {
            Servico s = invocation.getArgument(0);
            s.setId(5L);
            return s;
        });

        ServicoResponseDTO result = servicoService.criar(dto);

        assertEquals(StatusServico.EM_ANDAMENTO, result.getStatusServico());
        assertEquals(StatusSolicitacao.ACEITA, solicitacao.getStatusSolicitacao());
        assertEquals(5L, solicitacao.getServico().getId());
        assertEquals(7L, solicitacao.getServico().getSolicitacao().getId());
        verify(solicitacaoServicoRepository).save(solicitacao);
    }

    @Test
    void deletar_inexistente_lancaResourceNotFoundException() {
        when(servicoRepository.existsById(42L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> servicoService.deletar(42L));

        verify(avaliacaoServicoRepository, never()).deleteByServicoId(any());
    }

    @Test
    void deletar_existente_removeAvaliacoesEDesvinculaSolicitacaoAntesDeDeletar() {
        SolicitacaoServico solicitacao = new SolicitacaoServico();
        solicitacao.setId(7L);
        Servico servico = new Servico();
        servico.setId(1L);
        solicitacao.setServico(servico);

        when(servicoRepository.existsById(1L)).thenReturn(true);
        when(solicitacaoServicoRepository.findByServicoId(1L)).thenReturn(Optional.of(solicitacao));

        servicoService.deletar(1L);

        verify(avaliacaoServicoRepository).deleteByServicoId(1L);
        assertNull(solicitacao.getServico());
        verify(solicitacaoServicoRepository).save(solicitacao);
        verify(servicoRepository).deleteById(1L);
    }

    @Test
    void listarPorCliente_delegaParaRepository() {
        Servico servico = new Servico();
        servico.setId(1L);
        servico.setTipoServico(TipoServico.ENCANADOR);
        servico.setStatusServico(StatusServico.FINALIZADO);

        when(servicoRepository.findByClienteId(2L)).thenReturn(List.of(servico));

        List<ServicoResponseDTO> result = servicoService.listarPorCliente(2L);

        assertEquals(1, result.size());
    }
}
