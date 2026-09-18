package psg.facilitei.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import psg.facilitei.DTO.PortfolioRequestDTO;
import psg.facilitei.DTO.PortfolioResponseDTO;
import psg.facilitei.Entity.Portfolio;
import psg.facilitei.Entity.PortfolioImagem;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Repository.PortfolioRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private TrabalhadorService trabalhadorService;
    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private PortfolioService portfolioService;

    @Test
    void criar_enviaImagemAoCloudinaryESalvaPublicId() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "imagens", "servico.png", "image/png", new byte[] { 1, 2, 3 });
        PortfolioRequestDTO request = new PortfolioRequestDTO(7L, List.of(arquivo));
        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setId(7L);

        when(portfolioRepository.existsByTrabalhadorId(7L)).thenReturn(false);
        when(trabalhadorService.buscarEntidadePorId(7L)).thenReturn(trabalhador);
        when(cloudinaryService.uploadImagemPortfolio(arquivo))
                .thenReturn(new CloudinaryService.UploadImagemResult(
                        "https://res.cloudinary.com/facilitei/image/upload/servico.png",
                        "facilitei/portfolios/servico"));
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(invocation -> {
            Portfolio portfolio = invocation.getArgument(0);
            portfolio.setId(3L);
            portfolio.getImagens().get(0).setId(11L);
            return portfolio;
        });

        PortfolioResponseDTO response = portfolioService.criar(request);

        assertEquals(3L, response.getId());
        assertEquals(7L, response.getTrabalhadorId());
        assertEquals(11L, response.getImagens().get(0).getId());
        verify(cloudinaryService).uploadImagemPortfolio(arquivo);
    }

    @Test
    void criar_quandoTrabalhadorJaTemPortfolio_rejeitaDuplicidade() {
        PortfolioRequestDTO request = new PortfolioRequestDTO(7L, List.of());
        when(portfolioRepository.existsByTrabalhadorId(7L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> portfolioService.criar(request));
    }

    @Test
    void removerImagem_apagaNoCloudinaryENoPortfolio() {
        Portfolio portfolio = new Portfolio();
        portfolio.setId(3L);
        PortfolioImagem imagem = new PortfolioImagem();
        imagem.setId(11L);
        imagem.setUrl("https://res.cloudinary.com/facilitei/image/upload/servico.png");
        imagem.setPublicId("facilitei/portfolios/servico");
        portfolio.adicionarImagem(imagem);

        when(portfolioRepository.findById(3L)).thenReturn(Optional.of(portfolio));

        portfolioService.removerImagem(3L, 11L);

        assertEquals(0, portfolio.getImagens().size());
        verify(cloudinaryService).removerImagem("facilitei/portfolios/servico");
        verify(portfolioRepository).save(portfolio);
    }
}
