package psg.facilitei.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.from:}") String from,
            @Value("${spring.mail.username:}") String username) {
        this.mailSender = mailSender;
        String configuredFrom = from == null ? "" : from.trim();
        this.from = configuredFrom.isBlank()
                ? (username == null ? "" : username.trim())
                : configuredFrom;
    }

    private SimpleMailMessage novaMensagem() {
        SimpleMailMessage message = new SimpleMailMessage();
        if (!from.isBlank()) {
            message.setFrom(from);
        }
        return message;
    }

    public void enviarEmailRecuperacaoSenha(String destinatario, String linkRecuperacao) {
        SimpleMailMessage message = novaMensagem();
        message.setTo(destinatario);
        message.setSubject("Facilitei - Recuperação de senha");
        message.setText("Você solicitou a recuperação de senha. Acesse o link abaixo para criar uma nova senha:\n\n"
                + linkRecuperacao
                + "\n\nSe você não solicitou isso, ignore este email. O link expira em 30 minutos.");
        mailSender.send(message);
    }

    @Async("notificationExecutor")
    public void enviarEmailDecisaoConta(String destinatario, boolean suspensa, String justificativa) {
        SimpleMailMessage message = novaMensagem();
        message.setTo(destinatario);
        message.setSubject(suspensa
                ? "Facilitei - Decisão sobre sua conta"
                : "Facilitei - Sua conta foi reativada");
        message.setText((suspensa
                ? "Após análise do suporte, sua conta foi suspensa."
                : "Após nova análise do suporte, sua conta foi reativada.")
                + "\n\nJustificativa: " + justificativa
                + "\n\nSe quiser contestar esta decisão, responda este e-mail ou escreva para contato@facilitei.online.");
        mailSender.send(message);
    }
}
