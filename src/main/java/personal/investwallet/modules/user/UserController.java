package personal.investwallet.modules.user;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import personal.investwallet.modules.mailing.EmailService;
import personal.investwallet.modules.user.dto.*;
import personal.investwallet.security.TokenService;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TokenService tokenService;

    @PostMapping("/register")
    public ResponseEntity<CreateUserResponseDto> create(@Valid @RequestBody UserCreateRequestDto payload) {

        String result = userService.createUser(payload);

        CompletableFuture<String> email = emailService.sendUserConfirmationEmail(payload.email());

        return ResponseEntity.created(null).body(new CreateUserResponseDto(result));
    }

    @PatchMapping("/validate")
    public ResponseEntity<ValidateUserRespondeDto> validate(@Valid @RequestBody UserValidateRequestDto payload) {

        String result = userService.validateUser(payload);

        return ResponseEntity.ok(new ValidateUserRespondeDto(result));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@Valid @RequestBody UserLoginRequestDto payload, HttpServletResponse response) {

        String token = userService.authUser(payload, response);
        tokenService.addTokenToCookies(token, response);

        return ResponseEntity.ok(new TokenResponseDto(token));
    }

}