package psg.facilitei.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Object roleValue = session.getAttribute("auth.role");
            Object idValue = session.getAttribute("auth.userId");
            Object nameValue = session.getAttribute("auth.name");
            if (roleValue instanceof String role && idValue instanceof Long id
                    && ("cliente".equals(role) || "trabalhador".equals(role))) {
                AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                        id, role, nameValue instanceof String name ? name : role + "#" + id);
                var authentication = UsernamePasswordAuthenticationToken.authenticated(
                        principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
