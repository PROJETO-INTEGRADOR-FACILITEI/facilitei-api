package psg.facilitei.Services;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import psg.facilitei.DTO.AvaliacaoTrabalhadorRequestDTO;
import psg.facilitei.DTO.AvaliacaoTrabalhadorResponseDTO;
import psg.facilitei.Entity.AvaliacaoTrabalhador;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Exceptions.ResourceNotFoundException;
import psg.facilitei.Repository.AvaliacaoTrabalhadorRepository;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.TrabalhadorRepository;
import psg.facilitei.Util.HtmlSanitizer;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AvaliacaoTrabalhadorService {

    @Autowired
    private AvaliacaoTrabalhadorRepository repository;
    @Autowired
    private TrabalhadorRepository trabalhadorRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    public AvaliacaoTrabalhadorResponseDTO criar(AvaliacaoTrabalhadorRequestDTO dto) {
        if (repository.existsByClienteIdAndTrabalhadorId(dto.getClienteId(), dto.getTrabalhadorId())) {
            throw new BusinessRuleException("Este cliente já avaliou este trabalhador.");
        }

        Trabalhador trabalhador = trabalhadorRepository.findById(dto.getTrabalhadorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trabalhador não encontrado. ID: " + dto.getTrabalhadorId()));

        Cliente cliente = clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente não encontrado. ID: " + dto.getClienteId()));

        AvaliacaoTrabalhador avaliacao = new AvaliacaoTrabalhador();
        avaliacao.setTrabalhador(trabalhador);
        avaliacao.setCliente(cliente);
        avaliacao.setNota(dto.getNota());
        avaliacao.setComentario(HtmlSanitizer.sanitize(dto.getComentario()));
        avaliacao.setFotos(dto.getFotos());
        avaliacao.setData(new Date());

        AvaliacaoTrabalhador salvo = repository.save(avaliacao);
        return modelMapper.map(salvo, AvaliacaoTrabalhadorResponseDTO.class);
    }

    public List<AvaliacaoTrabalhadorResponseDTO> listarPorTrabalhador(Long trabalhadorId) {
        return repository.findByTrabalhadorId(trabalhadorId).stream()
                .map(avaliacao -> modelMapper.map(avaliacao, AvaliacaoTrabalhadorResponseDTO.class))
                .collect(Collectors.toList());
    }
}
