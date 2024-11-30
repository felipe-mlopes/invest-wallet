package personal.investwallet.modules.user;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import personal.investwallet.modules.mailing.EmailService;
import personal.investwallet.modules.user.dto.*;
import personal.investwallet.security.TokenService;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TokenService tokenService;

    @Operation(summary = "Registrar usuário", security = @SecurityRequirement(name = ""))
    @PostMapping("/register")
    public ResponseEntity<UserSuccessResponseDto> create(@Valid @RequestBody UserCreateRequestDto payload) {

        String result = userService.createUser(payload);

        emailService.sendUserConfirmationEmail(payload.email());

        return ResponseEntity.created(null).body(new UserSuccessResponseDto(result));
    }

    @Operation(summary = "Validar usuário", security = @SecurityRequirement(name = ""))
    @PatchMapping("/validate")
    public ResponseEntity<UserSuccessResponseDto> validate(@Valid @RequestBody UserValidateRequestDto payload) {

        String result = userService.validateUser(payload);

        return ResponseEntity.ok(new UserSuccessResponseDto(result));
    }

    @Operation(summary = "Atualizar código de validação de usuário", security = @SecurityRequirement(name = ""))
    @PostMapping("/revalidate")
    public ResponseEntity<UserSuccessResponseDto> revalidate(@Valid @RequestBody UserRevalidateRequestDto payload) {

        userService.verifyExistingUserAndVerificationCode(payload);

        emailService.sendUserConfirmationEmail(payload.email());

        return ResponseEntity.ok(new UserSuccessResponseDto("Código de confirmação reenviado"));
    }

    @Operation(summary = "Login do usuário", security = @SecurityRequirement(name = ""))
    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@Valid @RequestBody UserLoginRequestDto payload,
            HttpServletResponse response) {

        String token = userService.authUser(payload, response);
        tokenService.addTokenToCookies(token, response);

        return ResponseEntity.ok(new TokenResponseDto(token));
    }

}