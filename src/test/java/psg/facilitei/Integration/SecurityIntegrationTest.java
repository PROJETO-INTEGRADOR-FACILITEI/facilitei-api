package psg.facilitei.Integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import psg.facilitei.Services.AbacatePayWebhookVerifier;
import psg.facilitei.Services.AssinaturaPrestadorService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {
    @Autowired MockMvc mockMvc;

    @MockBean AbacatePayWebhookVerifier webhookVerifier;
    @MockBean AssinaturaPrestadorService assinaturaService;

    @Test
    void endpointProtegidoSemSessaoRetorna401() throws Exception {
        mockMvc.perform(get("/api/servicos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clienteNaoPodeAcessarAssinaturaDeProfissional() throws Exception {
        mockMvc.perform(get("/api/assinaturas/prestador")
                        .sessionAttr("auth.role", "cliente")
                        .sessionAttr("auth.userId", 7L)
                        .sessionAttr("auth.name", "Cliente"))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioComumNaoPodeAcessarPainelAdministrativo() throws Exception {
        mockMvc.perform(get("/api/admin/support/metrics")
                        .sessionAttr("auth.role", "cliente")
                        .sessionAttr("auth.userId", 7L)
                        .sessionAttr("auth.name", "Cliente")
                        .sessionAttr("auth.admin", false))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorPodeAcessarMetricasDeSuporte() throws Exception {
        mockMvc.perform(get("/api/admin/support/metrics")
                        .sessionAttr("auth.role", "cliente")
                        .sessionAttr("auth.userId", 7L)
                        .sessionAttr("auth.name", "Admin")
                        .sessionAttr("auth.admin", true))
                .andExpect(status().isOk());
    }

    @Test
    void mutacaoPublicaSemCsrfRetorna403() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void endpointCsrfPublicaTokenECookie() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void webhookAssinadoNaoDependeDeCookieCsrf() throws Exception {
        mockMvc.perform(post("/api/assinaturas/prestador/webhook")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());
    }
}
