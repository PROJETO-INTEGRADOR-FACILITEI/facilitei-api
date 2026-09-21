package psg.facilitei.Security;

import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import psg.facilitei.Entity.Enum.StatusServico;
import psg.facilitei.Repository.AvaliacaoClienteRepository;
import psg.facilitei.Repository.AvaliacaoServicoRepository;
import psg.facilitei.Repository.PortfolioRepository;
import psg.facilitei.Repository.ServicoRepository;
import psg.facilitei.Repository.SolicitacaoServicoRepository;

@Service
public class AccessControlService {
    private final ServicoRepository servicos;
    private final SolicitacaoServicoRepository solicitacoes;
    private final PortfolioRepository portfolios;
    private final AvaliacaoServicoRepository avaliacoesServico;
    private final AvaliacaoClienteRepository avaliacoesCliente;

    public AccessControlService(ServicoRepository servicos,
                                SolicitacaoServicoRepository solicitacoes,
                                PortfolioRepository portfolios,
                                AvaliacaoServicoRepository avaliacoesServico,
                                AvaliacaoClienteRepository avaliacoesCliente) {
        this.servicos = servicos;
        this.solicitacoes = solicitacoes;
        this.portfolios = portfolios;
        this.avaliacoesServico = avaliacoesServico;
        this.avaliacoesCliente = avaliacoesCliente;
    }

    public AuthenticatedPrincipal current() {
        return fromAuthentication(SecurityContextHolder.getContext().getAuthentication());
    }

    public AuthenticatedPrincipal fromPrincipal(Principal principal) {
        if (principal instanceof Authentication authentication) return fromAuthentication(authentication);
        throw unauthorized();
    }

    public void requireCliente(Long clienteId) {
        AuthenticatedPrincipal actor = current();
        if (!actor.isCliente() || !actor.id().equals(clienteId)) throw forbidden();
    }

    public void requireTrabalhador(Long trabalhadorId) {
        AuthenticatedPrincipal actor = current();
        if (!actor.isTrabalhador() || !actor.id().equals(trabalhadorId)) throw forbidden();
    }

    public void requireServiceParticipant(Long servicoId) {
        requireServiceParticipant(servicoId, current());
    }

    public void requireServiceParticipant(Long servicoId, Principal principal) {
        requireServiceParticipant(servicoId, fromPrincipal(principal));
    }

    public void requireServiceClient(Long servicoId) {
        AuthenticatedPrincipal actor = current();
        if (!actor.isCliente() || !servicos.existsByIdAndClienteId(servicoId, actor.id())) throw forbidden();
    }

    public void requireServiceWorker(Long servicoId) {
        AuthenticatedPrincipal actor = current();
        if (!actor.isTrabalhador() || !servicos.existsByIdAndTrabalhadorId(servicoId, actor.id())) throw forbidden();
    }

    public void requireServiceParties(Long servicoId, Long clienteId, Long trabalhadorId) {
        if (!servicos.existsByIdAndClienteIdAndTrabalhadorId(servicoId, clienteId, trabalhadorId)) {
            throw forbidden();
        }
    }

    public void requireServiceStatusChange(Long servicoId, StatusServico requestedStatus) {
        AuthenticatedPrincipal actor = current();
        var service = servicos.findById(servicoId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Serviço não encontrado."));
        boolean allowed = actor.isTrabalhador()
                && service.getTrabalhador().getId().equals(actor.id())
                && service.getStatusServico() == StatusServico.EM_ANDAMENTO
                && requestedStatus == StatusServico.PENDENTE_APROVACAO;
        allowed = allowed || actor.isCliente()
                && service.getCliente().getId().equals(actor.id())
                && service.getStatusServico() == StatusServico.PENDENTE_APROVACAO
                && (requestedStatus == StatusServico.FINALIZADO
                    || requestedStatus == StatusServico.EM_ANDAMENTO);
        if (!allowed) throw forbidden();
    }

    public void requireSolicitationParticipant(Long solicitacaoId) {
        AuthenticatedPrincipal actor = current();
        boolean allowed = actor.isCliente()
                ? solicitacoes.existsByIdAndClienteId(solicitacaoId, actor.id())
                : actor.isTrabalhador() && solicitacoes.existsByIdAndTrabalhadorId(solicitacaoId, actor.id());
        if (!allowed) throw forbidden();
    }

    public void requireSolicitationWorker(Long solicitacaoId) {
        AuthenticatedPrincipal actor = current();
        if (!actor.isTrabalhador()
                || !solicitacoes.existsByIdAndTrabalhadorId(solicitacaoId, actor.id())) throw forbidden();
    }

    public void requireSolicitationClient(Long solicitacaoId) {
        AuthenticatedPrincipal actor = current();
        if (!actor.isCliente()
                || !solicitacoes.existsByIdAndClienteId(solicitacaoId, actor.id())) throw forbidden();
    }

    public void requirePortfolioOwner(Long portfolioId) {
        AuthenticatedPrincipal actor = current();
        if (!actor.isTrabalhador()
                || !portfolios.existsByIdAndTrabalhadorId(portfolioId, actor.id())) throw forbidden();
    }

    public void requireServiceReviewAuthor(Long avaliacaoId) {
        AuthenticatedPrincipal actor = current();
        var review = avaliacoesServico.findById(avaliacaoId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada."));
        if (!actor.isCliente() || !review.getCliente().getId().equals(actor.id())) throw forbidden();
    }

    public void requireClientReviewAuthor(Long avaliacaoId) {
        AuthenticatedPrincipal actor = current();
        var review = avaliacoesCliente.findById(avaliacaoId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada."));
        if (!actor.isTrabalhador() || !review.getTrabalhador().getId().equals(actor.id())) throw forbidden();
    }

    private void requireServiceParticipant(Long servicoId, AuthenticatedPrincipal actor) {
        boolean allowed = actor.isCliente()
                ? servicos.existsByIdAndClienteId(servicoId, actor.id())
                : actor.isTrabalhador() && servicos.existsByIdAndTrabalhadorId(servicoId, actor.id());
        if (!allowed) throw forbidden();
    }

    private AuthenticatedPrincipal fromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthenticatedPrincipal principal) {
            return principal;
        }
        throw unauthorized();
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Você não tem permissão para acessar ou alterar este recurso.");
    }
}
