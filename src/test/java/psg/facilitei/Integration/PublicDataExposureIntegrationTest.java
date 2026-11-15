package psg.facilitei.Integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import psg.facilitei.Entity.Endereco;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Repository.TrabalhadorRepository;
import psg.facilitei.Services.AssinaturaPrestadorService;
import psg.facilitei.Services.MercadoPagoWebhookVerifier;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicDataExposureIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired TrabalhadorRepository trabalhadores;

    @MockBean MercadoPagoWebhookVerifier webhookVerifier;
    @MockBean AssinaturaPrestadorService assinaturaService;

    private Long trabalhador() {
        Endereco endereco = new Endereco();
        endereco.setRua("Rua das Flores");
        endereco.setNumero("123");
        endereco.setBairro("Centro");
        endereco.setCidade("Campinas");
        endereco.setEstado("SP");
        endereco.setCep("13010000");
        Trabalhador t = new Trabalhador();
        t.setNome("Carlos Pintor");
        t.setEmail("carlos-" + UUID.randomUUID() + "@example.com");
        t.setTelefone("19999990000");
        t.setSenha("$2a$12$hash");
        t.setEndereco(endereco);
        t.setHabilidades(List.of("PINTOR"));
        t.setServicoPrincipal("PINTOR");
        return trabalhadores.save(t).getId();
    }

    @Test
    void perfilPublicoNaoExpoeEmailTelefoneNemEnderecoCompleto() throws Exception {
        Long id = trabalhador();
        mockMvc.perform(get("/api/trabalhadores/buscarPorId/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carlos Pintor"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.telefone").doesNotExist())
                .andExpect(jsonPath("$.endereco.rua").doesNotExist())
                .andExpect(jsonPath("$.endereco.numero").doesNotExist())
                .andExpect(jsonPath("$.endereco.cep").doesNotExist())
                .andExpect(jsonPath("$.endereco.cidade").value("Campinas"));
    }

    @Test
    void outroUsuarioLogadoTambemNaoVeDadosPrivados() throws Exception {
        Long id = trabalhador();
        mockMvc.perform(get("/api/trabalhadores/buscarPorId/" + id)
                        .sessionAttr("auth.role", "cliente")
                        .sessionAttr("auth.userId", trabalhador())
                        .sessionAttr("auth.name", "Outro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void proprioProfissionalVeSeusDadosCompletos() throws Exception {
        Long id = trabalhador();
        mockMvc.perform(get("/api/trabalhadores/buscarPorId/" + id)
                        .sessionAttr("auth.role", "trabalhador")
                        .sessionAttr("auth.userId", id)
                        .sessionAttr("auth.name", "Carlos Pintor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefone").value("19999990000"))
                .andExpect(jsonPath("$.endereco.rua").value("Rua das Flores"));
    }

    @Test
    void uploadSemLoginEhRecusado() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "foto.png", "image/png", new byte[] {1, 2, 3});
        // CSRF válido (como o navegador faria) para provar que a recusa vem da falta de login.
        var csrf = mockMvc.perform(get("/api/auth/csrf")).andReturn().getResponse();
        String token = com.jayway.jsonpath.JsonPath.read(csrf.getContentAsString(), "$.token");
        mockMvc.perform(multipart("/api/arquivos/upload").file(file)
                        .cookie(csrf.getCookie("XSRF-TOKEN"))
                        .header("X-XSRF-TOKEN", token))
                .andExpect(status().isUnauthorized());
    }
}
