package psg.facilitei.Security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;

@Configuration
public class SecurityConfig {

    // Alinhados ao cookie de sessao. Cross-origin (Vercel + Render) exige
    // SameSite=None e Secure=true para o navegador enviar o cookie CSRF.
    @Value("${app.cookie.same-site:lax}")
    private String cookieSameSite;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            SessionAuthenticationFilter sessionAuthenticationFilter,
                                            SensitiveEndpointRateLimitFilter rateLimitFilter) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");
        csrfRepository.setCookieCustomizer(cookie -> cookie
                .sameSite(cookieSameSite)
                .secure(cookieSecure));
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();

        return http
                .cors(cors -> {})
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(csrfHandler)
                        // O provedor não possui o cookie CSRF do navegador; este
                        // endpoint é autenticado pela assinatura do webhook.
                        .ignoringRequestMatchers("/api/assinaturas/prestador/webhook"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET,
                                "/health",
                                "/api/auth/csrf",
                                "/api/trabalhadores/listar/**",
                                "/api/trabalhadores/buscarPorId/**",
                                "/api/portfolios/**",
                                "/api/avaliacoes-servico/trabalhador/**",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/clientes",
                                "/api/trabalhadores",
                                "/api/arquivos/upload",
                                "/api/assinaturas/prestador/webhook").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Autenticação necessária.\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            if (exception instanceof MissingCsrfTokenException
                                    || exception instanceof InvalidCsrfTokenException) {
                                response.getWriter().write("{\"code\":\"CSRF_INVALID\",\"message\":\"Token de segurança inválido ou expirado.\"}");
                            } else {
                                response.getWriter().write("{\"message\":\"Acesso negado.\"}");
                            }
                        }))
                .addFilterBefore(rateLimitFilter, AnonymousAuthenticationFilter.class)
                .addFilterBefore(sessionAuthenticationFilter, AnonymousAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
