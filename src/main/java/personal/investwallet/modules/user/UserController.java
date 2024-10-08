package personal.investwallet.modules.user;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import personal.investwallet.modules.user.dto.CreateUserResponseDto;
import personal.investwallet.modules.user.dto.TokenResponseDto;
import personal.investwallet.modules.user.dto.UserCreateRequestDto;
import personal.investwallet.modules.user.dto.UserLoginRequestDto;
import personal.investwallet.security.TokenService;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    @PostMapping("/register")
    public ResponseEntity<CreateUserResponseDto> create(@Valid @RequestBody UserCreateRequestDto payload) {

        String result = userService.createUser(payload);

        return ResponseEntity.created(null).body(new CreateUserResponseDto(result));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@Valid @RequestBody UserLoginRequestDto payload, HttpServletResponse response) {

        String token = userService.authUser(payload, response);
        tokenService.addTokenToCookies(token, response);

        return ResponseEntity.ok(new TokenResponseDto(token));
    }

}