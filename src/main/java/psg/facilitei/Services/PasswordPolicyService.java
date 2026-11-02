package psg.facilitei.Services;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import psg.facilitei.Exceptions.BusinessRuleException;

@Service
public class PasswordPolicyService {

    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH = 72;
    private static final String LENGTH_MESSAGE =
            "A senha deve ter entre 12 e 72 caracteres.";
    private static final String COMMON_MESSAGE =
            "Escolha uma senha menos comum e mais difícil de adivinhar.";

    private static final Set<String> BLOCKED_PASSWORDS = Stream.of(
            "123456789012",
            "abcdefgh1234",
            "admin1234567",
            "password1234",
            "qwerty123456",
            "qwertyuiop12",
            "senha12345678")
            .map(PasswordPolicyService::canonicalize)
            .collect(Collectors.toUnmodifiableSet());

    public void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            throw new BusinessRuleException(LENGTH_MESSAGE);
        }

        String canonical = canonicalize(password);
        if (BLOCKED_PASSWORDS.contains(canonical) || canonical.contains("facilitei")) {
            throw new BusinessRuleException(COMMON_MESSAGE);
        }
    }

    private static String canonicalize(String password) {
        String normalized = Normalizer.normalize(password, Normalizer.Form.NFKD)
                .toLowerCase(Locale.ROOT);
        StringBuilder canonical = new StringBuilder(normalized.length());

        for (int index = 0; index < normalized.length(); index++) {
            char character = normalized.charAt(index);
            if (Character.getType(character) == Character.NON_SPACING_MARK) {
                continue;
            }

            switch (character) {
                case '@', '4' -> canonical.append('a');
                case '3' -> canonical.append('e');
                case '1', '!' -> canonical.append('i');
                case '0' -> canonical.append('o');
                case '$', '5' -> canonical.append('s');
                case '7' -> canonical.append('t');
                default -> {
                    if (Character.isLetterOrDigit(character)) {
                        canonical.append(character);
                    }
                }
            }
        }

        return canonical.toString();
    }
}
