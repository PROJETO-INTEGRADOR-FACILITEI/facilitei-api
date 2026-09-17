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
