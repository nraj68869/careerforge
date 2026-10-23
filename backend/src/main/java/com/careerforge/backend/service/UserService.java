package com.careerforge.backend.service;

import com.careerforge.backend.Repository.JobApplicationRepository;
import com.careerforge.backend.Repository.JobRepository;
import com.careerforge.backend.Repository.UserRepository;
import com.careerforge.backend.dto.AdminCreateUserRequest;
import com.careerforge.backend.dto.CreateUserRequest;
import com.careerforge.backend.dto.UserResponse;
import com.careerforge.backend.dto.UserStatsResponse;
import com.careerforge.backend.entity.ApprovalStatus;
import com.careerforge.backend.entity.Role;
import com.careerforge.backend.entity.User;
import com.careerforge.backend.exception.ApiException;
import com.careerforge.backend.exception.DuplicateEmailException;
import com.careerforge.backend.exception.ForbiddenOperationException;
import com.careerforge.backend.exception.UserNotFoundException;
import com.careerforge.backend.security.AuthUser;
import com.careerforge.backend.Repository.StudentProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;
    private final StudentProfileRepository profileRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JobRepository jobRepository, JobApplicationRepository applicationRepository,StudentProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.profileRepository = profileRepository;
    }

    public UserResponse createUser(AdminCreateUserRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new ForbiddenOperationException("There can be only one administrator. You cannot create another.");
        }
        String email = normalize(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(email);
        }
        // accounts created by the admin are approved right away
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
        if (newRole == Role.ADMIN) {
            throw new ForbiddenOperationException("There can be only one administrator. You cannot promote another user.");
        }
        User user = findOrThrow(id);
        if (user.getRole() == Role.ADMIN) {
            throw new ForbiddenOperationException("The administrator's role cannot be changed");
        }
        user.setRole(newRole);
        user.setApprovalStatus(ApprovalStatus.APPROVED);   // the admin chose this role on purpose
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
    @Transactional
    public void deleteUser(Long id, AuthUser actor) {
        if (actor.id().equals(id)) {
            throw new ForbiddenOperationException("You cannot delete your own account");
        }
        if (jobRepository.existsByPostedById(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This user has posted jobs. Disable the account instead, or delete their jobs first.");
        }
        if (applicationRepository.existsByStudentId(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This student has applications. Disable the account instead of deleting it.");
        }
        profileRepository.deleteByUserId(id);
        userRepository.delete(findOrThrow(id));
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

    // ---------- recruiter approval ----------

    @Transactional(readOnly = true)
    public List<UserResponse> pendingRecruiters() {
        return userRepository
                .findByRoleAndApprovalStatus(Role.RECRUITER, ApprovalStatus.PENDING,
                        PageRequest.of(0, 50, Sort.by("id")))
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public UserResponse reviewRecruiter(Long id, ApprovalStatus status) {
        if (status == ApprovalStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose APPROVED or REJECTED");
        }
        User user = findOrThrow(id);
        if (user.getRole() != Role.RECRUITER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only recruiter accounts need approval");
        }
        user.setApprovalStatus(status);
        User saved = userRepository.save(user);
        if (status == ApprovalStatus.REJECTED) {
            jobRepository.closeOpenJobsOf(id);   // scam jobs disappear from students' view at once
        }
        return toResponse(saved);
    }

    // ---------- helpers ----------

    private User findOrThrow(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.isEnabled(),
                u.getApprovalStatus());
    }
}