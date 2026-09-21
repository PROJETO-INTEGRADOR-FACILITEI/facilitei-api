package psg.facilitei.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SensitiveEndpointRateLimitFilter extends OncePerRequestFilter {
    private record Rule(int limit, Duration window) {}
    private static final Rule UPLOAD_RULE = new Rule(30, Duration.ofHours(1));

    private static final Map<String, Rule> RULES = Map.of(
            "/api/auth/login", new Rule(10, Duration.ofMinutes(15)),
            "/api/auth/forgot-password", new Rule(5, Duration.ofHours(1)),
            "/api/auth/reset-password", new Rule(10, Duration.ofHours(1)),
            "/api/clientes", new Rule(5, Duration.ofHours(1)),
            "/api/trabalhadores", new Rule(5, Duration.ofHours(1)),
            "/api/arquivos/upload", UPLOAD_RULE);

    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();
    private final boolean enabled;

    public SensitiveEndpointRateLimitFilter(@Value("${security.rate-limit.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String endpoint = request.getRequestURI();
        Rule rule = "POST".equals(request.getMethod()) ? RULES.get(endpoint) : null;
        if ("POST".equals(request.getMethod())
                && endpoint.matches("/api/portfolios(?:/\\d+/imagens)?")) {
            rule = UPLOAD_RULE;
            endpoint = "/api/uploads";
        } else if (UPLOAD_RULE.equals(rule)) {
            endpoint = "/api/uploads";
        }
        if (!enabled || rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientAddress(request) + ':' + endpoint;
        Deque<Instant> timestamps = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        Instant now = Instant.now();
        synchronized (timestamps) {
            Instant oldestAllowed = now.minus(rule.window());
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(oldestAllowed)) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= rule.limit()) {
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"message\":\"Muitas tentativas. Aguarde antes de tentar novamente.\"}");
                return;
            }
            timestamps.addLast(now);
        }
        filterChain.doFilter(request, response);
    }

    private String clientAddress(HttpServletRequest request) {
        // X-Forwarded-For só pode ser aceito quando a aplicação conhece e valida
        // o proxy que o escreveu. Confiar nele diretamente permite contornar o
        // limite enviando um IP diferente em cada requisição.
        return request.getRemoteAddr();
    }
}
