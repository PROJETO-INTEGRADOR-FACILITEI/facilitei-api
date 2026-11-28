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
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
public class SecurityConfig {

    // Mesmos valores do cookie de sessão. Com front e API em domínios diferentes precisa
    // SameSite=None e Secure=true, senão o navegador não manda o cookie CSRF.
    @Value("${app.cookie.same-site:lax}")
    private String cookieSameSite;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            SessionAuthenticationFilter sessionAuthenticationFilter,
                                            SensitiveEndpointRateLimitFilter rateLimitFilter) throws Exception {
        // Cookie XSRF-TOKEN legível pelo JS: o front lê o valor e devolve no header X-XSRF-TOKEN
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");
        csrfRepository.setCookieCustomizer(cookie -> cookie
                .sameSite(cookieSameSite)
                .secure(cookieSecure));
        // Handler simples (sem a máscara XOR), assim o valor do cookie vale direto no header
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();

        return http
                .cors(cors -> {})
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(csrfHandler)
                        // O webhook vem do Mercado Pago, sem cookie CSRF. Ele é validado pela assinatura.
                        .ignoringRequestMatchers("/api/assinaturas/prestador/webhook"))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                // O SecurityContext vive só no request e o SessionAuthenticationFilter remonta o login
                // a cada pedido. Salvar na sessão JDBC com pedidos simultâneos dá Duplicate entry (500).
                .securityContext(context -> context.securityContextRepository(
                        new org.springframework.security.web.context.RequestAttributeSecurityContextRepository()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        // O AuthController já cria uma sessão nova no login. Se o Spring trocasse o ID
                        // a cada pedido, pedidos simultâneos com o ID antigo tomariam 401.
                        .sessionFixation(fixation -> fixation.none()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET,
                                "/health",
                                "/api/auth/csrf",
                                "/api/trabalhadores/listar/**",
                                "/api/trabalhadores/buscarPorId/**",
                                "/api/portfolios/**",
                                "/api/servicos-oferecidos/trabalhador/**",
                                "/api/avaliacoes-servico/trabalhador/**",
                                "/api/legal/config",
                                "/api/profissoes",
                                "/api/legal/documents/**",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/clientes",
                                "/api/trabalhadores",
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
