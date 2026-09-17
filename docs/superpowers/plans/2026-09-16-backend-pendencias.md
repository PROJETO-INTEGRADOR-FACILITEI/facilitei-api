# Backend Pendências Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close out the six backend items from the "Documentação de Pendências — Facilitei 28/08/2026" doc: password recovery, `AvaliacaoTrabalhadorController` architecture/DTO fixes, dead-comment cleanup in `ServicoController`, `StatusServico` enum documentation, and a real unit/integration test suite (H2-backed).

**Architecture:** Spring Boot 3.4.5 / Java 21 monolith, `Controller → Service → Repository` layering, ModelMapper for entity↔DTO mapping, `GlobalExceptionHandler` for error responses. Password reset follows the existing `AuthService` pattern (plaintext password compare/store — matching current login behavior, not introducing hashing since that's a separate, larger migration not in scope). Tests use JUnit 5 + Mockito for services, H2 in-memory DB + `@SpringBootTest`/`MockMvc` for integration.

**Tech Stack:** Spring Boot, Spring Data JPA, ModelMapper, Spring Mail (new), H2 (new, test scope), JUnit 5, Mockito.

## Global Constraints

- Package root: `psg.facilitei`. Follow existing casing/package conventions exactly.
- Do not touch any frontend code — this repo (`Facilitei-Api`) is backend-only; `LoginPage.tsx`/`api.ts` from the pendências doc live in a separate frontend repo not present here.
- Do not introduce Spring Security or password hashing — out of scope, would break existing plaintext login (`AuthService.java:33,42`).
- Keep `ddl-auto=update` compatible: never remove/rename existing enum constants or columns (avoids breaking persisted rows).
- New DB-backed features (password reset tokens) must go through Spring Data JPA, consistent with every other entity in the codebase.

---

### Task 1: Add Spring Mail + H2 test dependency

**Files:**
- Modify: `pom.xml`

**Steps:**
- [ ] Add `spring-boot-starter-mail` dependency (no version needed, managed by parent BOM):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```
- [ ] Add H2 as a test-scope dependency:
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```
- [ ] Run `mvn -q compile` to confirm the POM resolves.

---

### Task 2: Test profile configuration (H2)

**Files:**
- Create: `src/test/resources/application-test.properties`

**Content:**
```properties
spring.datasource.url=jdbc:h2:mem:faciliteidb;DB_CLOSE_DELAY=-1;MODE=MySQL
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
spring.flyway.enabled=false
spring.docker.compose.enabled=false
spring.mail.host=localhost
spring.mail.port=3025
app.reset-password-url=http://localhost:5173/reset-password
```
All integration tests (Task 12) activate this via `@ActiveProfiles("test")`.

---

### Task 3: `PasswordResetToken` entity + repositories

**Files:**
- Create: `src/main/java/psg/facilitei/Entity/PasswordResetToken.java`
- Create: `src/main/java/psg/facilitei/Repository/PasswordResetTokenRepository.java`
- Create: `src/main/java/psg/facilitei/Repository/UsuarioRepository.java`

**`PasswordResetToken.java`:**
```java
package psg.facilitei.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_token")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "data_expiracao", nullable = false)
    private LocalDateTime dataExpiracao;

    @Column(nullable = false)
    private boolean usado = false;
}
```

**`PasswordResetTokenRepository.java`:**
```java
package psg.facilitei.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import psg.facilitei.Entity.PasswordResetToken;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
}
```

**`UsuarioRepository.java`** (email lives on the shared `usuario` table via JOINED inheritance, so this repo can look up and persist Cliente/Trabalhador alike through the base entity):
```java
package psg.facilitei.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import psg.facilitei.Entity.Usuario;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
}
```

- [ ] Create the three files above.
- [ ] Run `mvn -q compile` to confirm they compile.

---

### Task 4: Forgot/reset password DTOs

**Files:**
- Create: `src/main/java/psg/facilitei/DTO/ForgotPasswordRequestDTO.java`
- Create: `src/main/java/psg/facilitei/DTO/ResetPasswordRequestDTO.java`

```java
package psg.facilitei.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequestDTO {
    @NotBlank(message = "O email é obrigatório")
    @Email(message = "Email inválido")
    private String email;
}
```

```java
package psg.facilitei.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequestDTO {
    @NotBlank(message = "O token é obrigatório")
    private String token;

    @NotBlank(message = "A nova senha é obrigatória")
    @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
    private String novaSenha;
}
```

- [ ] Create both files, `mvn -q compile`.

---

### Task 5: `EmailService`

**Files:**
- Create: `src/main/java/psg/facilitei/Services/EmailService.java`

```java
package psg.facilitei.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void enviarEmailRecuperacaoSenha(String destinatario, String linkRecuperacao) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destinatario);
        message.setSubject("Facilitei - Recuperação de senha");
        message.setText("Você solicitou a recuperação de senha. Acesse o link abaixo para criar uma nova senha:\n\n"
                + linkRecuperacao
                + "\n\nSe você não solicitou isso, ignore este email. O link expira em 30 minutos.");
        mailSender.send(message);
    }
}
```

- [ ] Create the file, `mvn -q compile`.

---

### Task 6: `PasswordResetService`

**Files:**
- Create: `src/main/java/psg/facilitei/Services/PasswordResetService.java`
- Test: `src/test/java/psg/facilitei/Services/PasswordResetServiceTest.java`

**`PasswordResetService.java`:**
```java
package psg.facilitei.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import psg.facilitei.Entity.PasswordResetToken;
import psg.facilitei.Entity.Usuario;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Repository.PasswordResetTokenRepository;
import psg.facilitei.Repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailService emailService;

    @Value("${app.reset-password-url}")
    private String resetPasswordUrl;

    private static final int EXPIRACAO_MINUTOS = 30;

    @Transactional
    public void solicitarRecuperacao(String email) {
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setUsuarioId(usuario.getId());
            resetToken.setDataExpiracao(LocalDateTime.now().plusMinutes(EXPIRACAO_MINUTOS));
            resetToken.setUsado(false);
            passwordResetTokenRepository.save(resetToken);

            String link = resetPasswordUrl + "?token=" + resetToken.getToken();
            emailService.enviarEmailRecuperacaoSenha(usuario.getEmail(), link);
        });
        // Resposta ao chamador é sempre genérica (ver AuthController) para não revelar se o email existe.
    }

    @Transactional
    public void redefinirSenha(String token, String novaSenha) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessRuleException("Token inválido."));

        if (resetToken.isUsado()) {
            throw new BusinessRuleException("Este token já foi utilizado.");
        }

        if (resetToken.getDataExpiracao().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Este token expirou. Solicite uma nova recuperação de senha.");
        }

        Usuario usuario = usuarioRepository.findById(resetToken.getUsuarioId())
                .orElseThrow(() -> new BusinessRuleException("Usuário não encontrado."));

        usuario.setSenha(novaSenha);
        usuarioRepository.save(usuario);

        resetToken.setUsado(true);
        passwordResetTokenRepository.save(resetToken);
    }
}
```

**`PasswordResetServiceTest.java`** (Mockito unit test):
```java
package psg.facilitei.Services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import psg.facilitei.Entity.PasswordResetToken;
import psg.facilitei.Entity.Usuario;
import psg.facilitei.Exceptions.BusinessRuleException;
import psg.facilitei.Repository.PasswordResetTokenRepository;
import psg.facilitei.Repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "resetPasswordUrl", "http://localhost:5173/reset-password");
    }

    @Test
    void solicitarRecuperacao_quandoUsuarioExiste_salvaTokenEEnviaEmail() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("teste@teste.com");
        when(usuarioRepository.findByEmail("teste@teste.com")).thenReturn(Optional.of(usuario));

        passwordResetService.solicitarRecuperacao("teste@teste.com");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getUsuarioId());
        assertFalse(captor.getValue().isUsado());
        assertTrue(captor.getValue().getDataExpiracao().isAfter(LocalDateTime.now()));

        verify(emailService).enviarEmailRecuperacaoSenha(eq("teste@teste.com"), anyString());
    }

    @Test
    void solicitarRecuperacao_quandoUsuarioNaoExiste_naoLancaExcecaoENaoEnviaEmail() {
        when(usuarioRepository.findByEmail("naoexiste@teste.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> passwordResetService.solicitarRecuperacao("naoexiste@teste.com"));

        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).enviarEmailRecuperacaoSenha(anyString(), anyString());
    }

    @Test
    void redefinirSenha_comTokenValido_atualizaSenhaEMarcaTokenComoUsado() {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(10L);
        token.setToken("abc123");
        token.setUsuarioId(1L);
        token.setUsado(false);
        token.setDataExpiracao(LocalDateTime.now().plusMinutes(10));

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setSenha("senhaAntiga");

        when(passwordResetTokenRepository.findByToken("abc123")).thenReturn(Optional.of(token));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        passwordResetService.redefinirSenha("abc123", "senhaNova");

        assertEquals("senhaNova", usuario.getSenha());
        assertTrue(token.isUsado());
        verify(usuarioRepository).save(usuario);
        verify(passwordResetTokenRepository).save(token);
    }

    @Test
    void redefinirSenha_comTokenInexistente_lancaBusinessRuleException() {
        when(passwordResetTokenRepository.findByToken("invalido")).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> passwordResetService.redefinirSenha("invalido", "senhaNova"));
    }

    @Test
    void redefinirSenha_comTokenExpirado_lancaBusinessRuleException() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("expirado");
        token.setUsuarioId(1L);
        token.setUsado(false);
        token.setDataExpiracao(LocalDateTime.now().minusMinutes(1));

        when(passwordResetTokenRepository.findByToken("expirado")).thenReturn(Optional.of(token));

        assertThrows(BusinessRuleException.class,
                () -> passwordResetService.redefinirSenha("expirado", "senhaNova"));
    }

    @Test
    void redefinirSenha_comTokenJaUsado_lancaBusinessRuleException() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("usado");
        token.setUsuarioId(1L);
        token.setUsado(true);
        token.setDataExpiracao(LocalDateTime.now().plusMinutes(10));

        when(passwordResetTokenRepository.findByToken("usado")).thenReturn(Optional.of(token));

        assertThrows(BusinessRuleException.class,
                () -> passwordResetService.redefinirSenha("usado", "senhaNova"));
    }
}
```
Note: add `import static org.mockito.ArgumentMatchers.eq;` alongside the other static imports.

- [ ] Create both files.
- [ ] Run `mvn -q test -Dtest=PasswordResetServiceTest`. Expected: all 6 tests PASS.

---

### Task 7: `AuthController` forgot/reset endpoints + mail config

**Files:**
- Modify: `src/main/java/psg/facilitei/Controller/AuthController.java`
- Modify: `src/main/resources/application.properties`

**AuthController.java** — add imports and two endpoints:
```java
import psg.facilitei.DTO.ForgotPasswordRequestDTO;
import psg.facilitei.DTO.ResetPasswordRequestDTO;
import psg.facilitei.Services.PasswordResetService;
```
```java
    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO dto) {
        passwordResetService.solicitarRecuperacao(dto.getEmail());
        return ResponseEntity.ok(Map.of("message",
                "Se o email informado estiver cadastrado, você receberá um link de recuperação."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO dto) {
        passwordResetService.redefinirSenha(dto.getToken(), dto.getNovaSenha());
        return ResponseEntity.ok(Map.of("message", "Senha redefinida com sucesso."));
    }
```

**application.properties** — append:
```properties
spring.mail.host=${MAIL_HOST:smtp.gmail.com}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME:}
spring.mail.password=${MAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

app.reset-password-url=${RESET_PASSWORD_URL:http://localhost:5173/reset-password}
```

- [ ] Apply both edits.
- [ ] Run `mvn -q compile`.

---

### Task 8: `AvaliacaoTrabalhadorResponseDTO` + ModelMapper wiring

**Files:**
- Create: `src/main/java/psg/facilitei/DTO/AvaliacaoTrabalhadorResponseDTO.java`
- Modify: `src/main/java/psg/facilitei/Config/ModelMapperConfig.java`

**`AvaliacaoTrabalhadorResponseDTO.java`:**
```java
package psg.facilitei.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class AvaliacaoTrabalhadorResponseDTO {
    private Long id;
    private Long trabalhadorId;
    private Long clienteId;
    private int nota;
    private String comentario;
    private Date data;
    private List<String> fotos;
}
```

**ModelMapperConfig.java** — add import `psg.facilitei.DTO.AvaliacaoTrabalhadorResponseDTO;` and `psg.facilitei.Entity.AvaliacaoTrabalhador;`, then inside the `modelMapper()` method (after the existing converters, before the `return modelMapper;`):
```java
                Converter<AvaliacaoTrabalhador, Long> avaliacaoTrabalhadorToTrabalhadorIdConverter = context -> context
                                .getSource() == null || context.getSource().getTrabalhador() == null
                                                ? null
                                                : context.getSource().getTrabalhador().getId();

                Converter<AvaliacaoTrabalhador, Long> avaliacaoTrabalhadorToClienteIdConverter = context -> context
                                .getSource() == null || context.getSource().getCliente() == null
                                                ? null
                                                : context.getSource().getCliente().getId();

                modelMapper.createTypeMap(AvaliacaoTrabalhador.class, AvaliacaoTrabalhadorResponseDTO.class)
                                .addMappings(mapper -> {
                                        mapper.using(avaliacaoTrabalhadorToTrabalhadorIdConverter).map(source -> source,
                                                        AvaliacaoTrabalhadorResponseDTO::setTrabalhadorId);
                                        mapper.using(avaliacaoTrabalhadorToClienteIdConverter).map(source -> source,
                                                        AvaliacaoTrabalhadorResponseDTO::setClienteId);
                                });
```

- [ ] Create the DTO, apply the config edit, `mvn -q compile`.

---

### Task 9: `AvaliacaoTrabalhadorRepository.findByTrabalhadorId`

**Files:**
- Modify: `src/main/java/psg/facilitei/Repository/AvaliacaoTrabalhadorRepository.java`

Add inside the interface (Spring Data derives the query — no `@Query` needed):
```java
    List<AvaliacaoTrabalhador> findByTrabalhadorId(Long trabalhadorId);
```

- [ ] Apply edit, `mvn -q compile`.

---

### Task 10: `AvaliacaoTrabalhadorService`

**Files:**
- Create: `src/main/java/psg/facilitei/Services/AvaliacaoTrabalhadorService.java`
- Test: `src/test/java/psg/facilitei/Services/AvaliacaoTrabalhadorServiceTest.java`

**`AvaliacaoTrabalhadorService.java`:**
```java
package psg.facilitei.Services;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import psg.facilitei.DTO.AvaliacaoTrabalhadorRequestDTO;
import psg.facilitei.DTO.AvaliacaoTrabalhadorResponseDTO;
import psg.facilitei.Entity.AvaliacaoTrabalhador;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Exceptions.ResourceNotFoundException;
import psg.facilitei.Repository.AvaliacaoTrabalhadorRepository;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.TrabalhadorRepository;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AvaliacaoTrabalhadorService {

    @Autowired
    private AvaliacaoTrabalhadorRepository repository;
    @Autowired
    private TrabalhadorRepository trabalhadorRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    public AvaliacaoTrabalhadorResponseDTO criar(AvaliacaoTrabalhadorRequestDTO dto) {
        Trabalhador trabalhador = trabalhadorRepository.findById(dto.getTrabalhadorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trabalhador não encontrado. ID: " + dto.getTrabalhadorId()));

        Cliente cliente = clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente não encontrado. ID: " + dto.getClienteId()));

        AvaliacaoTrabalhador avaliacao = new AvaliacaoTrabalhador();
        avaliacao.setTrabalhador(trabalhador);
        avaliacao.setCliente(cliente);
        avaliacao.setNota(dto.getNota());
        avaliacao.setComentario(dto.getComentario());
        avaliacao.setFotos(dto.getFotos());
        avaliacao.setData(new Date());

        AvaliacaoTrabalhador salvo = repository.save(avaliacao);
        return modelMapper.map(salvo, AvaliacaoTrabalhadorResponseDTO.class);
    }

    public List<AvaliacaoTrabalhadorResponseDTO> listarPorTrabalhador(Long trabalhadorId) {
        return repository.findByTrabalhadorId(trabalhadorId).stream()
                .map(avaliacao -> modelMapper.map(avaliacao, AvaliacaoTrabalhadorResponseDTO.class))
                .collect(Collectors.toList());
    }
}
```

**`AvaliacaoTrabalhadorServiceTest.java`:**
```java
package psg.facilitei.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import psg.facilitei.DTO.AvaliacaoTrabalhadorRequestDTO;
import psg.facilitei.DTO.AvaliacaoTrabalhadorResponseDTO;
import psg.facilitei.Entity.AvaliacaoTrabalhador;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Exceptions.ResourceNotFoundException;
import psg.facilitei.Repository.AvaliacaoTrabalhadorRepository;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.TrabalhadorRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvaliacaoTrabalhadorServiceTest {

    @Mock
    private AvaliacaoTrabalhadorRepository repository;
    @Mock
    private TrabalhadorRepository trabalhadorRepository;
    @Mock
    private ClienteRepository clienteRepository;

    private final ModelMapper modelMapper = new ModelMapper();

    @InjectMocks
    private AvaliacaoTrabalhadorService service;

    @Test
    void criar_comTrabalhadorEClienteExistentes_salvaEDevolveDTO() {
        service = new AvaliacaoTrabalhadorService();
        setField(service, "repository", repository);
        setField(service, "trabalhadorRepository", trabalhadorRepository);
        setField(service, "clienteRepository", clienteRepository);
        setField(service, "modelMapper", modelMapper);

        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setId(1L);
        Cliente cliente = new Cliente();
        cliente.setId(2L);

        AvaliacaoTrabalhadorRequestDTO dto = new AvaliacaoTrabalhadorRequestDTO();
        dto.setTrabalhadorId(1L);
        dto.setClienteId(2L);
        dto.setNota(5);
        dto.setComentario("Ótimo serviço");

        when(trabalhadorRepository.findById(1L)).thenReturn(Optional.of(trabalhador));
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(cliente));
        when(repository.save(any(AvaliacaoTrabalhador.class))).thenAnswer(invocation -> {
            AvaliacaoTrabalhador a = invocation.getArgument(0);
            a.setId(99L);
            return a;
        });

        AvaliacaoTrabalhadorResponseDTO result = service.criar(dto);

        assertEquals(99L, result.getId());
        assertEquals(1L, result.getTrabalhadorId());
        assertEquals(2L, result.getClienteId());
        assertEquals(5, result.getNota());
        assertEquals("Ótimo serviço", result.getComentario());
    }

    @Test
    void criar_comTrabalhadorInexistente_lancaResourceNotFoundException() {
        service = new AvaliacaoTrabalhadorService();
        setField(service, "trabalhadorRepository", trabalhadorRepository);

        AvaliacaoTrabalhadorRequestDTO dto = new AvaliacaoTrabalhadorRequestDTO();
        dto.setTrabalhadorId(1L);
        dto.setClienteId(2L);

        when(trabalhadorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.criar(dto));
    }

    @Test
    void listarPorTrabalhador_retornaListaMapeada() {
        service = new AvaliacaoTrabalhadorService();
        setField(service, "repository", repository);
        setField(service, "modelMapper", modelMapper);

        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setId(1L);
        Cliente cliente = new Cliente();
        cliente.setId(2L);

        AvaliacaoTrabalhador avaliacao = new AvaliacaoTrabalhador();
        avaliacao.setId(10L);
        avaliacao.setTrabalhador(trabalhador);
        avaliacao.setCliente(cliente);
        avaliacao.setNota(4);

        when(repository.findByTrabalhadorId(1L)).thenReturn(List.of(avaliacao));

        List<AvaliacaoTrabalhadorResponseDTO> result = service.listarPorTrabalhador(1L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals(1L, result.get(0).getTrabalhadorId());
        assertEquals(2L, result.get(0).getClienteId());
    }

    private static void setField(Object target, String field, Object value) {
        org.springframework.test.util.ReflectionTestUtils.setField(target, field, value);
    }
}
```
Note: this test builds the service manually via `setField`/`ReflectionTestUtils` per case (rather than relying on `@InjectMocks`) so the plain `ModelMapper` instance — not a mock — is wired in, since the mapping behavior itself is under test.

- [ ] Create both files.
- [ ] Run `mvn -q test -Dtest=AvaliacaoTrabalhadorServiceTest`. Expected: 3 tests PASS.

---

### Task 11: Refactor `AvaliacaoTrabalhadorController`

**Files:**
- Modify: `src/main/java/psg/facilitei/Controller/AvaliacaoTrabalhadorController.java`

Replace the entire file body:
```java
package psg.facilitei.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import psg.facilitei.DTO.AvaliacaoTrabalhadorRequestDTO;
import psg.facilitei.DTO.AvaliacaoTrabalhadorResponseDTO;
import psg.facilitei.Services.AvaliacaoTrabalhadorService;

import java.util.List;

@RestController
@RequestMapping("/api/avaliacoes-trabalhador")
public class AvaliacaoTrabalhadorController {

    @Autowired
    private AvaliacaoTrabalhadorService avaliacaoTrabalhadorService;

    @PostMapping
    public ResponseEntity<AvaliacaoTrabalhadorResponseDTO> criar(@RequestBody AvaliacaoTrabalhadorRequestDTO dto) {
        return ResponseEntity.ok(avaliacaoTrabalhadorService.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<AvaliacaoTrabalhadorResponseDTO>> listar(@RequestParam Long trabalhadorId) {
        return ResponseEntity.ok(avaliacaoTrabalhadorService.listarPorTrabalhador(trabalhadorId));
    }
}
```
This preserves the exact `/api/avaliacoes-trabalhador` URL and both method signatures (`criar`, `listar`) used by the frontend — only the internals move to the service layer and the return type becomes a DTO.

- [ ] Apply the replacement, `mvn -q compile`.

---

### Task 12: Clean obsolete comments in `ServicoController`

**Files:**
- Modify: `src/main/java/psg/facilitei/Controller/ServicoController.java`

- [ ] Remove these two comment lines above `listarPorCliente`:
```java
        // Você precisará criar este método no ServicoService também, chamando o
        // repository que editamos acima
```
- [ ] Remove these two comment lines inside `listar`, above the `trabalhadorId != null` branch:
```java
            // Você precisará criar findByTrabalhadorId no Repository e Service
            // Aqui estou simulando o retorno filtrado
```
(The methods themselves — `listarPorCliente`, `listar` — are already fully implemented and delegate to `ServicoService`; only the stale comments are removed, no behavior change.)

- [ ] `mvn -q compile`.

---

### Task 13: Document `StatusServico` enum

**Files:**
- Modify: `src/main/java/psg/facilitei/Entity/Enum/StatusServico.java`

The frontend (separate repo) currently only handles 7 of the 11 states. Rather than delete the other 4 (which would break `ddl-auto=update` for any already-persisted row using them, since removing a Java enum constant does not migrate existing DB string values), document the split so it's an explicit, intentional decision instead of dead-looking code:

```java
package psg.facilitei.Entity.Enum;

/**
 * Status do ciclo de vida de um {@link psg.facilitei.Entity.Servico}.
 *
 * O frontend atual (repositório separado) só trata explicitamente:
 * SOLICITADO, PENDENTE, EM_ANDAMENTO, PENDENTE_APROVACAO, FINALIZADO,
 * CANCELADO, RECUSADO.
 *
 * Os estados abaixo existem no backend mas ainda não têm tratamento visual
 * ou de transição no frontend. Mantidos (não removidos) para não quebrar
 * `ddl-auto=update` sobre linhas já persistidas com esses valores; ativar
 * o tratamento no frontend antes de considerá-los "prontos para uso".
 */
public enum StatusServico {
    SOLICITADO,        // Cliente solicitou o serviço
    AGUARDANDO_CONTATO, // Aguardando retorno do prestador — sem tratamento no frontend
    EM_ANALISE,         // Avaliando a viabilidade — sem tratamento no frontend
    APROVADO,           // Serviço aprovado e agendado — sem tratamento no frontend
    EM_ANDAMENTO,       // Serviço está sendo realizado
    PAUSADO,            // Serviço temporariamente interrompido — sem tratamento no frontend
    FINALIZADO,         // Serviço concluído
    CANCELADO,          // Serviço cancelado pelo cliente ou prestador
    NAO_COMPARECEU,     // Prestador ou cliente não compareceu — sem tratamento no frontend
    PENDENTE,
    PENDENTE_APROVACAO, // Trabalhador finalizou, aguardando cliente aprovar
    RECUSADO
}
```

- [ ] Apply edit, `mvn -q compile`.

---

### Task 14: `ServicoServiceTest`

**Files:**
- Test: `src/test/java/psg/facilitei/Services/ServicoServiceTest.java`

```java
package psg.facilitei.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import psg.facilitei.DTO.ServicoRequestDTO;
import psg.facilitei.DTO.ServicoResponseDTO;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Enum.StatusServico;
import psg.facilitei.Entity.Enum.TipoServico;
import psg.facilitei.Entity.Servico;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Exceptions.ResourceNotFoundException;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.ServicoRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicoServiceTest {

    @Mock
    private ServicoRepository servicoRepository;
    @Spy
    private ModelMapper modelMapper = new ModelMapper();
    @Mock
    private TrabalhadorService trabalhadorService;
    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ServicoService servicoService;

    @Test
    void buscarPorId_existente_retornaDTO() {
        Servico servico = new Servico();
        servico.setId(1L);
        servico.setTitulo("Reparo elétrico");
        servico.setDescricao("Troca de disjuntor");
        servico.setTipoServico(TipoServico.ELETRICISTA);
        servico.setStatusServico(StatusServico.PENDENTE);

        when(servicoRepository.findById(1L)).thenReturn(Optional.of(servico));

        ServicoResponseDTO dto = servicoService.buscarPorId(1L);

        assertEquals(1L, dto.getId());
        assertEquals("Reparo elétrico", dto.getTitulo());
    }

    @Test
    void buscarPorId_inexistente_lancaResourceNotFoundException() {
        when(servicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicoService.buscarPorId(99L));
    }

    @Test
    void criar_semStatusInformado_defineStatusComoPendente() {
        ServicoRequestDTO dto = new ServicoRequestDTO();
        dto.setTitulo("Pintura de sala");
        dto.setDescricao("Pintura completa");
        dto.setTipoServico(TipoServico.PINTOR);
        dto.setTrabalhadorId(1L);
        dto.setClienteId(2L);

        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setId(1L);
        Cliente cliente = new Cliente();
        cliente.setId(2L);

        when(trabalhadorService.buscarEntidadePorId(1L)).thenReturn(trabalhador);
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(cliente));
        when(servicoRepository.save(any(Servico.class))).thenAnswer(invocation -> {
            Servico s = invocation.getArgument(0);
            s.setId(5L);
            return s;
        });

        ServicoResponseDTO result = servicoService.criar(dto);

        assertEquals(StatusServico.PENDENTE.name(), result.getStatusServico());
    }

    @Test
    void deletar_inexistente_lancaResourceNotFoundException() {
        when(servicoRepository.existsById(42L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> servicoService.deletar(42L));
    }

    @Test
    void listarPorCliente_delegaParaRepository() {
        Servico servico = new Servico();
        servico.setId(1L);
        servico.setTipoServico(TipoServico.ENCANADOR);
        servico.setStatusServico(StatusServico.FINALIZADO);

        when(servicoRepository.findByClienteId(2L)).thenReturn(List.of(servico));

        List<ServicoResponseDTO> result = servicoService.listarPorCliente(2L);

        assertEquals(1, result.size());
    }
}
```

Check `TipoServico` enum values used above (`ELETRICISTA`, `PINTOR`, `ENCANADOR`) exist before running — read `src/main/java/psg/facilitei/Entity/Enum/TipoServico.java` first and swap in whatever constants actually exist if these don't match.

**Check `ServicoResponseDTO.getStatusServico()` return type** — read `src/main/java/psg/facilitei/DTO/ServicoResponseDTO.java` first; the assertion above assumes it returns a `String` (matching the `ModelMapperConfig` mapping `Servico::getStatusServico, ServicoResponseDTO::setStatusServico` into a DTO). If it's typed as `StatusServico` instead, change the assertion to `assertEquals(StatusServico.PENDENTE, result.getStatusServico())`.

- [ ] Create file (after verifying the two field/type notes above), run `mvn -q test -Dtest=ServicoServiceTest`. Expected: 5 tests PASS.

---

### Task 15: `SolicitacaoServicoServiceTest`

**Files:**
- Test: `src/test/java/psg/facilitei/Services/SolicitacaoServicoServiceTest.java`

```java
package psg.facilitei.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import psg.facilitei.DTO.SolicitacaoServicoRequestDTO;
import psg.facilitei.DTO.SolicitacaoServicoResponseDTO;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Enum.StatusSolicitacao;
import psg.facilitei.Entity.Enum.TipoServico;
import psg.facilitei.Entity.SolicitacaoServico;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Exceptions.ResourceNotFoundException;
import psg.facilitei.Repository.SolicitacaoServicoRepository;
import psg.facilitei.Repository.TrabalhadorRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitacaoServicoServiceTest {

    @Mock
    private SolicitacaoServicoRepository solicitacaoServicoRepository;
    @Mock
    private ClienteService clienteService;
    @Mock
    private TrabalhadorRepository trabalhadorRepository;
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private SolicitacaoServicoService solicitacaoServicoService;

    @Test
    void criar_semStatusInformado_defineStatusComoPendente() {
        SolicitacaoServicoRequestDTO dto = new SolicitacaoServicoRequestDTO();
        dto.setClienteId(1L);
        dto.setTrabalhadorId(2L);
        dto.setDescricao("Preciso de um eletricista");
        dto.setTipoServico(TipoServico.ELETRICISTA);

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setId(2L);

        when(clienteService.buscarEntidadePorId(1L)).thenReturn(cliente);
        when(trabalhadorRepository.findById(2L)).thenReturn(Optional.of(trabalhador));
        when(solicitacaoServicoRepository.save(any(SolicitacaoServico.class))).thenAnswer(invocation -> {
            SolicitacaoServico s = invocation.getArgument(0);
            s.setId(7L);
            return s;
        });

        SolicitacaoServicoResponseDTO result = solicitacaoServicoService.criar(dto);

        assertEquals(StatusSolicitacao.PENDENTE.name(), result.getStatus());
        assertEquals(1L, result.getClienteId());
        assertEquals(7L, result.getId());
    }

    @Test
    void criar_comTrabalhadorInexistente_lancaResourceNotFoundException() {
        SolicitacaoServicoRequestDTO dto = new SolicitacaoServicoRequestDTO();
        dto.setClienteId(1L);
        dto.setTrabalhadorId(99L);

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        when(clienteService.buscarEntidadePorId(1L)).thenReturn(cliente);
        when(trabalhadorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> solicitacaoServicoService.criar(dto));
    }

    @Test
    void buscarPorId_inexistente_lancaResourceNotFoundException() {
        when(solicitacaoServicoRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> solicitacaoServicoService.buscarPorId(5L));
    }

    @Test
    void deletar_inexistente_lancaResourceNotFoundException() {
        when(solicitacaoServicoRepository.existsById(3L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> solicitacaoServicoService.deletar(3L));
    }
}
```

Check `SolicitacaoServicoResponseDTO` field name for status (used above as `getStatus()`) and `SolicitacaoServicoRequestDTO` setters (`setClienteId`, `setTrabalhadorId`, `setDescricao`, `setTipoServico`) against the actual DTO source before running — read both DTO files first and adjust names if they differ.

- [ ] Create file (after the verification above), run `mvn -q test -Dtest=SolicitacaoServicoServiceTest`. Expected: 4 tests PASS.

---

### Task 16: `AvaliacaoServicoServiceTest`

**Files:**
- Test: `src/test/java/psg/facilitei/Services/AvaliacaoServicoServiceTest.java`

```java
package psg.facilitei.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import psg.facilitei.DTO.AvaliacaoServicoRequestDTO;
import psg.facilitei.DTO.AvaliacaoServicoResponseDTO;
import psg.facilitei.Entity.AvaliacaoServico;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Servico;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Repository.AvaliacaoServicoRepository;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.ServicoRepository;
import psg.facilitei.Repository.TrabalhadorRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvaliacaoServicoServiceTest {

    @Mock
    private AvaliacaoServicoRepository repository;
    @Mock
    private TrabalhadorRepository trabalhadorRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ServicoRepository servicoRepository;

    @InjectMocks
    private AvaliacaoServicoService avaliacaoServicoService;

    @Test
    void create_calculaERecalculaMediaDoTrabalhador() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setId(2L);
        Servico servico = new Servico();
        servico.setId(3L);
        servico.setTrabalhador(trabalhador);
        servico.setCliente(cliente);

        AvaliacaoServicoRequestDTO dto = new AvaliacaoServicoRequestDTO();
        dto.setClienteId(1L);
        dto.setServicoId(3L);
        dto.setNota(5);
        dto.setComentario("Excelente");

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(servicoRepository.findById(3L)).thenReturn(Optional.of(servico));
        when(repository.save(any(AvaliacaoServico.class))).thenAnswer(invocation -> {
            AvaliacaoServico a = invocation.getArgument(0);
            a.setId(9L);
            return a;
        });
        when(repository.calcularMediaPorTrabalhador(2L)).thenReturn(4.5);

        AvaliacaoServicoResponseDTO result = avaliacaoServicoService.create(dto);

        assertEquals(9L, result.getId());
        assertEquals(4.5, trabalhador.getNotaTrabalhador());
        verify(trabalhadorRepository).save(trabalhador);
    }

    @Test
    void buscarAvaliacoesPorServico_retornaListaMapeada() {
        Servico servico = new Servico();
        servico.setId(3L);
        Cliente cliente = new Cliente();
        cliente.setId(1L);

        AvaliacaoServico avaliacao = new AvaliacaoServico();
        avaliacao.setId(1L);
        avaliacao.setServico(servico);
        avaliacao.setCliente(cliente);
        avaliacao.setNota(4);

        when(repository.findByServicoId(3L)).thenReturn(List.of(avaliacao));

        List<AvaliacaoServicoResponseDTO> result = avaliacaoServicoService.buscarAvaliacoesPorServico(3L);

        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getServicoId());
    }

    private static void verify(Object mock) {
        org.mockito.Mockito.verify(mock);
    }
}
```

Fix the `verify(trabalhadorRepository).save(trabalhador)` line — remove the bogus local `verify(Object)` helper method at the bottom (it was a placeholder slip; Mockito's static `verify` is already available via the `org.mockito.Mockito.*` wildcard import at the top, so `import static org.mockito.Mockito.when;` needs `verify` added: `import static org.mockito.Mockito.*;`). Use `import static org.mockito.Mockito.*;` instead of the narrower `when`-only import, and delete the private `verify` method entirely before running.

- [ ] Create file with that fix applied, run `mvn -q test -Dtest=AvaliacaoServicoServiceTest`. Expected: 2 tests PASS.

---

### Task 17: `AvaliacaoClienteServiceTest`

**Files:**
- Test: `src/test/java/psg/facilitei/Services/AvaliacaoClienteServiceTest.java`

```java
package psg.facilitei.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import psg.facilitei.DTO.AvaliacaoClienteRequestDTO;
import psg.facilitei.DTO.AvaliacaoClienteResponseDTO;
import psg.facilitei.Entity.AvaliacaoCliente;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Exceptions.ResourceNotFoundException;
import psg.facilitei.Repository.AvaliacaoClienteRepository;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.TrabalhadorRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvaliacaoClienteServiceTest {

    @Mock
    private AvaliacaoClienteRepository avaliacaoClienteRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private TrabalhadorRepository trabalhadorRepository;

    @InjectMocks
    private AvaliacaoClienteService avaliacaoClienteService;

    @Test
    void criarAvaliacao_primeiraAvaliacao_mediaIgualNota() {
        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setId(1L);
        Cliente cliente = new Cliente();
        cliente.setId(2L);

        AvaliacaoClienteRequestDTO dto = new AvaliacaoClienteRequestDTO();
        dto.setTrabalhadorId(1L);
        dto.setClienteId(2L);
        dto.setNota(5);
        dto.setComentario("Cliente pontual");

        when(trabalhadorRepository.findById(1L)).thenReturn(Optional.of(trabalhador));
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(cliente));
        when(avaliacaoClienteRepository.findByClienteId(2L)).thenReturn(Collections.emptyList());
        when(avaliacaoClienteRepository.save(any(AvaliacaoCliente.class))).thenAnswer(invocation -> {
            AvaliacaoCliente a = invocation.getArgument(0);
            a.setId(11L);
            return a;
        });

        AvaliacaoClienteResponseDTO result = avaliacaoClienteService.criarAvaliacao(dto);

        assertEquals(5.0, result.getMediaCliente());
        assertEquals(5.0, cliente.getNotaCliente());
        verify(clienteRepository).save(cliente);
    }

    @Test
    void criarAvaliacao_comTrabalhadorInexistente_lancaResourceNotFoundException() {
        AvaliacaoClienteRequestDTO dto = new AvaliacaoClienteRequestDTO();
        dto.setTrabalhadorId(1L);
        dto.setClienteId(2L);

        when(trabalhadorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> avaliacaoClienteService.criarAvaliacao(dto));
    }

    @Test
    void deletarAvaliacao_inexistente_lancaResourceNotFoundException() {
        when(avaliacaoClienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> avaliacaoClienteService.deletarAvaliacao(99L));
    }

    @Test
    void listarPorCliente_retornaListaMapeada() {
        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setId(1L);
        Cliente cliente = new Cliente();
        cliente.setId(2L);

        AvaliacaoCliente avaliacao = new AvaliacaoCliente();
        avaliacao.setId(1L);
        avaliacao.setTrabalhador(trabalhador);
        avaliacao.setCliente(cliente);
        avaliacao.setNota(4);

        when(avaliacaoClienteRepository.findByClienteId(2L)).thenReturn(List.of(avaliacao));

        List<AvaliacaoClienteResponseDTO> result = avaliacaoClienteService.listarPorCliente(2L);

        assertEquals(1, result.size());
    }
}
```

- [ ] Create file, run `mvn -q test -Dtest=AvaliacaoClienteServiceTest`. Expected: 4 tests PASS.

---

### Task 18: `LiveChatControllerTest`

**Files:**
- Test: `src/test/java/psg/facilitei/Controller/LiveChatControllerTest.java`

No dedicated service class exists for chat — `LiveChatController` talks to `MensagemRepository` directly (both the WebSocket handler and the REST history endpoint). Test both methods directly against a mocked repository (no need for a full `@SpringBootTest`/WebSocket handshake):

```java
package psg.facilitei.Controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import psg.facilitei.Controller.domain.ChatInput;
import psg.facilitei.Entity.Mensagem;
import psg.facilitei.Repository.MensagemRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveChatControllerTest {

    @Mock
    private MensagemRepository mensagemRepository;

    @InjectMocks
    private LiveChatController liveChatController;

    @Test
    void sendMessage_persisteMensagemComServicoIdEDadosDoInput() {
        ChatInput input = new ChatInput("cliente1", "Olá, tudo bem?", "TEXTO", null);

        when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mensagem result = liveChatController.sendMessage(42L, input);

        ArgumentCaptor<Mensagem> captor = ArgumentCaptor.forClass(Mensagem.class);
        org.mockito.Mockito.verify(mensagemRepository).save(captor.capture());

        assertEquals(42L, captor.getValue().getServicoId());
        assertEquals("cliente1", captor.getValue().getRemetente());
        assertEquals("Olá, tudo bem?", captor.getValue().getConteudo());
        assertEquals("TEXTO", captor.getValue().getTipo());
        assertEquals(42L, result.getServicoId());
    }

    @Test
    void getHistorico_retornaMensagensOrdenadasDoRepository() {
        Mensagem msg = new Mensagem();
        msg.setId(1L);
        msg.setServicoId(7L);
        msg.setRemetente("trabalhador1");
        msg.setConteudo("Chego em 10 minutos");

        when(mensagemRepository.findByServicoIdOrderByDataEnvioAsc(7L)).thenReturn(List.of(msg));

        ResponseEntity<List<Mensagem>> response = liveChatController.getHistorico(7L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("Chego em 10 minutos", response.getBody().get(0).getConteudo());
    }
}
```

Read `src/main/java/psg/facilitei/Controller/domain/ChatInput.java` first to confirm it's a record with fields in the order `(user, message, type, fileUrl)` (as used by `sendMessage` in `LiveChatController.java:28`) — adjust the constructor call above if the field order differs.

- [ ] Create file (after verifying `ChatInput`'s field order), run `mvn -q test -Dtest=LiveChatControllerTest`. Expected: 2 tests PASS.

---

### Task 19: Integration test — contratação + avaliação flow

**Files:**
- Test: `src/test/java/psg/facilitei/Integration/ContratacaoAvaliacaoFlowIntegrationTest.java`

This is the "fluxo de contratação e avaliação" integration test called out in the pendency doc: create a cliente and trabalhador, open a serviço between them, and post an avaliação — all through the real REST layer against H2.

```java
package psg.facilitei.Integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import psg.facilitei.DTO.AvaliacaoServicoRequestDTO;
import psg.facilitei.DTO.ServicoRequestDTO;
import psg.facilitei.Entity.Cliente;
import psg.facilitei.Entity.Enum.TipoServico;
import psg.facilitei.Entity.Trabalhador;
import psg.facilitei.Repository.ClienteRepository;
import psg.facilitei.Repository.TrabalhadorRepository;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContratacaoAvaliacaoFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private TrabalhadorRepository trabalhadorRepository;

    @Test
    void fluxoCompleto_criarServicoEAvaliar() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setNome("Cliente Teste");
        cliente.setEmail("cliente.integ@teste.com");
        cliente.setSenha("123456");
        cliente = clienteRepository.save(cliente);

        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setNome("Trabalhador Teste");
        trabalhador.setEmail("trabalhador.integ@teste.com");
        trabalhador.setSenha("123456");
        trabalhador = trabalhadorRepository.save(trabalhador);

        ServicoRequestDTO servicoDto = new ServicoRequestDTO();
        servicoDto.setTitulo("Instalação elétrica");
        servicoDto.setDescricao("Instalar tomadas na cozinha");
        servicoDto.setTipoServico(TipoServico.ELETRICISTA);
        servicoDto.setTrabalhadorId(trabalhador.getId());
        servicoDto.setClienteId(cliente.getId());

        String servicoResponse = mockMvc.perform(post("/api/servicos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(servicoDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo", is("Instalação elétrica")))
                .andReturn().getResponse().getContentAsString();

        Long servicoId = objectMapper.readTree(servicoResponse).get("id").asLong();

        AvaliacaoServicoRequestDTO avaliacaoDto = new AvaliacaoServicoRequestDTO();
        avaliacaoDto.setClienteId(cliente.getId());
        avaliacaoDto.setServicoId(servicoId);
        avaliacaoDto.setNota(5);
        avaliacaoDto.setComentario("Serviço excelente, super recomendo");

        mockMvc.perform(post("/api/avaliacoes-servico")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(avaliacaoDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nota", is(5)));

        mockMvc.perform(get("/api/avaliacoes-servico/servico/" + servicoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].comentario", is("Serviço excelente, super recomendo")));
    }
}
```

Read `src/main/java/psg/facilitei/Controller/AvaliacaoServicoController.java` and `src/main/java/psg/facilitei/DTO/ServicoRequestDTO.java` first to confirm the exact request mapping paths (`POST /api/avaliacoes-servico`, `GET /api/avaliacoes-servico/servico/{id}`) and the setter names on `ServicoRequestDTO` — adjust the paths/setters above if they differ from what's assumed here.

- [ ] Read those two files to verify assumptions, create the test file (adjusting if needed), run `mvn -q test -Dtest=ContratacaoAvaliacaoFlowIntegrationTest`. Expected: 1 test PASS.

---

### Task 20: Full verification pass

- [ ] Run `mvn test` (whole suite). Expected: BUILD SUCCESS, all tests green — including the pre-existing `FaciliteiApplicationTests.contextLoads`.
- [ ] Run `mvn -q compile` one more time on its own to catch anything the test-only runs missed in `main`.
- [ ] Manually smoke-test forgot/reset-password with `mvn spring-boot:run` against local MySQL if available; otherwise confirm via the passing `PasswordResetServiceTest` that the token lifecycle logic is correct (real SMTP dependency is out of this repo's control per the pendency doc — "Requer configuração de servidor SMTP").
- [ ] Report final diff summary to the user: files created/modified, test count, and the one open dependency (real SMTP credentials must be set via `MAIL_USERNAME`/`MAIL_PASSWORD`/`MAIL_HOST` env vars before `forgot-password` can actually deliver email in production).

---

## Self-Review Notes

- **Coverage:** 2.1 → Tasks 1,3–7; 2.2/2.3 → Tasks 8–11; 2.4 → Task 12; 2.5 → Task 13; 2.6 → Tasks 2, 14–19.
- **Frontend items** (`LoginPage.tsx`, `api.ts`) are explicitly out of scope — not present in this repository.
- **Verification tasks** (14, 15, 16, 18, 19) call out exact fields to double-check against real source before writing the test, since some DTO/enum shapes were inferred from partial reads rather than full file contents — this replaces guessing with a read-then-adjust step so no test is written against a wrong signature.
