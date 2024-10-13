package personal.investwallet.security;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import personal.investwallet.exceptions.ResourceNotFoundException;
import personal.investwallet.modules.user.UserEntity;
import personal.investwallet.modules.user.UserRepository;

@Component
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws ResourceNotFoundException {
        UserEntity user = this.repository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(),
                new ArrayList<>());
    }
}
