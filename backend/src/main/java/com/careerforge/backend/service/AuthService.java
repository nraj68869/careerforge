package com.careerforge.backend.service;

import com.careerforge.backend.Repository.RevokedTokenRepository;
import com.careerforge.backend.Repository.UserRepository;
import com.careerforge.backend.dto.*;
import com.careerforge.backend.entity.RevokedToken;
import com.careerforge.backend.entity.Role;
import com.careerforge.backend.entity.User;
import com.careerforge.backend.exception.DuplicateEmailException;
import com.careerforge.backend.exception.ForbiddenOperationException;
import com.careerforge.backend.exception.InvalidCredentialsException;
import com.careerforge.backend.exception.UserNotFoundException;
import com.careerforge.backend.security.AuthUser;
import com.careerforge.backend.security.JwtService;
import com.careerforge.backend.security.LoginAttemptService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final String dummyHash;

    public AuthService(UserRepository userRepository, RevokedTokenRepository revokedTokenRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService,
                       LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
        // a fake hash, so unknown emails cost the same time as real ones (see login)
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    public UserResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(email);
        }
        User saved = userRepository.save(
                new User(req.name().trim(), email, passwordEncoder.encode(req.password()), Role.STUDENT));
        return toResponse(saved);
    }

    public AuthResponse login(LoginRequest req, String clientIp) {
        String email = req.email().trim().toLowerCase();
        String key = clientIp + "|" + email;

        loginAttemptService.checkAllowed(key);                       // locked? stop here

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        // ALWAYS run one password check, even for unknown emails, so response time reveals nothing
        String hashToCheck = user != null ? user.getPasswordHash() : dummyHash;
        boolean passwordOk = passwordEncoder.matches(req.password(), hashToCheck);

        if (user == null || !user.isEnabled() || !passwordOk) {
            loginAttemptService.recordFailure(key);
            throw new InvalidCredentialsException();
        }

        loginAttemptService.recordSuccess(key);
        return new AuthResponse(jwtService.generateToken(user), "Bearer",
                jwtService.expiresInSeconds(), toResponse(user));
    }

    public void logout(AuthUser current) {
        revokedTokenRepository.save(new RevokedToken(current.tokenId(), current.tokenExpiresAt()));
    }

    @Transactional
    public void changePassword(AuthUser current, ChangePasswordRequest req) {
        User user = userRepository.findById(current.id())
                .orElseThrow(() -> new UserNotFoundException(current.id()));

        String key = "pw|" + user.getId();                          // guessing the current password is limited too
        loginAttemptService.checkAllowed(key);
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(key);
            throw new ForbiddenOperationException("Current password is incorrect");
        }
        loginAttemptService.recordSuccess(key);

        if (passwordEncoder.matches(req.newPassword(), user.getPasswordHash())) {
            throw new ForbiddenOperationException("The new password must be different from the current one");
        }

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        // seconds precision, because JWT "issued at" has seconds precision too
        user.setPasswordChangedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        userRepository.save(user);

        // the current token is also cancelled explicitly (covers "changed in the same second")
        revokedTokenRepository.save(new RevokedToken(current.tokenId(), current.tokenExpiresAt()));
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.isEnabled());
    }
}