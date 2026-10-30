package psg.facilitei.Integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "legal.operator-type=INDIVIDUAL",
        "legal.display-name=Arthur Faria Estrela",
        "legal.contact-email=contato@facilitei.online",
        "legal.payments-mode=TEST",
        "legal.documents.terms.version=1.0.0",
        "legal.documents.terms.effective-date=2026-10-30",
        "legal.documents.terms.requires-reaccept=true",
        "legal.documents.privacy.version=1.0.0",
        "legal.documents.privacy.effective-date=2026-10-30",
        "legal.documents.privacy.requires-reaccept=true",
        "legal.documents.subscription.version=1.0.0",
        "legal.documents.subscription.effective-date=2026-10-30",
        "legal.documents.subscription.requires-reaccept=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LegalDocumentIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void configuracaoLegalEhPublicaEExpoeSomenteMetadadosPublicos() throws Exception {
        mockMvc.perform(get("/api/legal/config"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=300")))
                .andExpect(jsonPath("$.operatorType").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.displayName").value("Arthur Faria Estrela"))
                .andExpect(jsonPath("$.contactEmail").value("contato@facilitei.online"))
                .andExpect(jsonPath("$.taxId").doesNotExist())
                .andExpect(jsonPath("$.businessAddress").doesNotExist())
                .andExpect(jsonPath("$.profileComplete").value(false))
                .andExpect(jsonPath("$.paymentsMode").value("TEST"))
                .andExpect(jsonPath("$.adultOnly").value(true))
                .andExpect(jsonPath("$.documents.TERMS.version").value("1.0.0"))
                .andExpect(jsonPath("$.documents.TERMS.effectiveDate").value("2026-10-30"))
                .andExpect(jsonPath("$.documents.TERMS.requiresReaccept").value(true))
                .andExpect(jsonPath("$.documents.TERMS.sha256")
                        .value(matchesPattern("^[a-f0-9]{64}$")));
    }

    @Test
    void documentoLegalEhPublicoVersionadoECarregaMarkdownImutavel() throws Exception {
        mockMvc.perform(get("/api/legal/documents/terms"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=300")))
                .andExpect(jsonPath("$.type").value("TERMS"))
                .andExpect(jsonPath("$.title").value("Termos de Uso"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.effectiveDate").value("2026-10-30"))
                .andExpect(jsonPath("$.requiresReaccept").value(true))
                .andExpect(jsonPath("$.sha256").value(matchesPattern("^[a-f0-9]{64}$")))
                .andExpect(jsonPath("$.markdown", containsString("# Termos de Uso")))
                .andExpect(jsonPath("$.markdown", containsString("contato@facilitei.online")));
    }

    @Test
    void tipoDeDocumentoDesconhecidoRetorna404() throws Exception {
        mockMvc.perform(get("/api/legal/documents/desconhecido"))
                .andExpect(status().isNotFound());
    }
}
