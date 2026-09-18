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
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Exceptions.ResourceNotFoundException;
import psg.facilitei.Repository.AvaliacaoServicoRepository;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.ServicoRepository;
import psg.facilitei.Repository.SolicitacaoServicoRepository;
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

        modelMapper.map(dto, existente);
        existente.setTitulo(HtmlSanitizer.sanitize(dto.getTitulo()));
        existente.setDescricao(HtmlSanitizer.sanitize(dto.getDescricao()));

        if (dto.getTrabalhadorId() != null && !existente.getTrabalhador().getId().equals(dto.getTrabalhadorId())) {
            Trabalhador novoTrabalhador = trabalhadorService.buscarEntidadePorId(dto.getTrabalhadorId());
            existente.setTrabalhador(novoTrabalhador);
        }

        if (dto.getClienteId() != null && !existente.getCliente().getId().equals(dto.getClienteId())) {
            Cliente novoCliente = clienteRepository.findById(dto.getClienteId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Cliente não encontrado com ID: " + dto.getClienteId()));
            existente.setCliente(novoCliente);
        }

        Servico atualizado = servicoRepository.save(existente);
        return modelMapper.map(atualizado, ServicoResponseDTO.class);
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
