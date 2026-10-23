package com.careerforge.backend.service;

import com.careerforge.backend.Repository.JobApplicationRepository;
import com.careerforge.backend.Repository.StudentProfileRepository;
import com.careerforge.backend.Repository.UserRepository;
import com.careerforge.backend.dto.ProfileRequest;
import com.careerforge.backend.dto.ProfileResponse;
import com.careerforge.backend.entity.Role;
import com.careerforge.backend.entity.StudentProfile;
import com.careerforge.backend.entity.User;
import com.careerforge.backend.exception.ApiException;
import com.careerforge.backend.exception.UserNotFoundException;
import com.careerforge.backend.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ProfileService {
    private static final int MAX_SKILLS = 20;

    private final StudentProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final JobApplicationRepository applicationRepository;

    public ProfileService(StudentProfileRepository profileRepository, UserRepository userRepository,
                          JobApplicationRepository applicationRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
    }

    // ---------- the student's own profile ----------

    @Transactional(readOnly = true)
    public ProfileResponse getMine(AuthUser actor) {
        return load(findUser(actor.id()));
    }

    @Transactional
    public ProfileResponse saveMine(AuthUser actor, ProfileRequest req) {
        User user = findUser(actor.id());
        StudentProfile profile = profileRepository.findByUserId(user.getId()).orElseGet(() -> {
            StudentProfile created = new StudentProfile();
            created.setUser(user);
            return created;
        });

        profile.setCollege(blankToNull(req.college()));
        profile.setDegree(blankToNull(req.degree()));
        profile.setGraduationYear(req.graduationYear());
        profile.setCgpa(req.cgpa());
        profile.setAbout(blankToNull(req.about()));
        profile.setResumeUrl(blankToNull(req.resumeUrl()));
        profile.getSkills().clear();
        profile.getSkills().addAll(cleanSkills(req.skills()));
        profile.setUpdatedAt(Instant.now());

        return toResponse(user, profileRepository.save(profile));
    }

    // ---------- someone else viewing a student's profile ----------

    @Transactional(readOnly = true)
    public ProfileResponse getForViewer(AuthUser viewer, Long studentId) {
        User student = userRepository.findById(studentId)
                .filter(u -> u.getRole() == Role.STUDENT)
                .orElseThrow(this::notFound);

        boolean privileged = viewer.role() == Role.ADMIN || viewer.role() == Role.PLACEMENT_OFFICER;
        boolean recruiterWithApplicant = viewer.role() == Role.RECRUITER
                && applicationRepository.recruiterMayViewStudent(studentId, viewer.id());

        if (!privileged && !recruiterWithApplicant) {
            throw notFound();            // 404, so nobody can probe which profiles exist
        }
        return load(student);
    }

    // ---------- helpers ----------

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "Profile not found");
    }

    private ProfileResponse load(User user) {
        return profileRepository.findByUserId(user.getId())
                .map(p -> toResponse(user, p))
                .orElseGet(() -> new ProfileResponse(user.getId(), user.getName(), user.getEmail(),
                        null, null, null, null, null, null, List.of(), 0, null));
    }

    private ProfileResponse toResponse(User user, StudentProfile p) {
        List<String> skills = p.getSkills().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        return new ProfileResponse(user.getId(), user.getName(), user.getEmail(),
                p.getCollege(), p.getDegree(), p.getGraduationYear(), p.getCgpa(),
                p.getAbout(), p.getResumeUrl(), skills, completeness(p), p.getUpdatedAt());
    }

    // 7 things count: college, degree, graduation year, CGPA, about, resume link, at least one skill
    private int completeness(StudentProfile p) {
        int filled = 0;
        if (p.getCollege() != null) filled++;
        if (p.getDegree() != null) filled++;
        if (p.getGraduationYear() != null) filled++;
        if (p.getCgpa() != null) filled++;
        if (p.getAbout() != null) filled++;
        if (p.getResumeUrl() != null) filled++;
        if (!p.getSkills().isEmpty()) filled++;
        return Math.round(filled * 100f / 7);
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // trim, collapse spaces, drop empties, remove duplicates ignoring case ("Java" and "java" are one skill)
    private Set<String> cleanSkills(List<String> raw) {
        if (raw == null) return new LinkedHashSet<>();
        Map<String, String> unique = new LinkedHashMap<>();
        for (String skill : raw) {
            if (skill == null) continue;
            String cleaned = skill.trim().replaceAll("\\s+", " ");
            if (!cleaned.isEmpty()) unique.putIfAbsent(cleaned.toLowerCase(Locale.ROOT), cleaned);
        }
        if (unique.size() > MAX_SKILLS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "At most " + MAX_SKILLS + " skills");
        }
        return new LinkedHashSet<>(unique.values());
    }
}