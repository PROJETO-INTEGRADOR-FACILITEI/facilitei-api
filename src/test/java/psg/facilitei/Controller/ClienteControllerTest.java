package psg.facilitei.Controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import psg.facilitei.DTO.ClienteResponseDTO;
import psg.facilitei.Exceptions.GlobalExceptionHandler;
import psg.facilitei.Security.AccessControlService;
import psg.facilitei.Services.ClienteService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClienteControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ClienteService service = mock(ClienteService.class);
        ClienteController controller = new ClienteController();
        ReflectionTestUtils.setField(controller, "clienteService", service);
        ReflectionTestUtils.setField(controller, "access", mock(AccessControlService.class));
        when(service.update(anyLong(), any()))
                .thenReturn(ResponseEntity.ok(new ClienteResponseDTO()));

        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void editarPerfilSemAlterarSenhaAceitaPayloadDoFrontend() throws Exception {
        mvc.perform(put("/api/clientes/editar/23")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Cliente Atualizado",
                                  "email": "cliente@example.com",
                                  "telefone": "11999999999",
                                  "avatarUrl": "https://cdn.example.com/avatar.jpg",
                                  "endereco": {
                                    "rua": "Rua Um",
                                    "numero": "23",
                                    "bairro": "Centro",
                                    "cidade": "Sao Paulo",
                                    "estado": "SP",
                                    "cep": "01001-000"
                                  }
                                }
                                """))
                .andExpect(status().isOk());
    }
}
