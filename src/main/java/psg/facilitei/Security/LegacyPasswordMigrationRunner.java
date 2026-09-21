package psg.facilitei.Security;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import psg.facilitei.Entity.Usuario;
import psg.facilitei.Repository.UsuarioRepository;
import psg.facilitei.Services.PasswordHashService;

@Component
public class LegacyPasswordMigrationRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(LegacyPasswordMigrationRunner.class);

    private final UsuarioRepository usuarios;
    private final PasswordHashService passwords;
    private final boolean enabled;

    public LegacyPasswordMigrationRunner(UsuarioRepository usuarios,
                                         PasswordHashService passwords,
                                         @Value("${security.password-migration.enabled:true}") boolean enabled) {
        this.usuarios = usuarios;
        this.passwords = passwords;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) return;

        var migrated = new ArrayList<Usuario>();
        for (Usuario usuario : usuarios.findAll()) {
            if (!passwords.isHashed(usuario.getSenha())) {
                usuario.setSenha(passwords.hash(usuario.getSenha()));
                migrated.add(usuario);
            }
        }
        if (!migrated.isEmpty()) {
            usuarios.saveAll(migrated);
            log.info("Migração de senha concluída para {} conta(s).", migrated.size());
        }
    }
}
