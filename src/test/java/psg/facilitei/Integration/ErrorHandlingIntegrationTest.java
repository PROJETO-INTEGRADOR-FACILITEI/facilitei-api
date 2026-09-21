package psg.facilitei.Integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorHandlingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void jsonMalformado_retorna400EmVezDe500() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{nome: invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void idNaoNumericoNaUrl_retorna400EmVezDe500() throws Exception {
        mockMvc.perform(get("/api/clientes/id/abc").with(user("test")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void metodoHttpNaoSuportado_retorna405EmVezDe500() throws Exception {
        mockMvc.perform(put("/api/clientes/id/1").with(user("test")).with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void contentTypeNaoSuportado_retorna415EmVezDe500() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .with(csrf())
                        .contentType("text/plain")
                        .content("qualquer coisa"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void enumInvalidoNoBody_retorna400EmVezDe500() throws Exception {
        mockMvc.perform(post("/api/servicos")
                        .with(user("test"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"titulo\":\"T\",\"descricao\":\"D\",\"tipoServico\":\"NAO_EXISTE\",\"trabalhadorId\":1,\"clienteId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
