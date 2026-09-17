package psg.facilitei.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import psg.facilitei.DTO.PortfolioRequestDTO;
import psg.facilitei.DTO.PortfolioResponseDTO;
import psg.facilitei.Entity.Portfolio;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Exceptions.ResourceNotFoundException;
import psg.facilitei.Repository.PortfolioRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private TrabalhadorService trabalhadorService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Transactional
    public PortfolioResponseDTO criar(PortfolioRequestDTO dto) {
        if (portfolioRepository.existsByTrabalhadorId(dto.getTrabalhadorId())) {
            throw new BusinessRuleException("Este trabalhador já possui um portfolio cadastrado.");
        }

        Trabalhador trabalhador = trabalhadorService.buscarEntidadePorId(dto.getTrabalhadorId());

        Portfolio portfolio = new Portfolio();
        portfolio.setTrabalhador(trabalhador);
        portfolio.setUrlsImagens(uploadImagens(dto.getImagens()));

        Portfolio salvo = portfolioRepository.save(portfolio);
        return toResponseDTO(salvo);
    }

    public PortfolioResponseDTO buscarPorId(Long id) {
        Portfolio portfolio = buscarEntidadePorId(id);
        return toResponseDTO(portfolio);
    }

    public Portfolio buscarEntidadePorId(Long id) {
        return portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio não encontrado com ID: " + id));
    }

    public PortfolioResponseDTO buscarPorTrabalhador(Long trabalhadorId) {
        Portfolio portfolio = portfolioRepository.findByTrabalhadorId(trabalhadorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Portfolio não encontrado para o trabalhador com ID: " + trabalhadorId));
        return toResponseDTO(portfolio);
    }

    @Transactional
    public PortfolioResponseDTO adicionarImagens(Long id, List<MultipartFile> imagens) {
        Portfolio portfolio = buscarEntidadePorId(id);
        portfolio.getUrlsImagens().addAll(uploadImagens(imagens));

        Portfolio atualizado = portfolioRepository.save(portfolio);
        return toResponseDTO(atualizado);
    }

    @Transactional
    public void deletar(Long id) {
        if (!portfolioRepository.existsById(id)) {
            throw new ResourceNotFoundException("Portfolio não encontrado para exclusão.");
        }
        portfolioRepository.deleteById(id);
    }

    private List<String> uploadImagens(List<MultipartFile> imagens) {
        if (imagens == null || imagens.isEmpty()) {
            throw new BusinessRuleException("É necessário enviar ao menos uma imagem.");
        }
        return imagens.stream()
                .map(cloudinaryService::uploadArquivo)
                .collect(Collectors.toList());
    }

    private PortfolioResponseDTO toResponseDTO(Portfolio portfolio) {
        PortfolioResponseDTO dto = new PortfolioResponseDTO();
        dto.setId(portfolio.getId());
        dto.setUrlsImagens(portfolio.getUrlsImagens());
        if (portfolio.getTrabalhador() != null) {
            dto.setTrabalhadorId(portfolio.getTrabalhador().getId());
        }
        return dto;
    }
}
