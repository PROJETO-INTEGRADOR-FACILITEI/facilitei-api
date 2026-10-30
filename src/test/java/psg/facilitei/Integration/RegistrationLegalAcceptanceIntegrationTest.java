package psg.facilitei.Integration;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import psg.facilitei.Domain.LegalDocumentType;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.LegalAcceptanceRepository;
import psg.facilitei.Repository.TrabalhadorRepository;
import psg.facilitei.Services.LegalDocumentService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationLegalAcceptanceIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired LegalDocumentService documents;
    @Autowired LegalAcceptanceRepository acceptances;
    @Autowired ClienteRepository clientes;
    @Autowired TrabalhadorRepository trabalhadores;

    @Test
    void cadastroSemAceiteNaoCriaCliente() throws Exception {
        long before = clientes.count();
        Map<String, Object> body = clientBody(uniqueEmail("sem-aceite"));

        mockMvc.perform(post("/api/clientes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        assertEquals(before, clientes.count());
    }

    @Test
    void cadastroDoClientePersisteContaEAceitesNaMesmaOperacao() throws Exception {
        String email = uniqueEmail("cliente-legal");
        Map<String, Object> body = clientBody(email);
        body.put("legalAcceptance", currentLegalAcceptance());

        mockMvc.perform(post("/api/clientes").with(csrf())
                        .header("User-Agent", "Integration Test Browser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        Long id = clientes.findByEmail(email).orElseThrow().getId();
        var saved = acceptances.findByUserIdAndUserRole(id, "cliente");
        assertEquals(2, saved.size());
        assertTrue(saved.stream().allMatch(value -> value.getIpHash().matches("[0-9a-f]{64}")));
        assertTrue(saved.stream().allMatch(value -> value.getUserAgentHash().matches("[0-9a-f]{64}")));
        assertFalse(saved.stream().anyMatch(value -> value.getIpHash().contains("127.0.0.1")));

        mockMvc.perform(get("/api/legal/status")
                        .sessionAttr("auth.role", "cliente")
                        .sessionAttr("auth.userId", id)
                        .sessionAttr("auth.name", "Cliente Legal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adultConfirmed").value(true))
                .andExpect(jsonPath("$.requiresReaccept").value(false))
                .andExpect(jsonPath("$.documents.TERMS.accepted").value(true))
                .andExpect(jsonPath("$.documents.PRIVACY.accepted").value(true));
    }

    @Test
    void cadastroDoProfissionalTambemRegistraAceite() throws Exception {
        String email = uniqueEmail("profissional-legal");
        Map<String, Object> body = workerBody(email);
        body.put("legalAcceptance", currentLegalAcceptance());

        mockMvc.perform(post("/api/trabalhadores").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        Long id = trabalhadores.findByEmail(email).orElseThrow().getId();
        var saved = acceptances.findByUserIdAndUserRole(id, "trabalhador");
        assertEquals(2, saved.size());
        assertTrue(saved.stream().map(value -> value.getDocumentType()).toList()
                .containsAll(List.of(LegalDocumentType.TERMS, LegalDocumentType.PRIVACY)));
    }

    @Test
    void statusLegalSemSessaoNaoEhPublico() throws Exception {
        mockMvc.perform(get("/api/legal/status"))
                .andExpect(status().isUnauthorized());
    }

    private Map<String, Object> clientBody(String email) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nome", "Cliente Legal");
        body.put("email", email);
        body.put("senha", "senha-segura-123");
        body.put("endereco", address());
        body.put("telefone", "31999999999");
        return body;
    }

    private Map<String, Object> workerBody(String email) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nome", "Profissional Legal");
        body.put("email", email);
        body.put("senha", "senha-segura-123");
        body.put("endereco", address());
        body.put("habilidades", List.of("ELETRICISTA"));
        body.put("servicoPrincipal", "ELETRICISTA");
        body.put("disponibilidade", "Comercial");
        return body;
    }

    private Map<String, Object> address() {
        return Map.of(
                "rua", "Rua Teste",
                "numero", "10",
                "bairro", "Centro",
                "cidade", "Belo Horizonte",
                "estado", "MG",
                "cep", "30110000");
    }

    private Map<String, Object> currentLegalAcceptance() {
        var terms = documents.document(LegalDocumentType.TERMS);
        var privacy = documents.document(LegalDocumentType.PRIVACY);
        return Map.of(
                "adultConfirmed", true,
                "documents", List.of(
                        Map.of("type", "TERMS", "version", terms.version(), "sha256", terms.sha256()),
                        Map.of("type", "PRIVACY", "version", privacy.version(), "sha256", privacy.sha256())));
    }

    private String uniqueEmail(String prefix) {
        return prefix + "+" + UUID.randomUUID() + "@example.com";
    }
}
