package psg.facilitei.Services;

import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import psg.facilitei.DTO.ServicoRequestDTO;
import psg.facilitei.DTO.ServicoResponseDTO;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Servico;
import psg.facilitei.Entity.SolicitacaoServico;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Entity.Enum.StatusSolicitacao;
import psg.facilitei.Entity.Enum.StatusServico;
import psg.facilitei.Entity.Enum.NotificationType;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Exceptions.ResourceNotFoundException;
import psg.facilitei.Repository.AvaliacaoServicoRepository;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.ServicoRepository;
import psg.facilitei.Repository.SolicitacaoServicoRepository;
import psg.facilitei.Repository.AssinaturaPrestadorRepository;
import psg.facilitei.Util.HtmlSanitizer;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ServicoService {

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ProfissaoService profissaoService;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private TrabalhadorService trabalhadorService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AvaliacaoServicoRepository avaliacaoServicoRepository;

    @Autowired
    private SolicitacaoServicoRepository solicitacaoServicoRepository;

    @Autowired
    private AssinaturaPrestadorRepository assinaturaPrestadorRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PaymentEnvironmentGuard paymentEnvironmentGuard;

    @Autowired
    private psg.facilitei.Repository.MensagemRepository mensagemRepository;

    @Autowired(required = false)
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    private static final java.time.format.DateTimeFormatter DATA_RESUMO =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    /** Status em que o cliente ainda pode cancelar o serviço. */
    private static final java.util.Set<StatusServico> CANCELAVEIS_PELO_CLIENTE = java.util.EnumSet.of(
            StatusServico.SOLICITADO, StatusServico.PENDENTE, StatusServico.EM_ANDAMENTO,
            StatusServico.AGUARDANDO_CONTATO, StatusServico.EM_ANALISE, StatusServico.APROVADO,
            StatusServico.PAUSADO);

    public List<ServicoResponseDTO> listarTodos() {
        return servicoRepository.findAll()
                .stream()
                .map(servico -> modelMapper.map(servico, ServicoResponseDTO.class))
                .collect(Collectors.toList());
    }

    public ServicoResponseDTO buscarPorId(Long id) {
        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com ID: " + id));
        return modelMapper.map(servico, ServicoResponseDTO.class);
    }

    public Servico buscarEntidadePorId(Long id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com ID: " + id));
    }

    @Transactional
    public ServicoResponseDTO criar(ServicoRequestDTO dto) {
        Servico servico = modelMapper.map(dto, Servico.class);
        servico.setTitulo(HtmlSanitizer.sanitize(servico.getTitulo()));
        servico.setDescricao(HtmlSanitizer.sanitize(servico.getDescricao()));

        Trabalhador trabalhador = trabalhadorService.buscarEntidadePorId(dto.getTrabalhadorId());
        if (paymentEnvironmentGuard.isCheckoutEnabled()
                && !assinaturaPrestadorRepository.existsByTrabalhadorIdAndStatusAndActiveUntilAfter(
                        trabalhador.getId(), "ACTIVE", java.time.Instant.now())) {
            throw new BusinessRuleException("Assinatura mensal do profissional inativa.");
        }
        servico.setTrabalhador(trabalhador);

        Cliente cliente = clienteRepository.findById(dto.getClienteId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Cliente não encontrado com ID: " + dto.getClienteId()));
        servico.setCliente(cliente);

        SolicitacaoServico solicitacao = null;
        if (dto.getSolicitacaoId() != null) {
            solicitacao = solicitacaoServicoRepository.findById(dto.getSolicitacaoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Solicitação não encontrada com ID: " + dto.getSolicitacaoId()));

            validarSolicitacaoPendente(solicitacao, dto);
            servico.setSolicitacao(solicitacao);
            if (solicitacao.getTitulo() != null && !solicitacao.getTitulo().isBlank()) {
                servico.setTitulo(solicitacao.getTitulo());
            }
        }

        if (servico.getStatusServico() == null) {
            servico.setStatusServico(StatusServico.PENDENTE);
        }

        Servico salvo = servicoRepository.save(servico);

        if (solicitacao != null) {
            solicitacao.setServico(salvo);
            solicitacao.setStatusSolicitacao(StatusSolicitacao.ACEITA);
            solicitacaoServicoRepository.save(solicitacao);
        }
        publicarResumoNoChat(salvo, solicitacao);

        notificationService.notify(
                cliente.getId(), NotificationType.REQUEST_ACCEPTED,
                "Seu pedido foi aceito",
                trabalhador.getNome() + " aceitou seu pedido. Agora vocês já podem conversar.",
                "/painel/chat/" + salvo.getId());

        return modelMapper.map(salvo, ServicoResponseDTO.class);
    }

    /**
     * Primeira mensagem do chat: o resumo do pedido, para o profissional entender
     * de cara o que o cliente precisa.
     */
    private void publicarResumoNoChat(Servico servico, SolicitacaoServico solicitacao) {
        Cliente cliente = servico.getCliente();
        StringBuilder resumo = new StringBuilder("Resumo do pedido\n");
        resumo.append("Pedido: ").append(servico.getTitulo()).append('\n');
        if (solicitacao != null && solicitacao.getServicoOferecido() != null
                && solicitacao.getServicoOferecido().getPreco() != null) {
            resumo.append("Preço anunciado: R$ ")
                    .append(String.format(java.util.Locale.forLanguageTag("pt-BR"), "%,.2f",
                            solicitacao.getServicoOferecido().getPreco()))
                    .append('\n');
        }
        resumo.append("Categoria: ").append(formatarTipo(servico.getTipoServico())).append('\n');
        resumo.append("Cliente: ").append(cliente.getNome()).append('\n');
        if (cliente.getEndereco() != null) {
            var e = cliente.getEndereco();
            String local = java.util.stream.Stream.of(e.getBairro(), e.getCidade(), e.getEstado())
                    .filter(v -> v != null && !v.isBlank())
                    .collect(Collectors.joining(", "));
            if (!local.isBlank()) resumo.append("Local: ").append(local).append('\n');
        }
        if (solicitacao != null && solicitacao.getDataSolicitacao() != null) {
            resumo.append("Pedido feito em: ").append(DATA_RESUMO.format(solicitacao.getDataSolicitacao())).append('\n');
        }
        String descricao = solicitacao != null ? solicitacao.getDescricao() : servico.getDescricao();
        resumo.append("\nO que o cliente precisa:\n").append(descricao);

        psg.facilitei.Entity.Mensagem mensagem = new psg.facilitei.Entity.Mensagem();
        mensagem.setServicoId(servico.getId());
        mensagem.setRemetente("Facilitei");
        mensagem.setTipo("RESUMO");
        mensagem.setConteudo(resumo.toString());
        mensagem.setDataEnvio(java.time.LocalDateTime.now());
        mensagemRepository.save(mensagem);
    }

    private void publicarMensagemDoSistema(Long servicoId, String conteudo) {
        psg.facilitei.Entity.Mensagem mensagem = new psg.facilitei.Entity.Mensagem();
        mensagem.setServicoId(servicoId);
        mensagem.setRemetente("Facilitei");
        mensagem.setTipo("SISTEMA");
        mensagem.setConteudo(conteudo);
        mensagem.setDataEnvio(java.time.LocalDateTime.now());
        psg.facilitei.Entity.Mensagem salva = mensagemRepository.save(mensagem);
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topics/chat/" + servicoId, salva);
        }
    }

    /** Cancelamento solicitado pelo cliente enquanto o serviço ainda não foi concluído. */
    @Transactional
    public ServicoResponseDTO cancelarPeloCliente(Long id, String motivoInformado) {
        Servico servico = buscarEntidadePorId(id);
        if (!CANCELAVEIS_PELO_CLIENTE.contains(servico.getStatusServico())) {
            throw new BusinessRuleException(servico.getStatusServico() == StatusServico.PENDENTE_APROVACAO
                    ? "O profissional já informou a conclusão. Aprove ou conteste o serviço, ou abra uma disputa."
                    : "Este serviço não pode mais ser cancelado.");
        }
        String motivo = motivoInformado == null || motivoInformado.isBlank()
                ? null : HtmlSanitizer.sanitize(motivoInformado.trim());
        if (motivo == null) throw new BusinessRuleException("Informe o motivo do cancelamento.");
        if (motivo.length() > 500) motivo = motivo.substring(0, 500);
        cancelar(servico, motivo);
        publicarMensagemDoSistema(servico.getId(), "O cliente cancelou o serviço. Motivo: " + motivo);
        notificationService.notify(
                servico.getTrabalhador().getId(), NotificationType.STATUS_CHANGED,
                "Serviço cancelado pelo cliente",
                servico.getCliente().getNome() + " cancelou “" + servico.getTitulo() + "”. Motivo: " + motivo,
                "/painel");
        return modelMapper.map(servico, ServicoResponseDTO.class);
    }

    /** Cancelamento administrativo, em qualquer etapa não encerrada. */
    @Transactional
    public ServicoResponseDTO cancelarPeloAdmin(Long id, String motivoInformado) {
        Servico servico = buscarEntidadePorId(id);
        if (!psg.facilitei.Services.AccountModerationService.OPEN_SERVICE.contains(servico.getStatusServico())) {
            throw new BusinessRuleException("Este serviço já está encerrado.");
        }
        String motivo = HtmlSanitizer.sanitize(motivoInformado == null ? "" : motivoInformado.trim());
        if (motivo.length() > 500) motivo = motivo.substring(0, 500);
        cancelar(servico, motivo);
        publicarMensagemDoSistema(servico.getId(), "A administração cancelou este serviço. Motivo: " + motivo);
        for (Long destinatario : List.of(servico.getCliente().getId(), servico.getTrabalhador().getId())) {
            notificationService.notify(destinatario, NotificationType.STATUS_CHANGED,
                    "Serviço cancelado pela administração",
                    "“" + servico.getTitulo() + "” foi cancelado. Motivo: " + motivo, "/painel");
        }
        return modelMapper.map(servico, ServicoResponseDTO.class);
    }

    private void cancelar(Servico servico, String motivo) {
        servico.setStatusServico(StatusServico.CANCELADO);
        servico.setMotivoCancelamento(motivo);
        servicoRepository.save(servico);
        solicitacaoServicoRepository.findByServicoId(servico.getId()).ifPresent(solicitacao -> {
            solicitacao.setStatusSolicitacao(StatusSolicitacao.CANCELADA);
            solicitacao.setMotivoCancelamento(motivo);
            solicitacaoServicoRepository.save(solicitacao);
        });
    }

    private String formatarTipo(String value) {
        return profissaoService == null ? ProfissaoService.humanizar(value) : profissaoService.nome(value);
    }

    private void validarSolicitacaoPendente(SolicitacaoServico solicitacao, ServicoRequestDTO dto) {
        if (solicitacao.getStatusSolicitacao() != StatusSolicitacao.PENDENTE) {
            throw new BusinessRuleException("A solicitação já foi processada.");
        }
        if (!solicitacao.getTrabalhador().getId().equals(dto.getTrabalhadorId())
                || !solicitacao.getCliente().getId().equals(dto.getClienteId())
                || !java.util.Objects.equals(solicitacao.getTipoServico(), dto.getTipoServico())) {
            throw new BusinessRuleException("Os dados do serviço não correspondem à solicitação informada.");
        }
    }

    @Transactional
    public ServicoResponseDTO atualizar(Long id, ServicoRequestDTO dto) {
        Servico existente = servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado para atualização."));

        StatusServico previousStatus = existente.getStatusServico();
        existente.setTitulo(HtmlSanitizer.sanitize(dto.getTitulo()));
        existente.setDescricao(HtmlSanitizer.sanitize(dto.getDescricao()));
        existente.setStatusServico(dto.getStatusServico());

        Servico atualizado = servicoRepository.save(existente);
        if (previousStatus != atualizado.getStatusServico()) {
            notifyStatusChange(atualizado, previousStatus);
        }
        return modelMapper.map(atualizado, ServicoResponseDTO.class);
    }

    private void notifyStatusChange(Servico servico, StatusServico previousStatus) {
        if (servico.getStatusServico() == StatusServico.PENDENTE_APROVACAO) {
            notificationService.notify(
                    servico.getCliente().getId(), NotificationType.APPROVAL_REQUIRED,
                    "Serviço aguardando sua aprovação",
                    servico.getTrabalhador().getNome() + " informou que “" + servico.getTitulo() + "” foi concluído.",
                    "/painel");
        } else if (servico.getStatusServico() == StatusServico.FINALIZADO) {
            notificationService.notify(
                    servico.getTrabalhador().getId(), NotificationType.STATUS_CHANGED,
                    "Serviço aprovado",
                    servico.getCliente().getNome() + " aprovou a conclusão de “" + servico.getTitulo() + "”.",
                    "/painel");
        } else if (previousStatus == StatusServico.PENDENTE_APROVACAO
                && servico.getStatusServico() == StatusServico.EM_ANDAMENTO) {
            notificationService.notify(
                    servico.getTrabalhador().getId(), NotificationType.STATUS_CHANGED,
                    "Cliente pediu ajustes",
                    servico.getCliente().getNome() + " contestou a conclusão de “" + servico.getTitulo() + "”.",
                    "/painel/chat/" + servico.getId());
        }
    }

    @Transactional
    public void deletar(Long id) {
        if (!servicoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Serviço não encontrado para exclusão.");
        }

        // Remove avaliações vinculadas e desvincula a solicitação de origem
        // para não violar as FKs que apontam para este serviço.
        avaliacaoServicoRepository.deleteByServicoId(id);
        solicitacaoServicoRepository.findByServicoId(id).ifPresent(solicitacao -> {
            solicitacao.setServico(null);
            solicitacaoServicoRepository.save(solicitacao);
        });

        servicoRepository.deleteById(id);
    }

    // Em ServicoService.java

    public List<ServicoResponseDTO> listarPorCliente(Long clienteId) {
        // Você já tem o método findByClienteId no Repository, basta usar
        List<Servico> servicos = servicoRepository.findByClienteId(clienteId);

        return servicos.stream()
                .map(servico -> modelMapper.map(servico, ServicoResponseDTO.class))
                .collect(Collectors.toList());
    }
    // Adicione este método dentro da classe ServicoService

    public List<ServicoResponseDTO> listarPorTrabalhador(Long trabalhadorId) {
        List<Servico> servicos = servicoRepository.findByTrabalhadorId(trabalhadorId);

        return servicos.stream()
                .map(servico -> modelMapper.map(servico, ServicoResponseDTO.class))
                .collect(Collectors.toList());
    }
}
