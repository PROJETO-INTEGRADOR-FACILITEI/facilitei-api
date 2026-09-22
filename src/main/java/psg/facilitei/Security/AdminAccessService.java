package psg.facilitei.Security;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import psg.facilitei.Entity.Usuario;
import psg.facilitei.Repository.UsuarioRepository;

@Service
public class AdminAccessService {
    private final UsuarioRepository users;
    private final Set<String> adminEmails;

    public AdminAccessService(UsuarioRepository users,
                              @Value("${app.admin-emails:}") String configuredEmails) {
        this.users = users;
        this.adminEmails = new LinkedHashSet<>(Arrays.stream(configuredEmails.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList());
    }

    public boolean isAdminEmail(String email) {
        return email != null && adminEmails.contains(email.trim().toLowerCase(Locale.ROOT));
    }

    public boolean isCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    public List<Usuario> configuredAdmins() {
        return adminEmails.stream()
                .map(users::findByEmailIgnoreCase)
                .flatMap(java.util.Optional::stream)
                .toList();
    }
}
