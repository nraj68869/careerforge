package com.careerforge.backend.security;

import com.careerforge.backend.Repository.RevokedTokenRepository;
import com.careerforge.backend.Repository.UserRepository;
import com.careerforge.backend.entity.User;
import io.jsonwebtoken.Claims;
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
public class JwtAuthFilter extends OncePerRequestFilter{
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RevokedTokenRepository revokedTokenRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository, RevokedTokenRepository revokedTokenRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.revokedTokenRepository = revokedTokenRepository;
    }
        @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain) throws ServletException,IOException {
                String header = request.getHeader("Authorization");
        
                if (header != null && header.startsWith("Bearer ")) {
                    try {
                        Claims claims = jwtService.parse(header.substring(7));
        
                        if (!revokedTokenRepository.existsById(claims.getId())) {
                    User user = userRepository.findById(Long.valueOf(claims.getSubject()))
                            .filter(User::isEnabled)
                            // NEW: a token issued BEFORE the last password change is dead
                            .filter(u -> u.getPasswordChangedAt() == null
                                    || !claims.getIssuedAt().toInstant().isBefore(u.getPasswordChangedAt()))
                            .orElse(null);        
                if (user != null) {
                        AuthUser principal = new AuthUser(user.getId(), user.getEmail(), user.getRole(),
                                claims.getId(), claims.getExpiration().toInstant());
                        var auth = new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                // fake, edited or expired token: stay "not logged in", the config below answers 401
            }
        }
        chain.doFilter(request, response);
    }
}
