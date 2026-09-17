package psg.facilitei.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import psg.facilitei.DTO.AvaliacaoTrabalhadorRequestDTO;
import psg.facilitei.DTO.AvaliacaoTrabalhadorResponseDTO;
import psg.facilitei.Entity.AvaliacaoTrabalhador;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Exceptions.ResourceNotFoundException;
import psg.facilitei.Repository.AvaliacaoTrabalhadorRepository;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.TrabalhadorRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvaliacaoTrabalhadorServiceTest {

    @Mock
    private AvaliacaoTrabalhadorRepository repository;
    @Mock
    private TrabalhadorRepository trabalhadorRepository;
    @Mock
    private ClienteRepository clienteRepository;

    private AvaliacaoTrabalhadorService newService() {
        AvaliacaoTrabalhadorService service = new AvaliacaoTrabalhadorService();
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "trabalhadorRepository", trabalhadorRepository);
        ReflectionTestUtils.setField(service, "clienteRepository", clienteRepository);
        ReflectionTestUtils.setField(service, "modelMapper", new ModelMapper());
        return service;
    }

    @Test
    void criar_comTrabalhadorEClienteExistentes_salvaEDevolveDTO() {
        AvaliacaoTrabalhadorService service = newService();

        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setId(1L);
        Cliente cliente = new Cliente();
        cliente.setId(2L);

        AvaliacaoTrabalhadorRequestDTO dto = new AvaliacaoTrabalhadorRequestDTO();
        dto.setTrabalhadorId(1L);
        dto.setClienteId(2L);
        dto.setNota(5);
        dto.setComentario("Ótimo serviço");

        when(trabalhadorRepository.findById(1L)).thenReturn(Optional.of(trabalhador));
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(cliente));
        when(repository.save(any(AvaliacaoTrabalhador.class))).thenAnswer(invocation -> {
            AvaliacaoTrabalhador a = invocation.getArgument(0);
            a.setId(99L);
            return a;
        });

        AvaliacaoTrabalhadorResponseDTO result = service.criar(dto);

        assertEquals(99L, result.getId());
        assertEquals(1L, result.getTrabalhadorId());
        assertEquals(2L, result.getClienteId());
        assertEquals(5, result.getNota());
        assertEquals("Ótimo serviço", result.getComentario());
    }

    @Test
    void criar_comTrabalhadorInexistente_lancaResourceNotFoundException() {
        AvaliacaoTrabalhadorService service = newService();

        AvaliacaoTrabalhadorRequestDTO dto = new AvaliacaoTrabalhadorRequestDTO();
        dto.setTrabalhadorId(1L);
        dto.setClienteId(2L);

        when(trabalhadorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.criar(dto));
    }

    @Test
    void criar_quandoClienteJaAvaliouEsteTrabalhador_lancaBusinessRuleException() {
        AvaliacaoTrabalhadorService service = newService();

        AvaliacaoTrabalhadorRequestDTO dto = new AvaliacaoTrabalhadorRequestDTO();
        dto.setTrabalhadorId(1L);
        dto.setClienteId(2L);
        dto.setNota(5);

        when(repository.existsByClienteIdAndTrabalhadorId(2L, 1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> service.criar(dto));

        verify(repository, never()).save(any(AvaliacaoTrabalhador.class));
    }

    @Test
    void listarPorTrabalhador_retornaListaMapeada() {
        AvaliacaoTrabalhadorService service = newService();

        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setId(1L);
        Cliente cliente = new Cliente();
        cliente.setId(2L);

        AvaliacaoTrabalhador avaliacao = new AvaliacaoTrabalhador();
        avaliacao.setId(10L);
        avaliacao.setTrabalhador(trabalhador);
        avaliacao.setCliente(cliente);
        avaliacao.setNota(4);

        when(repository.findByTrabalhadorId(1L)).thenReturn(List.of(avaliacao));

        List<AvaliacaoTrabalhadorResponseDTO> result = service.listarPorTrabalhador(1L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals(1L, result.get(0).getTrabalhadorId());
        assertEquals(2L, result.get(0).getClienteId());
    }
}
