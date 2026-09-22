package psg.facilitei.Services;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import psg.facilitei.DTO.SolicitacaoServicoRequestDTO;
import psg.facilitei.DTO.SolicitacaoServicoResponseDTO;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.SolicitacaoServico;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Entity.Enum.StatusSolicitacao;
import psg.facilitei.Entity.Enum.NotificationType;
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

    @Autowired
    private SolicitacaoServicoRepository solicitacaoServicoRepository;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private TrabalhadorRepository trabalhadorRepository;

    @Autowired
    private AssinaturaPrestadorRepository assinaturaPrestadorRepository;

    @Value("${abacatepay.product-id:}")
    private String monthlyProductId;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public SolicitacaoServicoResponseDTO criar(SolicitacaoServicoRequestDTO dto) {
        SolicitacaoServico solicitacao = new SolicitacaoServico();

        // 1. Busca e define o Cliente
        Cliente cliente = clienteService.buscarEntidadePorId(dto.getClienteId());
        solicitacao.setCliente(cliente);

        // 2. Busca e define o Trabalhador (Obrigatório nesta etapa)
        Trabalhador trabalhador = trabalhadorRepository.findById(dto.getTrabalhadorId())
                .orElseThrow(() -> new ResourceNotFoundException("Trabalhador não encontrado com ID: " + dto.getTrabalhadorId()));
        if (monthlyProductId != null && !monthlyProductId.isBlank()
                && !assinaturaPrestadorRepository.existsByTrabalhadorIdAndStatusAndActiveUntilAfter(
                        trabalhador.getId(), "ACTIVE", java.time.Instant.now())) {
            throw new BusinessRuleException("Este profissional precisa de uma assinatura mensal ativa.");
        }
        solicitacao.setTrabalhador(trabalhador);

        // 3. Define dados da solicitação
        solicitacao.setDescricao(HtmlSanitizer.sanitize(dto.getDescricao()));
        solicitacao.setTipoServico(dto.getTipoServico());
        solicitacao.setDataSolicitacao(LocalDateTime.now());
        
        // 4. O serviço começa como NULL (será criado quando o trabalhador aceitar)
        solicitacao.setServico(null);

        solicitacao.setStatusSolicitacao(StatusSolicitacao.PENDENTE);

        SolicitacaoServico salvo = solicitacaoServicoRepository.save(solicitacao);
        notificationService.notify(
                trabalhador.getId(), NotificationType.NEW_REQUEST,
                "Nova solicitação de serviço",
                cliente.getNome() + " precisa de " + formatarTipo(solicitacao.getTipoServico().name()) + ".",
                "/painel");
        
        // Mapeamento manual para garantir retorno correto dos IDs
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
        // Adicione outras atualizações conforme necessário

        SolicitacaoServico atualizada = solicitacaoServicoRepository.save(existente);
        notificationService.notify(
                existente.getCliente().getId(), NotificationType.REQUEST_DECLINED,
                "Solicitação não aceita",
                existente.getTrabalhador().getNome() + " não pôde aceitar seu pedido de "
                        + formatarTipo(existente.getTipoServico().name()) + ".",
                "/painel");
        return mapToResponse(atualizada);
    }
    
    @Transactional
    public void deletar(Long id) {
        if (!solicitacaoServicoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Solicitação não encontrada para exclusão ID: " + id);
        }
        solicitacaoServicoRepository.deleteById(id);
    }

    // Helper para converter entidade para DTO
    private SolicitacaoServicoResponseDTO mapToResponse(SolicitacaoServico entity) {
        return new SolicitacaoServicoResponseDTO(
            entity.getId(),
            entity.getCliente().getId(),
            entity.getTrabalhador().getId(),
            entity.getServico() != null ? entity.getServico().getId() : null,
            entity.getTipoServico(),
            entity.getDescricao(),
            entity.getStatusSolicitacao().name()
        );
    }

    private String formatarTipo(String value) {
        return value.toLowerCase().replace('_', ' ');
    }
}
