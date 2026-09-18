package psg.facilitei.Services;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Repository.TrabalhadorRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AutenticacaoPrestadorTest {
    @Test
    void autenticaSomenteComCredenciaisDoPrestador() {
        TrabalhadorRepository repository = mock(TrabalhadorRepository.class);
        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setEmail("ana@example.com");
        trabalhador.setSenha("senha-correta");
        when(repository.findByEmail("ana@example.com")).thenReturn(Optional.of(trabalhador));
        AutenticacaoPrestador auth = new AutenticacaoPrestador(repository);

        assertSame(trabalhador, auth.autenticar(basic("ana@example.com:senha-correta")));
        assertThrows(ResponseStatusException.class,
                () -> auth.autenticar(basic("ana@example.com:errada")));
        assertThrows(ResponseStatusException.class,
                () -> auth.autenticar("Basic invalid-base64"));
        assertThrows(ResponseStatusException.class, () -> auth.autenticar(null));
    }

    @Test
    void autenticaComSessaoDoPrestadorSemExporSenha() {
        TrabalhadorRepository repository = mock(TrabalhadorRepository.class);
        HttpSession session = mock(HttpSession.class);
        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setId(42L);
        when(session.getAttribute("auth.role")).thenReturn("trabalhador");
        when(session.getAttribute("auth.userId")).thenReturn(42L);
        when(repository.findById(42L)).thenReturn(Optional.of(trabalhador));

        AutenticacaoPrestador auth = new AutenticacaoPrestador(repository);

        assertSame(trabalhador, auth.autenticar(null, session));
        verify(repository, never()).findByEmail(anyString());
    }

    private String basic(String credentials) {
        return "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
