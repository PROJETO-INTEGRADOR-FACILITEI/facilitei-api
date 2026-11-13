package psg.facilitei.Security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveEndpointRateLimitFilterTest {
    @Test
    void naoLimitaTentativasDeLogin() throws Exception {
        SensitiveEndpointRateLimitFilter filter = new SensitiveEndpointRateLimitFilter(true);
        FilterChain chain = (request, response) -> {};

        for (int tentativa = 0; tentativa < 50; tentativa++) {
            MockHttpServletResponse response = executar(filter, chain, "/api/auth/login");
            assertTrue(response.getStatus() < 400);
        }
    }

    @Test
    void bloqueiaForgotPasswordAposCincoTentativasDoMesmoIp() throws Exception {
        SensitiveEndpointRateLimitFilter filter = new SensitiveEndpointRateLimitFilter(true);
        FilterChain chain = (request, response) -> {};

        for (int tentativa = 0; tentativa < 5; tentativa++) {
            assertTrue(executar(filter, chain, "/api/auth/forgot-password").getStatus() < 400);
        }

        assertEquals(429, executar(filter, chain, "/api/auth/forgot-password").getStatus());
    }

    @Test
    void agrupaUploadsGeraisEDePortfolioNoMesmoLimite() throws Exception {
        SensitiveEndpointRateLimitFilter filter = new SensitiveEndpointRateLimitFilter(true);
        FilterChain chain = (request, response) -> {};

        for (int tentativa = 0; tentativa < 15; tentativa++) {
            executar(filter, chain, "/api/arquivos/upload");
            executar(filter, chain, "/api/portfolios/42/imagens");
        }

        assertEquals(429, executar(filter, chain, "/api/portfolios").getStatus());
    }

    private MockHttpServletResponse executar(SensitiveEndpointRateLimitFilter filter,
                                              FilterChain chain,
                                              String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr("203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }
}
