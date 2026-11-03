package psg.facilitei.Services;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmailServiceTest {

    @Test
    void usaRemetenteConfiguradoEmTodosOsEmails() {
        CapturingMailSender mailSender = new CapturingMailSender();
        EmailService service = new EmailService(
                mailSender,
                "contato@facilitei.online",
                "admin@facilitei.online");

        service.enviarEmailRecuperacaoSenha("cliente@example.com", "https://facilitei.online/redefinir");
        service.enviarEmailDecisaoConta("profissional@example.com", true, "Evidências analisadas");

        assertEquals(2, mailSender.messages.size());
        for (SimpleMailMessage message : mailSender.messages) {
            assertEquals("contato@facilitei.online", message.getFrom());
        }
    }

    @Test
    void usaUsuarioSmtpQuandoRemetenteEstaVazio() {
        CapturingMailSender mailSender = new CapturingMailSender();
        EmailService service = new EmailService(mailSender, "   ", "admin@facilitei.online");

        service.enviarEmailRecuperacaoSenha("cliente@example.com", "https://facilitei.online/redefinir");

        assertEquals("admin@facilitei.online", mailSender.messages.getFirst().getFrom());
    }

    private static final class CapturingMailSender extends JavaMailSenderImpl {
        private final List<SimpleMailMessage> messages = new ArrayList<>();

        @Override
        public void send(SimpleMailMessage simpleMessage) {
            messages.add(new SimpleMailMessage(simpleMessage));
        }
    }
}
