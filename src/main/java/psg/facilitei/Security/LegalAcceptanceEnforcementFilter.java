package psg.facilitei.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import psg.facilitei.Services.LegalAcceptanceService;

@Component
public class LegalAcceptanceEnforcementFilter extends OncePerRequestFilter {
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private final LegalAcceptanceService acceptances;

    public LegalAcceptanceEnforcementFilter(LegalAcceptanceService acceptances) {
        this.acceptances = acceptances;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return SAFE_METHODS.contains(request.getMethod())
                || path.startsWith("/api/legal/")
                || path.startsWith("/api/auth/")
                || path.equals("/api/clientes")
                || path.equals("/api/trabalhadores")
                || path.equals("/api/assinaturas/prestador/webhook");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthenticatedPrincipal principal
                && !acceptances.hasCurrentGlobalAcceptance(principal.id(), principal.role())) {
            response.setStatus(428);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"LEGAL_ACCEPTANCE_REQUIRED\","
                    + "\"message\":\"Aceite os Termos de Uso e a Política de Privacidade vigentes antes de continuar.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
