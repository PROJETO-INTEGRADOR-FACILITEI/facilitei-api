package psg.facilitei.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import psg.facilitei.Entity.AvaliacaoCliente;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.SolicitacaoServico;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Entity.Enum.StatusServico;
import psg.facilitei.Entity.Enum.StatusSolicitacao;
import psg.facilitei.Repository.AvaliacaoClienteRepository;
import psg.facilitei.Repository.ServicoRepository;
import psg.facilitei.Repository.SolicitacaoServicoRepository;

/** O profissional vê a reputação do cliente no pedido antes de aceitar. */
@ExtendWith(MockitoExtension.class)
class SolicitacaoReputacaoClienteTest {
    @Mock private SolicitacaoServicoRepository solicitacoes;
    @Mock private AvaliacaoClienteRepository avaliacoes;
    @Mock private ServicoRepository servicos;

    @InjectMocks private SolicitacaoServicoService service;

    private SolicitacaoServico pedido() {
        Cliente cliente = new Cliente();
        cliente.setId(2L);
        cliente.setNome("Marlon");
        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setId(1L);
        trabalhador.setNome("Sérgio");
        SolicitacaoServico s = new SolicitacaoServico();
        s.setId(9L);
        s.setCliente(cliente);
        s.setTrabalhador(trabalhador);
        s.setTipoServico("AULAS_INGLES");
        s.setDescricao("Preciso aprender inglês");
        s.setStatusSolicitacao(StatusSolicitacao.PENDENTE);
        s.setDataSolicitacao(LocalDateTime.now());
        return s;
    }

    private AvaliacaoCliente avaliacao(int nota, String comentario, long diasAtras) {
        AvaliacaoCliente a = new AvaliacaoCliente();
        a.setNota(nota);
        a.setComentario(comentario);
        a.setData(new Date(System.currentTimeMillis() - diasAtras * 86_400_000L));
        Trabalhador autor = new Trabalhador();
        autor.setNome("João");
        a.setTrabalhador(autor);
        return a;
    }

    @Test
    void pedidoTrazMediaQuantidadeConcluidosEComentariosMaisRecentes() {
        when(solicitacoes.findByTrabalhadorId(1L)).thenReturn(List.of(pedido()));
        when(avaliacoes.findByClienteId(2L)).thenReturn(List.of(
                avaliacao(5, "Pagou certinho", 10),
                avaliacao(3, "Atrasou", 1),
                avaliacao(4, null, 5),
                avaliacao(4, "Ok", 30)));
        when(servicos.countByClienteIdAndStatusServico(2L, StatusServico.FINALIZADO)).thenReturn(4L);

        var cliente = service.listarPorTrabalhador(1L).get(0).getCliente();

        assertEquals(4.0, cliente.getNotaCliente());
        assertEquals(4L, cliente.getQuantidadeAvaliacoes());
        assertEquals(4L, cliente.getServicosConcluidos());
        assertEquals(3, cliente.getAvaliacoesRecentes().size());
        assertEquals("Atrasou", cliente.getAvaliacoesRecentes().get(0).comentario());
    }

    @Test
    void clienteSemAvaliacoesFicaSemNota() {
        when(solicitacoes.findByTrabalhadorId(1L)).thenReturn(List.of(pedido()));
        when(avaliacoes.findByClienteId(2L)).thenReturn(List.of());

        var cliente = service.listarPorTrabalhador(1L).get(0).getCliente();

        assertNull(cliente.getNotaCliente());
        assertEquals(0L, cliente.getQuantidadeAvaliacoes());
    }
}
