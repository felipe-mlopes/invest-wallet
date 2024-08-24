package personal.investwallet.security;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import personal.investwallet.modules.user.UserEntity;
import personal.investwallet.modules.user.UserRepository;
import personal.investwallet.providers.JWTProvider;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    JWTProvider jwtProvider;

    @Autowired
    UserRepository userRepository;

    @SuppressWarnings("null")
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var token = this.recoverToken(request);
        var login = jwtProvider.validateToken(token);
        UUID id = UUID.fromString(login);

        if (login != null) {
            UserEntity user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User Not Found"));
            var authentication = new UsernamePasswordAuthenticationToken(user, null);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");

        if (authHeader == null)
            return null;

        return authHeader.replace("Bearer ", "");
    }

}
