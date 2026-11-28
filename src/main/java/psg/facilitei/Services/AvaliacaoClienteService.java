package psg.facilitei.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import psg.facilitei.DTO.AvaliacaoClienteRequestDTO;
import psg.facilitei.DTO.AvaliacaoClienteResponseDTO;
import psg.facilitei.Entity.AvaliacaoCliente;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Exceptions.ResourceNotFoundException;
import psg.facilitei.Repository.AvaliacaoClienteRepository;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.TrabalhadorRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AvaliacaoClienteService {

    @Autowired
    private AvaliacaoClienteRepository avaliacaoClienteRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TrabalhadorRepository trabalhadorRepository;

    @Transactional
    public AvaliacaoClienteResponseDTO criarAvaliacao(AvaliacaoClienteRequestDTO dto) {
        if (dto.getServicoId() != null
                && avaliacaoClienteRepository.existsByTrabalhadorIdAndServicoId(dto.getTrabalhadorId(), dto.getServicoId())) {
            throw new BusinessRuleException("Este trabalhador já avaliou este cliente para este serviço.");
        }

        AvaliacaoCliente avaliacao = toEntity(dto);

        // A média é calculada antes de salvar: avaliações antigas + a nota que está chegando
        List<AvaliacaoCliente> avaliacoesAntigas = avaliacaoClienteRepository.findByClienteId(dto.getClienteId());

        double somaNotas = avaliacoesAntigas.stream().mapToDouble(AvaliacaoCliente::getNota).sum();
        somaNotas += dto.getNota();

        double novaMedia = somaNotas / (avaliacoesAntigas.size() + 1);

        // Guarda a média do momento na avaliação e também atualiza o perfil do cliente
        avaliacao.setMediaCliente(novaMedia);

        AvaliacaoCliente salvo = avaliacaoClienteRepository.save(avaliacao);

        Cliente cliente = avaliacao.getCliente();
        cliente.setNotaCliente(novaMedia);
        clienteRepository.save(cliente);

        return toResponseDTO(salvo);
    }

    public List<AvaliacaoClienteResponseDTO> listarPorCliente(Long clienteId) {
        List<AvaliacaoCliente> avaliacoes = avaliacaoClienteRepository.findByClienteId(clienteId);
        return avaliacoes.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<AvaliacaoClienteResponseDTO> listarPorTrabalhador(Long trabalhadorId) {
        List<AvaliacaoCliente> avaliacoes = avaliacaoClienteRepository.findByTrabalhadorId(trabalhadorId);
        return avaliacoes.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deletarAvaliacao(Long avaliacaoId) {
        AvaliacaoCliente avaliacao = avaliacaoClienteRepository.findById(avaliacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Avaliação não encontrada"));

        Cliente cliente = avaliacao.getCliente();
        avaliacaoClienteRepository.delete(avaliacao);

        atualizarMediaAposDelecao(cliente);
    }

    private AvaliacaoCliente toEntity(AvaliacaoClienteRequestDTO dto) {
        Trabalhador trabalhador = trabalhadorRepository.findById(dto.getTrabalhadorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trabalhador não encontrado. ID: " + dto.getTrabalhadorId()));

        Cliente cliente = clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado. ID: " + dto.getClienteId()));

        AvaliacaoCliente avaliacao = new AvaliacaoCliente();
        avaliacao.setTrabalhador(trabalhador);
        avaliacao.setCliente(cliente);
        avaliacao.setServicoId(dto.getServicoId());
        avaliacao.setNota(dto.getNota());
        avaliacao.setComentario(psg.facilitei.Util.HtmlSanitizer.sanitize(dto.getComentario()));
        // mediaCliente é preenchida em criarAvaliacao

        return avaliacao;
    }

    private AvaliacaoClienteResponseDTO toResponseDTO(AvaliacaoCliente entity) {
        AvaliacaoClienteResponseDTO dto = new AvaliacaoClienteResponseDTO();
        dto.setId(entity.getId());
        dto.setTrabalhadorId(entity.getTrabalhador().getId());
        dto.setClienteId(entity.getCliente().getId());
        dto.setServicoId(entity.getServicoId());
        dto.setNota(entity.getNota());
        dto.setComentario(entity.getComentario());
        dto.setMediaCliente(entity.getMediaCliente());
        dto.setData(entity.getData());
        return dto;
    }

    private void atualizarMediaAposDelecao(Cliente cliente) {
        List<AvaliacaoCliente> restantes = avaliacaoClienteRepository.findByClienteId(cliente.getId());
        if (restantes.isEmpty()) {
            cliente.setNotaCliente(0.0);
        } else {
            double novaMedia = restantes.stream().mapToDouble(AvaliacaoCliente::getNota).average().orElse(0.0);
            cliente.setNotaCliente(novaMedia);
        }
        clienteRepository.save(cliente);
    }
}