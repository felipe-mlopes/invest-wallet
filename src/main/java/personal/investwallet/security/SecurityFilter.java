package personal.investwallet.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import personal.investwallet.exceptions.ResourceNotFoundException;
import personal.investwallet.exceptions.UnauthorizedException;
import personal.investwallet.modules.user.UserEntity;
import personal.investwallet.modules.user.UserRepository;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    TokenService tokenService;

    @Autowired
    UserRepository userRepository;

    private static final List<String> PUBLIC_URLS = Arrays.asList(
            "/user/register",
            "/user/validate",
            "/user/revalidate",
            "/user/login"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (isPublicUrl(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = recoverToken(request);

        if (token == null)
            throw new UnauthorizedException("Token não fornecido");

        String userId = tokenService.extractUserIdFromToken(token);

        if (userId != null)
            authenticateUser(userId);

        filterChain.doFilter(request, response);
    }

    private boolean isPublicUrl(String requestUri) {
        return PUBLIC_URLS.contains(requestUri);
    }

    private String recoverToken(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    private void authenticateUser(String userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Usuário não encontrado"));

        var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}