package personal.investwallet.modules.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import personal.investwallet.modules.user.dto.CreateResponseDTO;
import personal.investwallet.modules.user.dto.TokenResponseDTO;
import personal.investwallet.modules.user.dto.UserCreateRequestDto;
import personal.investwallet.modules.user.dto.UserLoginRequestDto;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<CreateResponseDTO> create(@Valid @RequestBody UserCreateRequestDto payload) {

        var message = userService.createUser(payload);

        return ResponseEntity.created(null).body(new CreateResponseDTO(message));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody UserLoginRequestDto payload) {

        var token = userService.authUser(payload);

        return ResponseEntity.ok(new TokenResponseDTO(token));
    }

}