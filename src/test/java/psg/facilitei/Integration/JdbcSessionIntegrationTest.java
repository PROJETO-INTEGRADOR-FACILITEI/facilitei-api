package psg.facilitei.Integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import psg.facilitei.Services.AssinaturaPrestadorService;
import psg.facilitei.Services.MercadoPagoWebhookVerifier;

/**
 * Sessões no banco: o login continua valendo nos pedidos seguintes só com o cookie
 * (mesmo fluxo do cadastro de profissional: cadastrar, logar, criar serviço próprio).
 */
@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JdbcSessionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @MockBean MercadoPagoWebhookVerifier webhookVerifier;
    @MockBean AssinaturaPrestadorService assinaturaService;

    private record Csrf(Cookie cookie, String token) {}

    private Csrf csrf(Cookie session) throws Exception {
        var request = get("/api/auth/csrf");
        if (session != null) request.cookie(session);
        var response = mvc.perform(request).andReturn().getResponse();
        return new Csrf(response.getCookie("XSRF-TOKEN"), JsonPath.read(response.getContentAsString(), "$.token"));
    }

    @Test
    void cadastroLoginEServicoProprioUsamASessaoGuardadaNoBanco() throws Exception {
        String config = mvc.perform(get("/api/legal/config")).andReturn().getResponse().getContentAsString();
        String terms = JsonPath.read(config, "$.documents.TERMS.sha256");
        String termsVersion = JsonPath.read(config, "$.documents.TERMS.version");
        String privacy = JsonPath.read(config, "$.documents.PRIVACY.sha256");
        String privacyVersion = JsonPath.read(config, "$.documents.PRIVACY.version");
        String email = "sessao-" + UUID.randomUUID() + "@example.com";
        String senha = "SenhaTeste#2026xy";

        Csrf anon = csrf(null);
        mvc.perform(post("/api/trabalhadores").cookie(anon.cookie()).header("X-XSRF-TOKEN", anon.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Teste Sessao","email":"%s","telefone":"31999998888","senha":"%s",
                                 "endereco":{"rua":"Rua A","numero":"1","bairro":"Centro","cidade":"BH","estado":"MG","cep":"30130010"},
                                 "habilidades":["PINTOR","montador de  Móveis"],"servicoPrincipal":"montador de  Móveis","disponibilidade":"Seg-Sex","sobre":"Monto moveis.",
                                 "legalAcceptance":{"adultConfirmed":true,"documents":[
                                   {"type":"TERMS","version":"%s","sha256":"%s"},
                                   {"type":"PRIVACY","version":"%s","sha256":"%s"}]}}
                                """.formatted(email, senha, termsVersion, terms, privacyVersion, privacy)))
                .andExpect(status().isCreated());

        var login = mvc.perform(post("/api/auth/login").cookie(anon.cookie()).header("X-XSRF-TOKEN", anon.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse();
        Cookie session = login.getCookie("SESSION");
        String userId = JsonPath.read(login.getContentAsString(), "$.user.id");

        // A profissão digitada virou item da lista, com código e nome bonitos, e é o destaque do perfil.
        mvc.perform(get("/api/profissoes"))
                .andExpect(jsonPath("$[?(@.codigo == 'MONTADOR_DE_MOVEIS')].nome").value(org.hamcrest.Matchers.contains("Montador de Móveis")))
                .andExpect(jsonPath("$[?(@.codigo == 'MONTADOR_DE_MOVEIS')].personalizada").value(org.hamcrest.Matchers.contains(true)));
        mvc.perform(get("/api/trabalhadores/buscarPorId/" + userId))
                .andExpect(jsonPath("$.servicoPrincipal").value("MONTADOR_DE_MOVEIS"))
                .andExpect(jsonPath("$.servicos").value(org.hamcrest.Matchers.contains("PINTOR", "MONTADOR_DE_MOVEIS")));
        assertNotNull(session, "login deve devolver o cookie da sessão em banco");

        Integer linhas = jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION", Integer.class);
        assertNotNull(linhas);
        assertEquals(true, linhas >= 1, "a sessão precisa estar gravada no banco");

        mvc.perform(get("/api/legal/status").cookie(session)).andExpect(status().isOk());
        // Nada do Spring Security vai para a sessão (evita conflito entre pedidos simultâneos).
        Integer contextos = jdbc.queryForObject(
                "SELECT COUNT(*) FROM SPRING_SESSION_ATTRIBUTES WHERE ATTRIBUTE_NAME = 'SPRING_SECURITY_CONTEXT'",
                Integer.class);
        assertEquals(0, contextos);

        Csrf logged = csrf(session);
        mvc.perform(post("/api/servicos-oferecidos").cookie(session, logged.cookie())
                        .header("X-XSRF-TOKEN", logged.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Montagem de móveis\",\"descricao\":\"Montagem de móveis\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Montagem de móveis"));
    }
}
