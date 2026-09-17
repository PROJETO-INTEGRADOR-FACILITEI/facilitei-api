package psg.facilitei.Services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import psg.facilitei.Entity.PasswordResetToken;
import psg.facilitei.Entity.Usuario;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Repository.PasswordResetTokenRepository;
import psg.facilitei.Repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "resetPasswordUrl", "http://localhost:5173/reset-password");
    }

    @Test
    void solicitarRecuperacao_quandoUsuarioExiste_salvaTokenEEnviaEmail() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("teste@teste.com");
        when(usuarioRepository.findByEmail("teste@teste.com")).thenReturn(Optional.of(usuario));

        passwordResetService.solicitarRecuperacao("teste@teste.com");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getUsuarioId());
        assertFalse(captor.getValue().isUsado());
        assertTrue(captor.getValue().getDataExpiracao().isAfter(LocalDateTime.now()));

        verify(emailService).enviarEmailRecuperacaoSenha(eq("teste@teste.com"), anyString());
    }

    @Test
    void solicitarRecuperacao_quandoUsuarioNaoExiste_naoLancaExcecaoENaoEnviaEmail() {
        when(usuarioRepository.findByEmail("naoexiste@teste.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> passwordResetService.solicitarRecuperacao("naoexiste@teste.com"));

        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).enviarEmailRecuperacaoSenha(anyString(), anyString());
    }

    @Test
    void redefinirSenha_comTokenValido_atualizaSenhaEMarcaTokenComoUsado() {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(10L);
        token.setToken("abc123");
        token.setUsuarioId(1L);
        token.setUsado(false);
        token.setDataExpiracao(LocalDateTime.now().plusMinutes(10));

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setSenha("senhaAntiga");

        when(passwordResetTokenRepository.findByToken("abc123")).thenReturn(Optional.of(token));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        passwordResetService.redefinirSenha("abc123", "senhaNova");

        assertEquals("senhaNova", usuario.getSenha());
        assertTrue(token.isUsado());
        verify(usuarioRepository).save(usuario);
        verify(passwordResetTokenRepository).save(token);
    }

    @Test
    void redefinirSenha_comTokenInexistente_lancaBusinessRuleException() {
        when(passwordResetTokenRepository.findByToken("invalido")).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> passwordResetService.redefinirSenha("invalido", "senhaNova"));
    }

    @Test
    void redefinirSenha_comTokenExpirado_lancaBusinessRuleException() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("expirado");
        token.setUsuarioId(1L);
        token.setUsado(false);
        token.setDataExpiracao(LocalDateTime.now().minusMinutes(1));

        when(passwordResetTokenRepository.findByToken("expirado")).thenReturn(Optional.of(token));

        assertThrows(BusinessRuleException.class,
                () -> passwordResetService.redefinirSenha("expirado", "senhaNova"));
    }

    @Test
    void redefinirSenha_comTokenJaUsado_lancaBusinessRuleException() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("usado");
        token.setUsuarioId(1L);
        token.setUsado(true);
        token.setDataExpiracao(LocalDateTime.now().plusMinutes(10));

        when(passwordResetTokenRepository.findByToken("usado")).thenReturn(Optional.of(token));

        assertThrows(BusinessRuleException.class,
                () -> passwordResetService.redefinirSenha("usado", "senhaNova"));
    }
}
