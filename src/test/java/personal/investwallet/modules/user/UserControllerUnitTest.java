package personal.investwallet.modules.user;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import personal.investwallet.modules.mailing.EmailService;
import personal.investwallet.modules.user.dto.*;
import personal.investwallet.security.TokenService;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class UserControllerUnitTest {

    @InjectMocks
    private UserController userController;

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    @Mock
    private TokenService tokenService;

    @Mock
    private HttpServletResponse httpServletResponse;

    private Validator validator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    class Create {

        @Test
        @DisplayName("Should be able to create new user with valid payload")
        void shouldBeAbleToCreateNewUserWithValidPayload() {

            UserCreateRequestDto payload = new UserCreateRequestDto(
                    "John Doe",
                    "test@example.com",
                    "Password123");

            ResponseEntity<UserSuccessResponseDto> response = userController.create(payload);

            assertNotNull(response);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());

            verify(userService, times(1)).createUser(payload);
            verify(emailService, times(1)).sendUserConfirmationEmail(payload.email());
        }

        @Test
        @DisplayName("Should not be able to create new user with empty payload")
        void shouldNotBeAbleToCreateNewUserWithEmptyPayload() {

            UserCreateRequestDto invalidPayload = new UserCreateRequestDto(
                    "",
                    "",
                    "");

            var violations = validator.validate(invalidPayload);

            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O nome não pode ser vazio")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O e-mail não pode ser vazio")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("A senha não pode ser vazia")));
        }

        @Test
        @DisplayName("Should not be able to create new user with invalid format payload")
        void shouldNotBeAbleToCreateNewUserWithInvalidFormatPayload() {

            UserCreateRequestDto invalidPayload = new UserCreateRequestDto(
                    "A",
                    "invalid-email",
                    "pass");

            var violations = validator.validate(invalidPayload);

            assertEquals(3, violations.size());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().equals("O nome deve ter entre 2 e 30 caracteres")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Formato inválido de email")));
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().equals("A senha deve ter entre 6 e 12 caracteres")));
        }
    }

    @Nested
    class Validate {

        @Test
        @DisplayName("Should be able to validate user with valid payload")
        void shouldBeAbleToValidateUserWithValidPayload() {

            UserValidateRequestDto payload = new UserValidateRequestDto(
                    "test@example.com",
                    "ABC4");

            ResponseEntity<UserSuccessResponseDto> response = userController.validate(payload);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());

            verify(userService, times(1)).validateUser(payload);
        }

        @Test
        @DisplayName("Should not be able to validate user with empty payload")
        void shouldNotBeAbleToValidateUserWithEmptyPayload() {

            UserValidateRequestDto invalidPayload = new UserValidateRequestDto(
                    "",
                    "");

            var violations = validator.validate(invalidPayload);

            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O e-mail não pode ser vazio")));
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().equals("O código de verificação não pode ser vazio")));
        }

        @Test
        @DisplayName("Should not be able to validate user with invalid format payload")
        void shouldNotBeAbleToValidateUserWithInvalidFormatPayload() {

            UserValidateRequestDto invalidPayload = new UserValidateRequestDto(
                    "invalid-email",
                    "password");

            var violations = validator.validate(invalidPayload);

            assertEquals(2, violations.size());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Formato inválido de email")));
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().equals("O código de verificação deve conter 4 dígitos")));
        }
    }

    @Nested
    class Revalidate {

        @Test
        @DisplayName("Should be able to revalidate user verification code with valid payload")
        void shouldBeAbleToRevalidateUserVerificationCodeWithValidPayload() {

            UserRevalidateRequestDto payload = new UserRevalidateRequestDto(
                    "test@example.com");

            ResponseEntity<UserSuccessResponseDto> response = userController.revalidate(payload);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());

            verify(userService, times(1)).verifyExistingUserAndVerificationCode(payload);
            verify(emailService, times(1)).sendUserConfirmationEmail(payload.email());
        }

        @Test
        @DisplayName("Should not be able to revalidate user verification code with empty email")
        void shouldNotBeAbleToRevalidateUserVerificationCodeWithEmptyEmail() {

            UserRevalidateRequestDto invalidPayload = new UserRevalidateRequestDto("");

            var violations = validator.validate(invalidPayload);

            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O e-mail não pode ser vazio")));
        }

        @Test
        @DisplayName("Should not be able to revalidate user verification code with invalid format email")
        void shouldNotBeAbleToRevalidateUserVerificationCodeWithInvalidFormatEmail() {

            UserRevalidateRequestDto invalidPayload = new UserRevalidateRequestDto("invalid-email");

            var violations = validator.validate(invalidPayload);

            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Formato inválido de email")));
        }
    }

    @Nested
    class Login {

        @Test
        @DisplayName("Should be able to user login with valid payload")
        void shouldBeAbleToUserLoginWithValidPayload() {

            UserLoginRequestDto payload = new UserLoginRequestDto(
                    "test@example.com",
                    "Password1234");

            ResponseEntity<TokenResponseDto> response = userController.login(payload, httpServletResponse);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());

            verify(userService, times(1)).authUser(payload, httpServletResponse);
            verify(tokenService, times(1)).addTokenToCookies(null, httpServletResponse);
        }

        @Test
        @DisplayName("Should not be able to user login with empty payload")
        void shouldNotBeAbleToUserLoginWithEmptyPayload() {

            UserLoginRequestDto invalidPayload = new UserLoginRequestDto(
                    "",
                    "");

            var violations = validator.validate(invalidPayload);

            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O e-mail não pode ser vazio")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("A senha não pode ser vazia")));
        }

        @Test
        @DisplayName("Should not be able to user login with invalid format payload")
        void shouldNotBeAbleToUserLoginWithInvalidFormatPayload() {

            UserLoginRequestDto invalidPayload = new UserLoginRequestDto(
                    "invalid-email",
                    "Password1234");

            var violations = validator.validate(invalidPayload);

            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Formato inválido de email")));
        }
    }
}
