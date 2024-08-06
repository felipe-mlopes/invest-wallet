package personal.investwallet.user;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import personal.investwallet.user.dto.UserCreateDto;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<UUID> createUser(@RequestBody @Valid UserCreateDto payload) {

        var userId = this.userService.createUser(payload);

        return ResponseEntity.ok(userId);
    }
}