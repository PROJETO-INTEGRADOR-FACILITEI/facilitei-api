package psg.facilitei.Security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import psg.facilitei.Services.LegalAcceptanceService;

class LegalAcceptanceEnforcementFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bloqueiaMutacaoAutenticadaSemAceiteGlobalVigente() throws Exception {
        LegalAcceptanceService acceptances = mock(LegalAcceptanceService.class);
        when(acceptances.hasCurrentGlobalAcceptance(7L, "cliente")).thenReturn(false);
        var principal = new AuthenticatedPrincipal(7L, "cliente", "Cliente");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, java.util.List.of()));
        LegalAcceptanceEnforcementFilter filter = new LegalAcceptanceEnforcementFilter(acceptances);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/solicitacoes-servico");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(428, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
    }

    @Test
    void permiteRegistrarAceiteMesmoQuandoPendente() throws Exception {
        LegalAcceptanceService acceptances = mock(LegalAcceptanceService.class);
        var principal = new AuthenticatedPrincipal(7L, "cliente", "Cliente");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, java.util.List.of()));
        LegalAcceptanceEnforcementFilter filter = new LegalAcceptanceEnforcementFilter(acceptances);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/legal/acceptances");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }
}
