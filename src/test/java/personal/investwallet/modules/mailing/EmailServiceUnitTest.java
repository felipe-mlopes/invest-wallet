package personal.investwallet.modules.mailing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import personal.investwallet.exceptions.EmailSendException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class EmailServiceUnitTest {

    @Value("${MAIL_USERNAME}")
    private String from;

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private EmailService emailService;

    private final String TO = "test@example.com";

    @Nested
    class sendUserConfirmationEmail {

        @Test
        @DisplayName("Should be able to send user confirmation email and store code in cache")
        void shouldBeAbleToSendUserConfirmationEmailAndStoreCodeInCache() {

            String codePattern = "[A-Z0-9]{4}";

            when(cacheManager.getCache("verificationCodes")).thenReturn(cache);

            emailService.sendUserConfirmationEmail(TO);

            verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
            verify(cache, times(1)).put(eq(TO), matches(codePattern));
        }

        @Test
        @DisplayName("Should not be able to send user confirmation email when mail sender fails")
        void shouldNotBeAbleToSendUserConfirmationEmailWhenMailSenderFails() {

            doThrow(new RuntimeException("SMTP error")).when(javaMailSender).send(any(SimpleMailMessage.class));

            EmailSendException exception = assertThrows(EmailSendException.class, () -> {
                emailService.sendUserConfirmationEmail(TO);
            });
            assertEquals("Erro ao tentar enviar email. SMTP error", exception.getMessage());
        }

        @Test
        void shouldBeAbleToGenerateCodeVerification() throws Exception {

            Method method = EmailService.class.getDeclaredMethod("generateCodeVerification");
            method.setAccessible(true);
            String code = (String) method.invoke(emailService);

            assertNotNull(code);
            assertEquals(4, code.length());
            assertTrue(code.matches("[A-Z0-9]+"));
        }
    }
}
