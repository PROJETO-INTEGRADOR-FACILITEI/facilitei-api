package psg.facilitei.Services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordHashService {
    private final PasswordEncoder encoder;

    public PasswordHashService(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) return false;
        if (isHashed(storedPassword)) return encoder.matches(rawPassword, storedPassword);
        return MessageDigest.isEqual(rawPassword.getBytes(StandardCharsets.UTF_8),
                storedPassword.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isHashed(String password) {
        return password != null && (password.startsWith("$2a$")
                || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }
}
