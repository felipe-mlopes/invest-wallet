package personal.investwallet.modules.mailing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


@Service
public class EmailService {

    @Value("${MAIL_USERNAME}")
    private String from;

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private CacheManager cacheManager;

    private final Logger log = LogManager.getLogger();

    @Async
    public CompletableFuture<String> sendUserConfirmationEmail(String to) {

        String subject = "Validação do cadastro";
        String codeVerification = generateCodeVerification();
        String text= "Este é o código de verificação única solicitado: " + codeVerification;

        try {
            SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
            simpleMailMessage.setFrom(from);
            simpleMailMessage.setTo(to);
            simpleMailMessage.setSubject(subject);
            simpleMailMessage.setText(text);
            javaMailSender.send(simpleMailMessage);

            log.info("Email enviado ao %s com sucesso!".formatted(to));

            Objects.requireNonNull(cacheManager.getCache("verificationCodes")).put(to, codeVerification);

            return CompletableFuture.completedFuture("Sucesso! E-mail enviado para: " + to);

        } catch (Exception e) {
            throw new RuntimeException( "Erro ao tentar enviar email. " + e.getLocalizedMessage());
        }
    }

    private String generateCodeVerification() {

        return UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
