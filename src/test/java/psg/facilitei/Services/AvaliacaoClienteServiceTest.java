package psg.facilitei.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import psg.facilitei.DTO.AvaliacaoClienteRequestDTO;
import psg.facilitei.DTO.AvaliacaoClienteResponseDTO;
import psg.facilitei.Entity.AvaliacaoCliente;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Exceptions.ResourceNotFoundException;
import psg.facilitei.Repository.AvaliacaoClienteRepository;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.TrabalhadorRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvaliacaoClienteServiceTest {

    @Mock
    private AvaliacaoClienteRepository avaliacaoClienteRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private TrabalhadorRepository trabalhadorRepository;

    @InjectMocks
    private AvaliacaoClienteService avaliacaoClienteService;

    @Test
    void criarAvaliacao_primeiraAvaliacao_mediaIgualNota() {
        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setId(1L);
        Cliente cliente = new Cliente();
        cliente.setId(2L);

        AvaliacaoClienteRequestDTO dto = new AvaliacaoClienteRequestDTO();
        dto.setTrabalhadorId(1L);
        dto.setClienteId(2L);
        dto.setNota(5);
        dto.setComentario("Cliente pontual");

        when(trabalhadorRepository.findById(1L)).thenReturn(Optional.of(trabalhador));
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(cliente));
        when(avaliacaoClienteRepository.findByClienteId(2L)).thenReturn(Collections.emptyList());
        when(avaliacaoClienteRepository.save(any(AvaliacaoCliente.class))).thenAnswer(invocation -> {
            AvaliacaoCliente a = invocation.getArgument(0);
            a.setId(11L);
            return a;
        });

        AvaliacaoClienteResponseDTO result = avaliacaoClienteService.criarAvaliacao(dto);

        assertEquals(5.0, result.getMediaCliente());
        assertEquals(5.0, cliente.getNotaCliente());
        verify(clienteRepository).save(cliente);
    }

    @Test
    void criarAvaliacao_comTrabalhadorInexistente_lancaResourceNotFoundException() {
        AvaliacaoClienteRequestDTO dto = new AvaliacaoClienteRequestDTO();
        dto.setTrabalhadorId(1L);
        dto.setClienteId(2L);

        when(trabalhadorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> avaliacaoClienteService.criarAvaliacao(dto));
    }

    @Test
    void deletarAvaliacao_inexistente_lancaResourceNotFoundException() {
        when(avaliacaoClienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> avaliacaoClienteService.deletarAvaliacao(99L));
    }

    @Test
    void listarPorCliente_retornaListaMapeada() {
        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setId(1L);
        Cliente cliente = new Cliente();
        cliente.setId(2L);

        AvaliacaoCliente avaliacao = new AvaliacaoCliente();
        avaliacao.setId(1L);
        avaliacao.setTrabalhador(trabalhador);
        avaliacao.setCliente(cliente);
        avaliacao.setNota(4);

        when(avaliacaoClienteRepository.findByClienteId(2L)).thenReturn(List.of(avaliacao));

        List<AvaliacaoClienteResponseDTO> result = avaliacaoClienteService.listarPorCliente(2L);

        assertEquals(1, result.size());
    }
}
