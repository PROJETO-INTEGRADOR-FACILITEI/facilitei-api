package psg.facilitei.Services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import psg.facilitei.Entity.AssinaturaPrestador;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Repository.AssinaturaPrestadorRepository;
import psg.facilitei.Repository.TrabalhadorRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "mercadopago.monthly-amount-cents=2990",
        "mercadopago.access-token=test-token"
})
@ActiveProfiles("test")
@Import(TrabalhadorService.class)
class TrabalhadorAssinaturaFiltroTest {
    @MockBean PasswordHashService passwordHashService;
    @Autowired TrabalhadorService service;
    @Autowired TrabalhadorRepository trabalhadores;
    @Autowired AssinaturaPrestadorRepository assinaturas;

    @Test
    void listagensMostramSomentePrestadoresComMensalidadeVigente() {
        Trabalhador ativo = criar("Ativo", "ativo@example.com");
        criar("Sem assinatura", "sem@example.com");
        Trabalhador vencido = criar("Vencido", "vencido@example.com");
        assinatura(ativo, Instant.now().plus(30, ChronoUnit.DAYS));
        assinatura(vencido, Instant.now().minus(1, ChronoUnit.DAYS));

        assertEquals(List.of("Ativo"), service.findAll().stream().map(r -> r.getNome()).toList());
        assertEquals(List.of("Ativo"), service.findAllPaginado(0, 12, null, null, List.of(), 0.0)
                .content().stream().map(r -> r.getNome()).toList());
    }

    private Trabalhador criar(String nome, String email) {
        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setNome(nome);
        trabalhador.setEmail(email);
        trabalhador.setSenha("senha");
        return trabalhadores.save(trabalhador);
    }

    private void assinatura(Trabalhador trabalhador, Instant validade) {
        AssinaturaPrestador assinatura = new AssinaturaPrestador();
        assinatura.setTrabalhador(trabalhador);
        assinatura.setStatus("ACTIVE");
        assinatura.setActiveUntil(validade);
        assinaturas.save(assinatura);
    }
}
