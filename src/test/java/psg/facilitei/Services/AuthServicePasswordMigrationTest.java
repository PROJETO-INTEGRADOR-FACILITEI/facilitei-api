package psg.facilitei.Services;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import psg.facilitei.DTO.ClienteResponseDTO;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.TrabalhadorRepository;
import psg.facilitei.Security.AdminAccessService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServicePasswordMigrationTest {
    @Mock ClienteRepository clientes;
    @Mock TrabalhadorRepository trabalhadores;
    @Mock ModelMapper modelMapper;
    @Mock PasswordHashService passwordHashService;
    @Mock AdminAccessService adminAccess;
    @InjectMocks AuthService service;

    @Test
    void loginValidoMigraSenhaLegadaParaBcrypt() {
        Cliente cliente = new Cliente();
        cliente.setEmail("cliente@example.com");
        cliente.setSenha("senha-legada-segura");
        ClienteResponseDTO response = new ClienteResponseDTO();

        when(clientes.findByEmail(cliente.getEmail())).thenReturn(Optional.of(cliente));
        when(passwordHashService.matches("senha-legada-segura", cliente.getSenha())).thenReturn(true);
        when(passwordHashService.isHashed(cliente.getSenha())).thenReturn(false);
        when(passwordHashService.hash("senha-legada-segura")).thenReturn("$2a$hash-migrado");
        when(modelMapper.map(cliente, ClienteResponseDTO.class)).thenReturn(response);
        when(adminAccess.isAdminEmail(cliente.getEmail())).thenReturn(true);

        var login = service.login(cliente.getEmail(), "senha-legada-segura");

        assertEquals("cliente", login.getRole());
        assertTrue(login.isAdmin());
        assertEquals("$2a$hash-migrado", cliente.getSenha());
        verify(clientes).save(cliente);
    }
}
