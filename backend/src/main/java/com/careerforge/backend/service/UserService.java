package com.careerforge.backend.service;

import com.careerforge.backend.Repository.UserRepository;
import com.careerforge.backend.dto.AdminCreateUserRequest;
import com.careerforge.backend.dto.CreateUserRequest;
import com.careerforge.backend.dto.UserResponse;
import com.careerforge.backend.entity.Role;
import com.careerforge.backend.entity.User;
import com.careerforge.backend.exception.DuplicateEmailException;
import com.careerforge.backend.exception.ForbiddenOperationException;
import com.careerforge.backend.exception.UserNotFoundException;
import com.careerforge.backend.security.AuthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.careerforge.backend.dto.UserStatsResponse;
import com.careerforge.backend.Repository.JobRepository;
import org.springframework.http.HttpStatus;
import com.careerforge.backend.exception.ApiException;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JobRepository jobRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JobRepository jobRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jobRepository = jobRepository;
    }

    public UserResponse createUser(AdminCreateUserRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(email);
        }
        User saved = userRepository.save(new User(
                request.name().trim(), email, passwordEncoder.encode(request.password()), request.role()));
        return toResponse(saved);
    }

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    public Page<UserResponse> searchUsers(String name, Pageable pageable) {
        return userRepository.findByNameContainingIgnoreCase(name, pageable).map(this::toResponse);
    }

    public UserResponse getUserById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public UserResponse updateUser(Long id, CreateUserRequest request) {
        User user = findOrThrow(id);
        String email = normalize(request.getEmail());
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateEmailException(email);
        }
        user.setName(request.getName().trim());
        user.setEmail(email);
        return toResponse(userRepository.save(user));
    }

    public UserResponse changeRole(Long id, Role newRole, AuthUser actor) {
        if (actor.id().equals(id)) {
            throw new ForbiddenOperationException("You cannot change your own role");
        }
        User user = findOrThrow(id);
        user.setRole(newRole);
        return toResponse(userRepository.save(user));
    }

    public UserResponse setEnabled(Long id, boolean enabled, AuthUser actor) {
        if (actor.id().equals(id)) {
            throw new ForbiddenOperationException("You cannot disable your own account");
        }
        User user = findOrThrow(id);
        user.setEnabled(enabled);
        return toResponse(userRepository.save(user));
    }

    public void deleteUser(Long id, AuthUser actor) {
        if (actor.id().equals(id)) {
            throw new ForbiddenOperationException("You cannot delete your own account");
        }
        if (jobRepository.existsByPostedById(id)) {
    throw new ApiException(HttpStatus.CONFLICT,
            "This user has posted jobs. Disable the account instead, or delete their jobs first.");
}
        userRepository.delete(findOrThrow(id));
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(),u.isEnabled());
    }
    public UserStatsResponse getStats() {
    return new UserStatsResponse(
            userRepository.count(),
            userRepository.countByRole(Role.STUDENT),
            userRepository.countByRole(Role.RECRUITER),
            userRepository.countByRole(Role.PLACEMENT_OFFICER),
            userRepository.countByRole(Role.ADMIN),
            userRepository.countByEnabled(false));
}
}