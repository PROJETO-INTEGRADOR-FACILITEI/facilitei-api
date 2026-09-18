package psg.facilitei.Controller;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import psg.facilitei.DTO.TrabalhadorPageResponseDTO;
import psg.facilitei.DTO.TrabalhadorResponseDTO;
import psg.facilitei.Entity.Enum.TipoServico;
import psg.facilitei.Exceptions.GlobalExceptionHandler;
import psg.facilitei.Services.TrabalhadorService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TrabalhadorControllerTest {

    private TrabalhadorService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(TrabalhadorService.class);
        TrabalhadorController controller = new TrabalhadorController();
        ReflectionTestUtils.setField(controller, "service", service);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listarPaginado_retornaContratoEsperadoPeloFrontend() throws Exception {
        TrabalhadorResponseDTO trabalhador = new TrabalhadorResponseDTO();
        trabalhador.setId("7");
        trabalhador.setNome("Ana");
        TrabalhadorPageResponseDTO resposta = new TrabalhadorPageResponseDTO(
                List.of(trabalhador), 13, 2, 0, 12, true, false);
        when(service.findAllPaginado(0, 12, "Ana", "Recife",
                List.of(TipoServico.DIARISTA), 4.0)).thenReturn(resposta);

        mvc.perform(get("/api/trabalhadores/listar/paginado")
                        .param("page", "0")
                        .param("size", "12")
                        .param("nome", "Ana")
                        .param("localizacao", "Recife")
                        .param("tiposServico", "DIARISTA")
                        .param("notaMinima", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("7"))
                .andExpect(jsonPath("$.totalElements").value(13))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void listarTodos_mantemListaSimples() throws Exception {
        when(service.findAll()).thenReturn(List.of());

        mvc.perform(get("/api/trabalhadores/listar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(service).findAll();
    }

    @Test
    void listarPaginado_rejeitaPaginaInvalida() throws Exception {
        mvc.perform(get("/api/trabalhadores/listar/paginado").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }
}
