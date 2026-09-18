package psg.facilitei.Services;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import psg.facilitei.DTO.TrabalhadorPageResponseDTO;
import psg.facilitei.Entity.Endereco;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Entity.Enum.TipoServico;
import psg.facilitei.Repository.TrabalhadorRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DataJpaTest
@ActiveProfiles("test")
@Import(TrabalhadorService.class)
class TrabalhadorServiceListagemTest {

    @Autowired
    private TrabalhadorRepository repository;

    @Autowired
    private TrabalhadorService service;

    @Test
    void listarPaginado_aplicaFiltrosSemDuplicarTrabalhadores() {
        repository.save(trabalhador("Ana Lima", "ana@example.com", "Recife", 4.5,
                List.of(TipoServico.DIARISTA, TipoServico.FAXINEIRA)));
        repository.save(trabalhador("Ana Souza", "ana2@example.com", "Recife", 3.0,
                List.of(TipoServico.DIARISTA)));
        repository.save(trabalhador("Bruna", "bruna@example.com", "Olinda", 5.0,
                List.of(TipoServico.PEDREIRO)));
        repository.flush();

        TrabalhadorPageResponseDTO resultado = service.findAllPaginado(0, 12, "ana", "recife",
                List.of(TipoServico.DIARISTA, TipoServico.FAXINEIRA), 4.0);

        assertEquals(1, resultado.totalElements());
        assertEquals("Ana Lima", resultado.content().get(0).getNome());
        assertEquals(1, resultado.totalPages());
    }

    @Test
    void listarPaginado_devolveMetadadosDaProximaPagina() {
        repository.save(trabalhador("Ana", "ana@example.com", "Recife", 4.5,
                List.of(TipoServico.DIARISTA)));
        repository.save(trabalhador("Bruna", "bruna@example.com", "Olinda", 5.0,
                List.of(TipoServico.PEDREIRO)));
        repository.flush();

        assertEquals(2, service.findAll().size());

        TrabalhadorPageResponseDTO resultado = service.findAllPaginado(0, 1, null, null,
                List.of(), 0.0);

        assertEquals(2, resultado.totalElements());
        assertEquals(2, resultado.totalPages());
        assertEquals(1, resultado.content().size());
        assertFalse(resultado.last());
    }

    private Trabalhador trabalhador(String nome, String email, String cidade, double nota,
            List<TipoServico> habilidades) {
        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setNome(nome);
        trabalhador.setEmail(email);
        trabalhador.setSenha("senha");
        trabalhador.setNotaTrabalhador(nota);
        trabalhador.setHabilidades(habilidades);

        Endereco endereco = new Endereco();
        endereco.setRua("Rua A");
        endereco.setNumero("1");
        endereco.setBairro("Centro");
        endereco.setCidade(cidade);
        endereco.setEstado("PE");
        endereco.setCep("50000000");
        trabalhador.setEndereco(endereco);
        return trabalhador;
    }
}
