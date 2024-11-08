package personal.investwallet.modules.user;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import personal.investwallet.exceptions.ConflictException;
import personal.investwallet.exceptions.UnauthorizedException;
import personal.investwallet.modules.mailing.EmailService;
import personal.investwallet.modules.user.dto.*;
import personal.investwallet.security.TokenService;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

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
                    "Password123"
            );

            String message = "Usuário cadastrado com sucesso";

            when(userService.createUser(any(UserCreateRequestDto.class))).thenReturn(message);
            doNothing().when(emailService).sendUserConfirmationEmail(payload.email());

            ResponseEntity<CreateUserResponseDto> response = userController.create(payload);

            assertNotNull(response);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(message, response.getBody().message());

            verify(userService, times(1)).createUser(payload);
            verify(emailService, times(1)).sendUserConfirmationEmail(payload.email());
        }

        @Test
        @DisplayName("Should be able to create new user with email already exist")
        void shouldNotBeAbleToCreateNewUserWithEmailAlreadyExist() {

            UserCreateRequestDto payload = new UserCreateRequestDto(
                    "John Doe",
                    "test@example.com",
                    "Password123"
            );

            String message = "Usuário já existe";

            when(userService.createUser(any(UserCreateRequestDto.class))).thenThrow(
                    new ConflictException(message)
            );

            ConflictException exception = assertThrows(ConflictException.class,
                    () -> userController.create(payload));

            assertEquals(message, exception.getMessage());

            verify(userService, times(1)).createUser(payload);
        }

        @Test
        @DisplayName("Should not be able to create new user with empty payload")
        void shouldNotBeAbleToCreateNewUserWithEmptyPayload() {

            UserCreateRequestDto invalidPayload = new UserCreateRequestDto(
                    "",
                    "",
                    ""
            );

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
                    "pass"
            );

            var violations = validator.validate(invalidPayload);

            assertEquals(3, violations.size());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O nome deve ter entre 2 e 30 caracteres")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Formato inválido de email")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("A senha deve ter entre 6 e 12 caracteres")));
        }
    }

    @Nested
    class Validate {

        @Test
        @DisplayName("Should be able to validate user with valid payload")
        void shouldBeAbleToValidateUserWithValidPayload() {

            UserValidateRequestDto payload = new UserValidateRequestDto(
                    "test@example.com",
                    "ABC4"
            );

            String message = "Validação concluída com sucesso";

            when(userService.validateUser(any(UserValidateRequestDto.class))).thenReturn(message);

            ResponseEntity<ValidateUserRespondeDto> response = userController.validate(payload);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(message, response.getBody().message());

            verify(userService, times(1)).validateUser(payload);
        }

        @Test
        @DisplayName("Should not be able to validate user with invalid email")
        void shouldNotBeAbleToValidateUserWithInvalidEmail() {

            UserValidateRequestDto payload = new UserValidateRequestDto(
                    "invalid@example.com",
                    "ABC4"
            );

            String message = "Email inválido";

            when(userService.validateUser(any(UserValidateRequestDto.class))).thenThrow(
                    new UnauthorizedException(message)
            );

            UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                    () -> userController.validate(payload));

            assertEquals(message, exception.getMessage());

            verify(userService, times(1)).validateUser(payload);
        }

        @Test
        @DisplayName("Should not be able to validate user with different code")
        void shouldNotBeAbleToValidateUserWithDifferentCode() {

            UserValidateRequestDto payload = new UserValidateRequestDto(
                    "test@example.com",
                    "ABC4"
            );

            String message = "O código informado não confere";

            when(userService.validateUser(any(UserValidateRequestDto.class))).thenThrow(
                    new UnauthorizedException(message)
            );

            UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                    () -> userController.validate(payload));

            assertEquals(message, exception.getMessage());

            verify(userService, times(1)).validateUser(payload);
        }

        @Test
        @DisplayName("Should not be able to validate user with time code expired")
        void shouldNotBeAbleToValidateUserWithTimeCodeExpired() {

            UserValidateRequestDto payload = new UserValidateRequestDto(
                    "test@example.com",
                    "ABC4"
            );

            String message = "Tempo de validação expirado";

            when(userService.validateUser(any(UserValidateRequestDto.class))).thenThrow(
                    new UnauthorizedException(message)
            );

            UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                    () -> userController.validate(payload));

            assertEquals(message, exception.getMessage());

            verify(userService, times(1)).validateUser(payload);
        }

        @Test
        @DisplayName("Should not be able to validate user with empty payload")
        void shouldNotBeAbleToValidateUserWithEmptyPayload() {

            UserValidateRequestDto invalidPayload = new UserValidateRequestDto(
                    "",
                    ""
            );

            var violations = validator.validate(invalidPayload);

            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O e-mail não pode ser vazio")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O código de verificação não pode ser vazio")));
        }

        @Test
        @DisplayName("Should not be able to validate user with invalid format payload")
        void shouldNotBeAbleToValidateUserWithInvalidFormatPayload() {

            UserValidateRequestDto invalidPayload = new UserValidateRequestDto(
                    "invalid-email",
                    "password"
            );

            var violations = validator.validate(invalidPayload);

            assertEquals(2, violations.size());
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Formato inválido de email")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O código de verificação deve conter 4 dígitos")));
        }
    }

    @Nested
    class Revalidate {

        @Test
        @DisplayName("Should be able to revalidate user verification code with valid payload")
        void shouldBeAbleToRevalidateUserVerificationCodeWithValidPayload() {

            UserRevalidateRequestDto payload = new UserRevalidateRequestDto(
                    "test@example.com"
            );

            String message = "Código de confirmação reenviado";

            doNothing().when(userService).verifyExistingUserAndVerificationCode(any(UserRevalidateRequestDto.class));
            doNothing().when(emailService).sendUserConfirmationEmail(payload.email());

            ResponseEntity<RevalidateUserResponseDto> response = userController.revalidate(payload);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(message, response.getBody().message());

            verify(userService, times(1)).verifyExistingUserAndVerificationCode(payload);
            verify(emailService, times(1)).sendUserConfirmationEmail(payload.email());
        }

        @Test
        @DisplayName("Should not be able to revalidate user verification code with email is not exist")
        void shouldNotBeAbleToRevalidateUserVerificationCodeWithEmailIsNotExist() {

            UserRevalidateRequestDto payload = new UserRevalidateRequestDto(
                    "test@example.com"
            );

            String message = "Email inválido";

            doThrow(new UnauthorizedException(message)).when(userService).verifyExistingUserAndVerificationCode(any(UserRevalidateRequestDto.class));

            UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                    () -> userController.revalidate(payload));

            assertEquals(message, exception.getMessage());

            verify(userService, times(1)).verifyExistingUserAndVerificationCode(payload);
        }

        @Test
        @DisplayName("Should not be able to revalidate user verification code with user already checked")
        void shouldNotBeAbleToRevalidateUserVerificationCodeWithUserAlreadyChecked() {

            UserRevalidateRequestDto payload = new UserRevalidateRequestDto(
                    "test@example.com"
            );

            String message = "O cadastro do usuário já está válido";

            doThrow(new ConflictException(message)).when(userService).verifyExistingUserAndVerificationCode(any(UserRevalidateRequestDto.class));

            ConflictException exception = assertThrows(ConflictException.class,
                    () -> userController.revalidate(payload));

            assertEquals(message, exception.getMessage());

            verify(userService, times(1)).verifyExistingUserAndVerificationCode(payload);
        }

        @Test
        @DisplayName("Should not be able to revalidate user verification code when valid code already sent")
        void shouldNotBeAbleToRevalidateUserVerificationCodeWhenValidCodeAlreadySent() {

            UserRevalidateRequestDto payload = new UserRevalidateRequestDto(
                    "test@example.com"
            );

            String message = "O código de verificação enviado anteriormente ainda está válido";

            doThrow(new ConflictException(message)).when(userService).verifyExistingUserAndVerificationCode(any(UserRevalidateRequestDto.class));

            ConflictException exception = assertThrows(ConflictException.class,
                    () -> userController.revalidate(payload));

            assertEquals(message, exception.getMessage());

            verify(userService, times(1)).verifyExistingUserAndVerificationCode(payload);
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
                    "Password1234"
            );
            String expectedToken = "abc123";

            when(userService.authUser(payload, httpServletResponse)).thenReturn(expectedToken);
            doNothing().when(tokenService).addTokenToCookies(expectedToken, httpServletResponse);

            ResponseEntity<TokenResponseDto> response = userController.login(payload, httpServletResponse);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(expectedToken, response.getBody().token());

            verify(userService, times(1)).authUser(payload, httpServletResponse);
            verify(tokenService, times(1)).addTokenToCookies(expectedToken, httpServletResponse);
        }

        @Test
        @DisplayName("Should not be able to user login with invalid credentials")
        void shouldNotBeAbleToRevalidateUserVerificationCodeWithInvalidCredentials() {

            UserLoginRequestDto payload = new UserLoginRequestDto(
                    "test@example.com",
                    "Password1234"
            );

            String message = "Usuário e/ou senha inválidos.";

            when(userService.authUser(payload, httpServletResponse)).thenThrow(
                    new UnauthorizedException(message)
            );

            UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                    () -> userController.login(payload, httpServletResponse));

            assertEquals(message, exception.getMessage());

            verify(userService, times(1)).authUser(payload, httpServletResponse);
        }

        @Test
        @DisplayName("Should not be able to user login when user is not checked yet")
        void shouldNotBeAbleToRevalidateUserVerificationCodeWhenUserIsNotCheckedYet() {

            UserLoginRequestDto payload = new UserLoginRequestDto(
                    "test@example.com",
                    "Password1234"
            );

            String message = "Usuário não confirmou seu cadastro por e-mail";

            when(userService.authUser(payload, httpServletResponse)).thenThrow(
                    new UnauthorizedException(message)
            );

            UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                    () -> userController.login(payload, httpServletResponse));

            assertEquals(message, exception.getMessage());

            verify(userService, times(1)).authUser(payload, httpServletResponse);
        }

        @Test
        @DisplayName("Should not be able to user login with empty payload")
        void shouldNotBeAbleToUserLoginWithEmptyPayload() {

            UserLoginRequestDto invalidPayload = new UserLoginRequestDto(
                    "",
                    ""
            );

            var violations = validator.validate(invalidPayload);

            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O e-mail não pode ser vazio")));
            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("A senha não pode ser vazia")));
        }

        @Test
        @DisplayName("Should not be able to user login with invalid format payload")
        void shouldNotBeAbleToUserLoginWithInvalidFormatPayload() {

            UserLoginRequestDto invalidPayload = new UserLoginRequestDto(
                    "invalid-email",
                    "Password1234"
            );

            var violations = validator.validate(invalidPayload);

            assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("Formato inválido de email")));
        }
    }
}
