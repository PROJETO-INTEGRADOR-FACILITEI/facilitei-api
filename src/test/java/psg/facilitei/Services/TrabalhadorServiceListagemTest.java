package psg.facilitei.Services;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import psg.facilitei.DTO.TrabalhadorPageResponseDTO;
import psg.facilitei.Entity.Endereco;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Servico;
import psg.facilitei.Entity.AvaliacaoServico;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Entity.Enum.StatusServico;
import psg.facilitei.Entity.Enum.TipoServico;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.ServicoRepository;
import psg.facilitei.Repository.AvaliacaoServicoRepository;
import psg.facilitei.Repository.TrabalhadorRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DataJpaTest
@ActiveProfiles("test")
@Import(TrabalhadorService.class)
class TrabalhadorServiceListagemTest {

    @MockBean
    private PasswordHashService passwordHashService;

    @Autowired
    private TrabalhadorRepository repository;

    @Autowired
    private TrabalhadorService service;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private AvaliacaoServicoRepository avaliacaoServicoRepository;

    @Test
    void listarPaginado_incluiMediaEQuantidadePorEspecialidade() {
        Trabalhador ana = repository.save(trabalhador("Ana", "ana@example.com", "Recife", 4.0,
                List.of(TipoServico.DIARISTA, TipoServico.FAXINEIRA)));
        Cliente cliente = new Cliente();
        cliente.setNome("Cliente");
        cliente.setEmail("cliente@example.com");
        cliente.setSenha("senha");
        cliente = clienteRepository.save(cliente);

        avaliar(ana, cliente, TipoServico.DIARISTA, 5);
        avaliar(ana, cliente, TipoServico.DIARISTA, 3);
        avaliar(ana, cliente, TipoServico.FAXINEIRA, 2);

        var resultado = service.findAllPaginado(0, 12, null, null, List.of(), 0.0);
        var resumos = resultado.content().get(0).getAvaliacoesPorServico();

        assertEquals(2, resumos.size());
        assertEquals(4.0, resumos.stream().filter(r -> r.tipoServico() == TipoServico.DIARISTA)
                .findFirst().orElseThrow().media());
        assertEquals(2L, resumos.stream().filter(r -> r.tipoServico() == TipoServico.DIARISTA)
                .findFirst().orElseThrow().quantidadeAvaliacoes());
        assertEquals(2.0, resumos.stream().filter(r -> r.tipoServico() == TipoServico.FAXINEIRA)
                .findFirst().orElseThrow().media());
    }

    private void avaliar(Trabalhador trabalhador, Cliente cliente, TipoServico tipo, int nota) {
        Servico servico = new Servico();
        servico.setTitulo("Serviço");
        servico.setDescricao("Descrição do serviço");
        servico.setTrabalhador(trabalhador);
        servico.setCliente(cliente);
        servico.setTipoServico(tipo);
        servico.setStatusServico(StatusServico.FINALIZADO);
        servicoRepository.save(servico);

        AvaliacaoServico avaliacao = new AvaliacaoServico();
        avaliacao.setCliente(cliente);
        avaliacao.setServico(servico);
        avaliacao.setNota(nota);
        avaliacaoServicoRepository.save(avaliacao);
    }

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
