package psg.facilitei.Services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Repository.TrabalhadorRepository;

@Component
public class AutenticacaoPrestador {
    private final TrabalhadorRepository trabalhadores;

    public AutenticacaoPrestador(TrabalhadorRepository trabalhadores) {
        this.trabalhadores = trabalhadores;
    }

    public Trabalhador autenticar(String authorization) {
        if (authorization == null || !authorization.startsWith("Basic ")) {
            throw unauthorized();
        }
        try {
            String credentials = new String(Base64.getDecoder().decode(authorization.substring(6)),
                    StandardCharsets.UTF_8);
            int separator = credentials.indexOf(':');
            if (separator < 1) throw unauthorized();
            Trabalhador trabalhador = trabalhadores.findByEmail(credentials.substring(0, separator))
                    .orElseThrow(this::unauthorized);
            byte[] expected = trabalhador.getSenha().getBytes(StandardCharsets.UTF_8);
            byte[] actual = credentials.substring(separator + 1).getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(expected, actual)) throw unauthorized();
            return trabalhador;
        } catch (IllegalArgumentException ex) {
            throw unauthorized();
        }
    }

    public Trabalhador autenticar(String authorization, HttpSession session) {
        if (session != null && "trabalhador".equals(session.getAttribute("auth.role"))) {
            Object userId = session.getAttribute("auth.userId");
            if (userId instanceof Long id) {
                return trabalhadores.findById(id).orElseThrow(this::unauthorized);
            }
        }
        return autenticar(authorization);
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais do prestador inválidas.");
    }
}
