package com.ctags.security;

import com.ctags.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads "Authorization: Bearer <token>", loads the user and puts it in the
 * security context. The principal is the {@link com.ctags.model.User} entity,
 * with authority ROLE_ADMIN, ROLE_MANAGER or ROLE_EMPLOYEE.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR = "ctags.auth_error";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Long userId = jwtService.parseUserId(header.substring(7));
                userRepository.findById(userId).filter(u -> u.isActive()).ifPresentOrElse(user -> {
                    var auth = new UsernamePasswordAuthenticationToken(user, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }, () -> request.setAttribute(AUTH_ERROR, "User not found or inactive"));
            } catch (ExpiredJwtException e) {
                request.setAttribute(AUTH_ERROR, "Token expired");
            } catch (JwtException | IllegalArgumentException e) {
                request.setAttribute(AUTH_ERROR, "Invalid token");
            }
        }
        chain.doFilter(request, response);
    }
}
