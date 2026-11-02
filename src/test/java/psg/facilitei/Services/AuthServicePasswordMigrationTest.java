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
import psg.facilitei.Entity.Enum.AccountStatus;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.TrabalhadorRepository;
import psg.facilitei.Security.AdminAccessService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void loginRecusaContaSuspensaMesmoComSenhaCorreta() {
        Cliente cliente = new Cliente();
        cliente.setEmail("suspenso@example.com");
        cliente.setSenha("$2a$hash-valido");
        cliente.setAccountStatus(AccountStatus.SUSPENDED);

        when(clientes.findByEmail(cliente.getEmail())).thenReturn(Optional.of(cliente));
        when(passwordHashService.matches("senha-correta-123", cliente.getSenha())).thenReturn(true);

        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.login(cliente.getEmail(), "senha-correta-123"));

        assertEquals(403, error.getStatusCode().value());
        assertEquals("Conta suspensa. Entre em contato com o suporte.", error.getReason());
    }

    @Test
    void restauracaoDeSessaoRecusaContaSuspensa() {
        Cliente cliente = new Cliente();
        cliente.setId(18L);
        cliente.setEmail("suspenso@example.com");
        cliente.setAccountStatus(AccountStatus.SUSPENDED);
        when(clientes.findById(18L)).thenReturn(Optional.of(cliente));

        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.restaurarSessao("cliente", 18L));

        assertEquals(403, error.getStatusCode().value());
    }
}
