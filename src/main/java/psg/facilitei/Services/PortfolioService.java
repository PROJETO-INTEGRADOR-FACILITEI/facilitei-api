package psg.facilitei.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import psg.facilitei.DTO.PortfolioRequestDTO;
import psg.facilitei.DTO.PortfolioImagemResponseDTO;
import psg.facilitei.DTO.PortfolioResponseDTO;
import psg.facilitei.Entity.Portfolio;
import psg.facilitei.Entity.PortfolioImagem;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Entity.Enum.TipoServico;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Exceptions.ResourceNotFoundException;
import psg.facilitei.Repository.PortfolioRepository;

import java.util.List;
import java.util.ArrayList;

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
        adicionarUploads(portfolio, dto.getImagens(), dto.getTipoServico());

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
    public PortfolioResponseDTO adicionarImagens(Long id, List<MultipartFile> imagens, TipoServico tipoServico) {
        Portfolio portfolio = buscarEntidadePorId(id);
        adicionarUploads(portfolio, imagens, tipoServico);

        Portfolio atualizado = portfolioRepository.save(portfolio);
        return toResponseDTO(atualizado);
    }

    @Transactional
    public void removerImagem(Long portfolioId, Long imagemId) {
        Portfolio portfolio = buscarEntidadePorId(portfolioId);
        PortfolioImagem imagem = portfolio.getImagens().stream()
                .filter(item -> item.getId().equals(imagemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Imagem não encontrada neste portfolio com ID: " + imagemId));

        cloudinaryService.removerImagem(imagem.getPublicId());
        portfolio.removerImagem(imagem);
        portfolioRepository.save(portfolio);
    }

    @Transactional
    public void deletar(Long id) {
        Portfolio portfolio = buscarEntidadePorId(id);
        portfolio.getImagens().forEach(imagem -> cloudinaryService.removerImagem(imagem.getPublicId()));
        portfolioRepository.deleteById(id);
    }

    private void adicionarUploads(Portfolio portfolio, List<MultipartFile> imagens, TipoServico tipoServico) {
        if (imagens == null || imagens.isEmpty()) {
            throw new BusinessRuleException("É necessário enviar ao menos uma imagem.");
        }

        List<PortfolioImagem> imagensEnviadas = new ArrayList<>();
        try {
            for (MultipartFile arquivo : imagens) {
                CloudinaryService.UploadImagemResult upload = cloudinaryService.uploadImagemPortfolio(arquivo);
                PortfolioImagem imagem = new PortfolioImagem();
                imagem.setUrl(upload.url());
                imagem.setPublicId(upload.publicId());
                imagem.setTipoServico(tipoServico);
                portfolio.adicionarImagem(imagem);
                imagensEnviadas.add(imagem);
            }
        } catch (RuntimeException exception) {
            imagensEnviadas.forEach(imagem -> {
                try {
                    cloudinaryService.removerImagem(imagem.getPublicId());
                } catch (RuntimeException ignored) {
                    // Preserva a falha original do upload.
                }
                portfolio.removerImagem(imagem);
            });
            throw exception;
        }
    }

    private PortfolioResponseDTO toResponseDTO(Portfolio portfolio) {
        PortfolioResponseDTO dto = new PortfolioResponseDTO();
        dto.setId(portfolio.getId());
        dto.setImagens(portfolio.getImagens().stream()
                .map(imagem -> new PortfolioImagemResponseDTO(
                        imagem.getId(), imagem.getUrl(), imagem.getTipoServico()))
                .toList());
        if (portfolio.getTrabalhador() != null) {
            dto.setTrabalhadorId(portfolio.getTrabalhador().getId());
        }
        return dto;
    }
}
