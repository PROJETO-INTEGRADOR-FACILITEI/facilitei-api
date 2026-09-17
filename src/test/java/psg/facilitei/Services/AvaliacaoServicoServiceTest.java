package psg.facilitei.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import psg.facilitei.DTO.AvaliacaoServicoRequestDTO;
import psg.facilitei.DTO.AvaliacaoServicoResponseDTO;
import psg.facilitei.Entity.AvaliacaoServico;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Servico;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Repository.AvaliacaoServicoRepository;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.ServicoRepository;
import psg.facilitei.Repository.TrabalhadorRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvaliacaoServicoServiceTest {

    @Mock
    private AvaliacaoServicoRepository repository;
    @Mock
    private TrabalhadorRepository trabalhadorRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ServicoRepository servicoRepository;

    @InjectMocks
    private AvaliacaoServicoService avaliacaoServicoService;

    @Test
    void create_calculaERecalculaMediaDoTrabalhador() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setId(2L);
        Servico servico = new Servico();
        servico.setId(3L);
        servico.setTrabalhador(trabalhador);
        servico.setCliente(cliente);

        AvaliacaoServicoRequestDTO dto = new AvaliacaoServicoRequestDTO();
        dto.setClienteId(1L);
        dto.setServicoId(3L);
        dto.setNota(5);
        dto.setComentario("Excelente");

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(servicoRepository.findById(3L)).thenReturn(Optional.of(servico));
        when(repository.save(any(AvaliacaoServico.class))).thenAnswer(invocation -> {
            AvaliacaoServico a = invocation.getArgument(0);
            a.setId(9L);
            return a;
        });
        when(repository.calcularMediaPorTrabalhador(2L)).thenReturn(4.5);

        AvaliacaoServicoResponseDTO result = avaliacaoServicoService.create(dto);

        assertEquals(9L, result.getId());
        assertEquals(4.5, trabalhador.getNotaTrabalhador());
        verify(trabalhadorRepository).save(trabalhador);
    }

    @Test
    void buscarAvaliacoesPorServico_retornaListaMapeada() {
        Servico servico = new Servico();
        servico.setId(3L);
        Cliente cliente = new Cliente();
        cliente.setId(1L);

        AvaliacaoServico avaliacao = new AvaliacaoServico();
        avaliacao.setId(1L);
        avaliacao.setServico(servico);
        avaliacao.setCliente(cliente);
        avaliacao.setNota(4);

        when(repository.findByServicoId(3L)).thenReturn(List.of(avaliacao));

        List<AvaliacaoServicoResponseDTO> result = avaliacaoServicoService.buscarAvaliacoesPorServico(3L);

        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getServicoId());
    }
}
