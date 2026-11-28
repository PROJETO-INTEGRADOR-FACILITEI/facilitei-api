package psg.facilitei.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import psg.facilitei.Entity.Enum.AccountStatus;
import psg.facilitei.Repository.UsuarioRepository;

/**
 * Remonta o usuário logado a cada pedido a partir dos atributos auth.* da sessão.
 * Busca o usuário no banco toda vez para barrar na hora conta excluída ou suspensa.
 */
@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {
    private final UsuarioRepository users;

    public SessionAuthenticationFilter(UsuarioRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Object roleValue = session.getAttribute("auth.role");
            Object idValue = session.getAttribute("auth.userId");
            Object nameValue = session.getAttribute("auth.name");
            Object adminValue = session.getAttribute("auth.admin");
            if (roleValue instanceof String role && idValue instanceof Long id
                    && ("cliente".equals(role) || "trabalhador".equals(role))) {
                var user = users.findById(id).orElse(null);
                if (user == null) {
                    session.invalidate();
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
                if (user.isBlockedNow()) {
                    // Devolve 403 com motivo e prazo para o front mostrar a tela de conta suspensa
                    session.invalidate();
                    SecurityContextHolder.clearContext();
                    var body = psg.facilitei.Services.AccountModerationService.suspensionBody(
                            psg.facilitei.Services.AccountModerationService.toException(user));
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper()
                            .writeValueAsString(body));
                    return;
                }
                AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                        id, role, nameValue instanceof String name ? name : role + "#" + id);
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                if (Boolean.TRUE.equals(adminValue)) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                }
                var authentication = UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
