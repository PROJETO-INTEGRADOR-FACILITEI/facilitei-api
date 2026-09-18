package psg.facilitei.Controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import psg.facilitei.DTO.LoginResponseDTO;
import psg.facilitei.DTO.TrabalhadorResponseDTO;
import psg.facilitei.Exceptions.GlobalExceptionHandler;
import psg.facilitei.Services.AuthService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerSessionTest {
    private AuthService authService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        AuthController controller = new AuthController();
        ReflectionTestUtils.setField(controller, "authService", authService);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void loginCriaSessaoERestauraPrestadorSemReenviarSenha() throws Exception {
        TrabalhadorResponseDTO trabalhador = new TrabalhadorResponseDTO();
        trabalhador.setId("42");
        trabalhador.setNome("Ana");
        LoginResponseDTO response = new LoginResponseDTO("trabalhador", trabalhador);
        when(authService.login("ana@example.com", "senha-segura")).thenReturn(response);
        when(authService.restaurarSessao("trabalhador", 42L)).thenReturn(response);

        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@example.com\",\"senha\":\"senha-segura\"}"))
                .andExpect(status().isOk())
                .andExpect(request().sessionAttribute("auth.role", "trabalhador"))
                .andExpect(request().sessionAttribute("auth.userId", 42L))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/api/auth/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("trabalhador"))
                .andExpect(jsonPath("$.user.id").value("42"));
    }

    @Test
    void consultaSemSessaoRetornaUnauthorized() throws Exception {
        mvc.perform(get("/api/auth/session"))
                .andExpect(status().isUnauthorized());
    }
}
