package psg.facilitei.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import psg.facilitei.Entity.PasswordResetToken;
import psg.facilitei.Entity.Usuario;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Repository.PasswordResetTokenRepository;
import psg.facilitei.Repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailService emailService;

    @Value("${app.reset-password-url}")
    private String resetPasswordUrl;

    private static final int EXPIRACAO_MINUTOS = 30;

    @Transactional
    public void solicitarRecuperacao(String email) {
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setUsuarioId(usuario.getId());
            resetToken.setDataExpiracao(LocalDateTime.now().plusMinutes(EXPIRACAO_MINUTOS));
            resetToken.setUsado(false);
            passwordResetTokenRepository.save(resetToken);

            String link = resetPasswordUrl + "?token=" + resetToken.getToken();
            emailService.enviarEmailRecuperacaoSenha(usuario.getEmail(), link);
        });
        // Resposta ao chamador é sempre genérica (ver AuthController) para não revelar se o email existe.
    }

    @Transactional
    public void redefinirSenha(String token, String novaSenha) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessRuleException("Token inválido."));

        if (resetToken.isUsado()) {
            throw new BusinessRuleException("Este token já foi utilizado.");
        }

        if (resetToken.getDataExpiracao().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Este token expirou. Solicite uma nova recuperação de senha.");
        }

        Usuario usuario = usuarioRepository.findById(resetToken.getUsuarioId())
                .orElseThrow(() -> new BusinessRuleException("Usuário não encontrado."));

        usuario.setSenha(novaSenha);
        usuarioRepository.save(usuario);

        resetToken.setUsado(true);
        passwordResetTokenRepository.save(resetToken);
    }
}
