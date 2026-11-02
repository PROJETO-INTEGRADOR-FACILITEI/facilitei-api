package psg.facilitei.Security;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import psg.facilitei.Entity.Usuario;
import psg.facilitei.Entity.Enum.AccountStatus;
import psg.facilitei.Repository.UsuarioRepository;

class SessionAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sessaoExistenteDeContaSuspensaEInvalidada() throws Exception {
        UsuarioRepository users = mock(UsuarioRepository.class);
        Usuario suspended = new Usuario();
        suspended.setId(7L);
        suspended.setAccountStatus(AccountStatus.SUSPENDED);
        when(users.findById(7L)).thenReturn(Optional.of(suspended));
        SessionAuthenticationFilter filter = new SessionAuthenticationFilter(users);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("auth.role", "cliente");
        session.setAttribute("auth.userId", 7L);
        session.setAttribute("auth.name", "Conta suspensa");
        request.setSession(session);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertTrue(session.isInvalid());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
