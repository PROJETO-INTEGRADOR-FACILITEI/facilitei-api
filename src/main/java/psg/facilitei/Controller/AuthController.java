package psg.facilitei.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import psg.facilitei.DTO.ForgotPasswordRequestDTO;
import psg.facilitei.DTO.ClienteResponseDTO;
import psg.facilitei.DTO.LoginRequestDTO;
import psg.facilitei.DTO.LoginResponseDTO;
import psg.facilitei.DTO.ResetPasswordRequestDTO;
import psg.facilitei.DTO.TrabalhadorResponseDTO;
import psg.facilitei.Services.AuthService; // Vamos criar este
import psg.facilitei.Services.PasswordResetService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String SESSION_ROLE = "auth.role";
    private static final String SESSION_USER_ID = "auth.userId";
    private static final int SESSION_TTL_SECONDS = 7 * 24 * 60 * 60;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest,
                                                   HttpServletRequest request) {
        LoginResponseDTO response = authService.login(loginRequest.getEmail(), loginRequest.getSenha());
        HttpSession existing = request.getSession(false);
        if (existing != null) existing.invalidate();
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(SESSION_TTL_SECONDS);
        session.setAttribute(SESSION_ROLE, response.getRole());
        session.setAttribute(SESSION_USER_ID, extrairId(response));
        session.setAttribute("auth.name", extrairNome(response));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(CsrfToken token) {
        return ResponseEntity.ok(Map.of(
                "headerName", token.getHeaderName(),
                "parameterName", token.getParameterName(),
                "token", token.getToken()));
    }

    @GetMapping("/session")
    public ResponseEntity<LoginResponseDTO> session(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) throw sessaoInvalida();
        Object role = session.getAttribute(SESSION_ROLE);
        Object userId = session.getAttribute(SESSION_USER_ID);
        if (!(role instanceof String) || !(userId instanceof Long)) throw sessaoInvalida();
        return ResponseEntity.ok(authService.restaurarSessao((String) role, (Long) userId));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO dto) {
        passwordResetService.solicitarRecuperacao(dto.getEmail());
        return ResponseEntity.ok(Map.of("message",
                "Se o email informado estiver cadastrado, você receberá um link de recuperação."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO dto) {
        passwordResetService.redefinirSenha(dto.getToken(), dto.getNovaSenha());
        return ResponseEntity.ok(Map.of("message", "Senha redefinida com sucesso."));
    }

    private Long extrairId(LoginResponseDTO response) {
        Object user = response.getUser();
        String id;
        if (user instanceof ClienteResponseDTO cliente) {
            id = cliente.getId();
        } else if (user instanceof TrabalhadorResponseDTO trabalhador) {
            id = trabalhador.getId();
        } else {
            throw new IllegalStateException("Tipo de usuário inválido na autenticação.");
        }
        return Long.valueOf(id);
    }

    private String extrairNome(LoginResponseDTO response) {
        if (response.getUser() instanceof ClienteResponseDTO cliente) return cliente.getNome();
        if (response.getUser() instanceof TrabalhadorResponseDTO trabalhador) return trabalhador.getNome();
        throw new IllegalStateException("Tipo de usuário inválido na autenticação.");
    }

    private ResponseStatusException sessaoInvalida() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida ou expirada.");
    }
}
