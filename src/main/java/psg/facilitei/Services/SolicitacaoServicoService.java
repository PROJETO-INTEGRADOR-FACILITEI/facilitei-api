package psg.facilitei.Services;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import psg.facilitei.DTO.SolicitacaoServicoRequestDTO;
import psg.facilitei.DTO.SolicitacaoServicoResponseDTO;
import psg.facilitei.DTO.ClienteResumoDTO;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.SolicitacaoServico;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Entity.Enum.StatusSolicitacao;
import psg.facilitei.Entity.Enum.NotificationType;
import psg.facilitei.Entity.Enum.AccountStatus;
import psg.facilitei.Exceptions.ResourceNotFoundException;
import psg.facilitei.Repository.SolicitacaoServicoRepository;
import psg.facilitei.Repository.TrabalhadorRepository;
import psg.facilitei.Repository.AssinaturaPrestadorRepository;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Util.HtmlSanitizer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SolicitacaoServicoService {

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ProfissaoService profissaoService;

    @Autowired
    private SolicitacaoServicoRepository solicitacaoServicoRepository;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private TrabalhadorRepository trabalhadorRepository;

    @Autowired
    private AssinaturaPrestadorRepository assinaturaPrestadorRepository;

    @Autowired
    private PaymentEnvironmentGuard paymentEnvironmentGuard;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private psg.facilitei.Repository.ServicoOferecidoRepository servicoOferecidoRepository;

    @Autowired(required = false)
    private psg.facilitei.Repository.AvaliacaoClienteRepository avaliacaoClienteRepository;

    @Autowired(required = false)
    private psg.facilitei.Repository.ServicoRepository servicoRepository;

    @Transactional
    public SolicitacaoServicoResponseDTO criar(SolicitacaoServicoRequestDTO dto) {
        SolicitacaoServico solicitacao = new SolicitacaoServico();

        Cliente cliente = clienteService.buscarEntidadePorId(dto.getClienteId());
        solicitacao.setCliente(cliente);

        // O profissional precisa estar ativo e, com o checkout ligado, ter assinatura em dia
        Trabalhador trabalhador = trabalhadorRepository.findById(dto.getTrabalhadorId())
                .orElseThrow(() -> new ResourceNotFoundException("Trabalhador não encontrado com ID: " + dto.getTrabalhadorId()));
        if (trabalhador.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Este profissional não está disponível para novas solicitações.");
        }
        if (paymentEnvironmentGuard.isCheckoutEnabled()
                && !assinaturaPrestadorRepository.existsByTrabalhadorIdAndStatusAndActiveUntilAfter(
                        trabalhador.getId(), "ACTIVE", java.time.Instant.now())) {
            throw new BusinessRuleException("Este profissional precisa de uma assinatura mensal ativa.");
        }
        solicitacao.setTrabalhador(trabalhador);

        solicitacao.setDescricao(HtmlSanitizer.sanitize(dto.getDescricao()));
        solicitacao.setTipoServico(profissaoService == null ? dto.getTipoServico() : profissaoService.exigir(dto.getTipoServico()));
        // Se o cliente escolheu um serviço próprio do profissional, título e categoria vêm dele
        if (dto.getServicoOferecidoId() != null) {
            var oferecido = servicoOferecidoRepository
                    .findByIdAndTrabalhadorId(dto.getServicoOferecidoId(), trabalhador.getId())
                    .filter(psg.facilitei.Entity.ServicoOferecido::isAtivo)
                    .orElseThrow(() -> new BusinessRuleException(
                            "O serviço escolhido não está mais disponível com este profissional."));
            solicitacao.setServicoOferecido(oferecido);
            solicitacao.setTitulo(oferecido.getTitulo());
            if (oferecido.getTipoServico() != null) solicitacao.setTipoServico(oferecido.getTipoServico());
        }
        solicitacao.setDataSolicitacao(LocalDateTime.now());
        
        // O Servico só é criado quando o profissional aceita o pedido
        solicitacao.setServico(null);

        solicitacao.setStatusSolicitacao(StatusSolicitacao.PENDENTE);

        SolicitacaoServico salvo = solicitacaoServicoRepository.save(solicitacao);
        notificationService.notify(
                trabalhador.getId(), NotificationType.NEW_REQUEST,
                "Nova solicitação de serviço",
                cliente.getNome() + " precisa de " + (solicitacao.getTitulo() != null
                        ? "“" + solicitacao.getTitulo() + "”"
                        : formatarTipo(solicitacao.getTipoServico())) + ".",
                "/painel");

        return mapToResponse(salvo);
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoServicoResponseDTO> listarTodos() {
        return solicitacaoServicoRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoServicoResponseDTO> listarPorCliente(Long clienteId) {
        return solicitacaoServicoRepository.findByClienteId(clienteId).stream()
                .map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoServicoResponseDTO> listarPorTrabalhador(Long trabalhadorId) {
        return solicitacaoServicoRepository.findByTrabalhadorId(trabalhadorId).stream()
                .map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public SolicitacaoServicoResponseDTO buscarPorId(Long id) {
        SolicitacaoServico solicitacao = solicitacaoServicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada ID: " + id));
        return mapToResponse(solicitacao);
    }

    @Transactional
    public SolicitacaoServicoResponseDTO atualizar(Long id, SolicitacaoServicoRequestDTO dto) {
        SolicitacaoServico existente = solicitacaoServicoRepository.findById(id)
             .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada ID: " + id));
        
        if (dto.getStatusSolicitacao() != StatusSolicitacao.RECUSADA) {
            throw new BusinessRuleException("O prestador só pode recusar uma solicitação por este endpoint.");
        }
        if (existente.getStatusSolicitacao() != StatusSolicitacao.PENDENTE) {
            throw new BusinessRuleException("A solicitação já foi processada.");
        }
        existente.setStatusSolicitacao(StatusSolicitacao.RECUSADA);

        SolicitacaoServico atualizada = solicitacaoServicoRepository.save(existente);
        notificationService.notify(
                existente.getCliente().getId(), NotificationType.REQUEST_DECLINED,
                "Solicitação não aceita",
                existente.getTrabalhador().getNome() + " não pôde aceitar seu pedido de "
                        + formatarTipo(existente.getTipoServico()) + ".",
                "/painel");
        return mapToResponse(atualizada);
    }
    
    /** O cliente desiste do pedido enquanto o profissional ainda não respondeu. */
    @Transactional
    public SolicitacaoServicoResponseDTO cancelarPeloCliente(Long id, String motivoInformado) {
        SolicitacaoServico solicitacao = solicitacaoServicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada ID: " + id));
        if (solicitacao.getStatusSolicitacao() != StatusSolicitacao.PENDENTE) {
            throw new BusinessRuleException(solicitacao.getServico() != null
                    ? "O profissional já aceitou este pedido. Cancele o serviço pelo painel."
                    : "Esta solicitação já foi encerrada.");
        }
        String motivo = motivoInformado == null || motivoInformado.isBlank()
                ? null : HtmlSanitizer.sanitize(motivoInformado.trim());
        if (motivo != null && motivo.length() > 500) motivo = motivo.substring(0, 500);
        solicitacao.setStatusSolicitacao(StatusSolicitacao.CANCELADA);
        solicitacao.setMotivoCancelamento(motivo);
        SolicitacaoServico salva = solicitacaoServicoRepository.save(solicitacao);
        notificationService.notify(
                solicitacao.getTrabalhador().getId(), NotificationType.STATUS_CHANGED,
                "Pedido cancelado pelo cliente",
                solicitacao.getCliente().getNome() + " cancelou o pedido"
                        + (motivo == null ? "." : ". Motivo: " + motivo),
                "/painel");
        return mapToResponse(salva);
    }

    @Transactional
    public void deletar(Long id) {
        if (!solicitacaoServicoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Solicitação não encontrada para exclusão ID: " + id);
        }
        solicitacaoServicoRepository.deleteById(id);
    }

    private SolicitacaoServicoResponseDTO mapToResponse(SolicitacaoServico entity) {
        SolicitacaoServicoResponseDTO dto = new SolicitacaoServicoResponseDTO(
            entity.getId(),
            entity.getCliente().getId(),
            entity.getTrabalhador().getId(),
            entity.getServico() != null ? entity.getServico().getId() : null,
            entity.getTipoServico(),
            entity.getDescricao(),
            entity.getStatusSolicitacao().name(),
            resumoCliente(entity.getCliente())
        );
        dto.setTitulo(entity.getTitulo());
        dto.setServicoOferecidoId(entity.getServicoOferecido() == null ? null : entity.getServicoOferecido().getId());
        dto.setTrabalhadorNome(entity.getTrabalhador().getNome());
        dto.setTrabalhadorAvatarUrl(entity.getTrabalhador().getUrlFoto());
        dto.setDataSolicitacao(entity.getDataSolicitacao());
        dto.setMotivoCancelamento(entity.getMotivoCancelamento());
        return dto;
    }

    /**
     * Reputação do cliente mostrada no pedido: média, total de avaliações, as 3 mais
     * recentes e serviços concluídos. Os repositórios são opcionais por causa dos testes unitários.
     */
    private ClienteResumoDTO resumoCliente(Cliente cliente) {
        ClienteResumoDTO resumo = new ClienteResumoDTO(cliente.getId(), cliente.getNome(), cliente.getUrlFoto());
        if (avaliacaoClienteRepository != null) {
            var avaliacoes = avaliacaoClienteRepository.findByClienteId(cliente.getId());
            resumo.setQuantidadeAvaliacoes((long) avaliacoes.size());
            resumo.setNotaCliente(avaliacoes.isEmpty() ? null : avaliacoes.stream()
                    .mapToInt(psg.facilitei.Entity.AvaliacaoCliente::getNota).average().orElse(0));
            resumo.setAvaliacoesRecentes(avaliacoes.stream()
                    .sorted(java.util.Comparator.comparing(psg.facilitei.Entity.AvaliacaoCliente::getData,
                            java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                    .limit(3)
                    .map(a -> new ClienteResumoDTO.AvaliacaoRecente(a.getNota(), a.getComentario(),
                            a.getTrabalhador() == null ? null : a.getTrabalhador().getNome(), a.getData()))
                    .toList());
        }
        if (servicoRepository != null) {
            resumo.setServicosConcluidos(servicoRepository.countByClienteIdAndStatusServico(
                    cliente.getId(), psg.facilitei.Entity.Enum.StatusServico.FINALIZADO));
        }
        return resumo;
    }

    private String formatarTipo(String value) {
        String nome = profissaoService == null ? ProfissaoService.humanizar(value) : profissaoService.nome(value);
        return nome.toLowerCase();
    }
}
