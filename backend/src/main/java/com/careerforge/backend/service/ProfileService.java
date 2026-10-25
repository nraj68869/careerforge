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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;


import java.time.Instant;
import java.util.List;

@Service
public class ProfileService {
    private static final int MAX_SKILLS = 20;

    private final StudentProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final JobApplicationRepository applicationRepository;
    private final FileStorageService storage;

    public ProfileService(StudentProfileRepository profileRepository, UserRepository userRepository,
                          JobApplicationRepository applicationRepository, FileStorageService storage) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.storage = storage;
    }

    // ---------- the student's own profile ----------

    @Transactional(readOnly = true)
    public ProfileResponse getMine(AuthUser actor) {
        return load(findUser(actor.id()));
    }

    @Transactional
    public ProfileResponse saveMine(AuthUser actor, ProfileRequest req) {
        User user = findUser(actor.id());
        StudentProfile profile = profileFor(user);

        profile.setCollege(blankToNull(req.college()));
        profile.setDegree(blankToNull(req.degree()));
        profile.setGraduationYear(req.graduationYear());
        profile.setCgpa(req.cgpa());
        profile.setAbout(blankToNull(req.about()));
        profile.setResumeUrl(blankToNull(req.resumeUrl()));
        profile.getSkills().clear();
        profile.getSkills().addAll(SkillUtils.clean(req.skills(), MAX_SKILLS));
        profile.setUpdatedAt(Instant.now());

        return toResponse(user, profileRepository.save(profile));
    }

    // ---------- resume file ----------

    @Transactional
    public ProfileResponse uploadResume(AuthUser actor, MultipartFile file) {
        User user = findUser(actor.id());
        StudentProfile profile = profileFor(user);

        String newName = storage.storePdf(file);
        String oldName = profile.getResumeFile();
        profile.setResumeFile(newName);
        profile.setResumeOriginalName(storage.displayName(file));
        profile.setUpdatedAt(Instant.now());

        StudentProfile saved;
        try {
            saved = profileRepository.save(profile);
        } catch (RuntimeException e) {
            storage.deleteQuietly(newName);
            throw e;
        }
        deleteAfterCommit(oldName);
        return toResponse(user, saved);
    }

    @Transactional
    public ProfileResponse removeResume(AuthUser actor) {
        User user = findUser(actor.id());
        StudentProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        if (profile == null || profile.getResumeFile() == null) return load(user);

        deleteAfterCommit(profile.getResumeFile());
        profile.setResumeFile(null);
        profile.setResumeOriginalName(null);
        profile.setUpdatedAt(Instant.now());
        return toResponse(user, profileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public byte[] readMyResume(AuthUser actor) {
        return readFile(profileRepository.findByUserId(actor.id()).orElse(null));
    }

    // ---------- someone else viewing a student ----------

    @Transactional(readOnly = true)
    public ProfileResponse getForViewer(AuthUser viewer, Long studentId) {
        return load(authorizeViewer(viewer, studentId));
    }

    @Transactional(readOnly = true)
    public byte[] readResumeForViewer(AuthUser viewer, Long studentId) {
        User student = authorizeViewer(viewer, studentId);
        return readFile(profileRepository.findByUserId(student.getId()).orElse(null));
    }

    /** Used when an account is deleted: removes the profile row and the resume file. */
    @Transactional
    public void deleteFor(Long userId) {
        profileRepository.findByUserId(userId).ifPresent(profile -> {
            deleteAfterCommit(profile.getResumeFile());
            profileRepository.delete(profile);
        });
    }

    // ---------- rules and helpers ----------

    private User authorizeViewer(AuthUser viewer, Long studentId) {
        User student = userRepository.findById(studentId)
                .filter(u -> u.getRole() == Role.STUDENT)
                .orElseThrow(this::notFound);

        boolean privileged = viewer.role() == Role.ADMIN || viewer.role() == Role.PLACEMENT_OFFICER;
        boolean recruiterWithApplicant = viewer.role() == Role.RECRUITER
                && applicationRepository.recruiterMayViewStudent(studentId, viewer.id());

        if (!privileged && !recruiterWithApplicant) {
            throw notFound();            // 404, so nobody can probe which profiles exist
        }
        return student;
    }

    private byte[] readFile(StudentProfile profile) {
        if (profile == null || profile.getResumeFile() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Resume not found");
        }
        return storage.read(profile.getResumeFile());
    }

    private StudentProfile profileFor(User user) {
        return profileRepository.findByUserId(user.getId()).orElseGet(() -> {
            StudentProfile created = new StudentProfile();
            created.setUser(user);
            return created;
        });
    }

    // delete the old file only after the database change is safely committed
    private void deleteAfterCommit(String fileName) {
        if (fileName == null) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    storage.deleteQuietly(fileName);
                }
            });
        } else {
            storage.deleteQuietly(fileName);
        }
    }

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
                        null, null, null, null, null, null, false, null, List.of(), 0, null));
    }

    private ProfileResponse toResponse(User user, StudentProfile p) {
        List<String> skills = p.getSkills().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        return new ProfileResponse(user.getId(), user.getName(), user.getEmail(),
                p.getCollege(), p.getDegree(), p.getGraduationYear(), p.getCgpa(),
                p.getAbout(), p.getResumeUrl(), p.getResumeFile() != null, p.getResumeOriginalName(),
                skills, completeness(p), p.getUpdatedAt());
    }

    // 7 things count: college, degree, graduation year, CGPA, about, a resume (link OR PDF), a skill
    private int completeness(StudentProfile p) {
        int filled = 0;
        if (p.getCollege() != null) filled++;
        if (p.getDegree() != null) filled++;
        if (p.getGraduationYear() != null) filled++;
        if (p.getCgpa() != null) filled++;
        if (p.getAbout() != null) filled++;
        if (p.getResumeUrl() != null || p.getResumeFile() != null) filled++;
        if (!p.getSkills().isEmpty()) filled++;
        return Math.round(filled * 100f / 7);
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}