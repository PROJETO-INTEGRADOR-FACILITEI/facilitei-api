package psg.facilitei.Security;

import java.util.List;
import org.junit.jupiter.api.Test;
import psg.facilitei.Entity.Usuario;
import psg.facilitei.Repository.UsuarioRepository;
import psg.facilitei.Services.PasswordHashService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegacyPasswordMigrationRunnerTest {
    @Test
    void migraSomenteSenhasAindaEmTextoPuro() throws Exception {
        Usuario legado = usuario("senha-legada");
        Usuario migrado = usuario("$2a$hash-existente");
        UsuarioRepository repository = mock(UsuarioRepository.class);
        PasswordHashService passwords = mock(PasswordHashService.class);

        when(repository.findAll()).thenReturn(List.of(legado, migrado));
        when(passwords.isHashed("senha-legada")).thenReturn(false);
        when(passwords.isHashed("$2a$hash-existente")).thenReturn(true);
        when(passwords.hash("senha-legada")).thenReturn("$2a$novo-hash");

        new LegacyPasswordMigrationRunner(repository, passwords, true).run(null);

        assertEquals("$2a$novo-hash", legado.getSenha());
        assertEquals("$2a$hash-existente", migrado.getSenha());
        verify(repository).saveAll(List.of(legado));
    }

    private Usuario usuario(String senha) {
        Usuario usuario = new Usuario();
        usuario.setSenha(senha);
        return usuario;
    }
}
