package psg.facilitei.Integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import psg.facilitei.Services.AssinaturaPrestadorService;
import psg.facilitei.Services.MercadoPagoWebhookVerifier;

/** As respostas de erro dizem em português qual campo está errado e por quê. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorMessagesIntegrationTest {
    @Autowired MockMvc mockMvc;

    @MockBean MercadoPagoWebhookVerifier webhookVerifier;
    @MockBean AssinaturaPrestadorService assinaturaService;

    private ResultActions postJson(String url, String body) throws Exception {
        var csrf = mockMvc.perform(get("/api/auth/csrf")).andReturn().getResponse();
        String token = com.jayway.jsonpath.JsonPath.read(csrf.getContentAsString(), "$.token");
        Cookie cookie = csrf.getCookie("XSRF-TOKEN");
        return mockMvc.perform(post(url)
                .cookie(cookie)
                .header("X-XSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void cadastroInvalidoListaTodosOsCamposComNomeAmigavel() throws Exception {
        postJson("/api/trabalhadores", "{\"nome\":\"\",\"email\":\"nao-e-email\",\"senha\":\"curta\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Corrija os campos")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("nome")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("senha")))
                .andExpect(jsonPath("$.errors[*].label", hasItem("E-mail")))
                .andExpect(jsonPath("$.errors[*].message", hasItem("A senha deve ter entre 12 e 72 caracteres.")));
    }

    @Test
    void opcaoVaziaEmCampoDeListaDizQualCampo() throws Exception {
        postJson("/api/trabalhadores", "{\"nome\":\"Ana\",\"servicoPrincipal\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Selecione uma opção em Serviço principal."))
                .andExpect(jsonPath("$.errors[0].field").value("servicoPrincipal"));
    }

    @Test
    void numeroInvalidoDizQueDeveSerNumero() throws Exception {
        postJson("/api/trabalhadores", "{\"endereco\":{\"cep\":\"1\"},\"notaTrabalhador\":\"abc\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nota trabalhador deve ser um número."));
    }

    @Test
    void jsonQuebradoTemMensagemEmPortugues() throws Exception {
        postJson("/api/trabalhadores", "{\"nome\": ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("formato inválido")));
    }
}
