package psg.facilitei.Services;

import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${abacatepay.product-id:}")
    private String monthlyProductId;

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
        if (monthlyProductId != null && !monthlyProductId.isBlank()
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

        notificationService.notify(
                cliente.getId(), NotificationType.REQUEST_ACCEPTED,
                "Seu pedido foi aceito",
                trabalhador.getNome() + " aceitou seu pedido. Agora vocês já podem conversar.",
                "/painel/chat/" + salvo.getId());

        return modelMapper.map(salvo, ServicoResponseDTO.class);
    }

    private void validarSolicitacaoPendente(SolicitacaoServico solicitacao, ServicoRequestDTO dto) {
        if (solicitacao.getStatusSolicitacao() != StatusSolicitacao.PENDENTE) {
            throw new BusinessRuleException("A solicitação já foi processada.");
        }
        if (!solicitacao.getTrabalhador().getId().equals(dto.getTrabalhadorId())
                || !solicitacao.getCliente().getId().equals(dto.getClienteId())
                || solicitacao.getTipoServico() != dto.getTipoServico()) {
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
