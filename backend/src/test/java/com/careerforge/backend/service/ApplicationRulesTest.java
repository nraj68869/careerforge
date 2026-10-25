package com.careerforge.backend.service;

import com.careerforge.backend.Repository.JobApplicationRepository;
import com.careerforge.backend.Repository.JobRepository;
import com.careerforge.backend.Repository.StudentProfileRepository;
import com.careerforge.backend.Repository.UserRepository;
import com.careerforge.backend.dto.ApplicationResponse;
import com.careerforge.backend.entity.*;
import com.careerforge.backend.exception.ApiException;
import com.careerforge.backend.security.AuthUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationRulesTest {
    private final JobApplicationRepository applications = mock(JobApplicationRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final ApplicationService service = new ApplicationService(applications, mock(JobRepository.class),
            mock(UserRepository.class), mock(StudentProfileRepository.class), new MatchingService(), notifications);

    private final User recruiter = user(10L, "Rita Recruiter", Role.RECRUITER);
    private final User otherRecruiter = user(11L, "Omar Other", Role.RECRUITER);
    private final User student = user(20L, "Sam Student", Role.STUDENT);
    private final User otherStudent = user(21L, "Nina Neighbour", Role.STUDENT);
    private final User officer = user(50L, "Olive Officer", Role.PLACEMENT_OFFICER);

    private User user(long id, String name, Role role) {
        User u = new User(name, name.toLowerCase().replace(" ", "") + "@test.com", "hash", role);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private AuthUser actor(User u) {
        return new AuthUser(u.getId(), u.getEmail(), u.getRole(), "jti", Instant.now().plusSeconds(60));
    }

    /** An application (id 1) posted by `recruiter`, belonging to `student`, in the given status. */
    private void givenApplication(ApplicationStatus status) {
        Job job = new Job();
        job.setTitle("Java Intern");
        job.setCompany("Acme");
        job.setPostedBy(recruiter);

        JobApplication application = new JobApplication();
        application.setJob(job);
        application.setStudent(student);
        application.setStatus(status);

        when(applications.findWithDetailsById(1L)).thenReturn(Optional.of(application));
        when(applications.save(any(JobApplication.class))).thenAnswer(call -> call.getArgument(0));
    }

    private HttpStatus statusOf(ApiException e) {
        return e.getStatus();
    }

    // ---------- recruiter / officer moves ----------

    @Test
    void recruiterCanShortlistAndTheStudentIsNotified() {
        givenApplication(ApplicationStatus.APPLIED);

        ApplicationResponse response = service.updateStatus(actor(recruiter), 1L, ApplicationStatus.SHORTLISTED);

        assertEquals(ApplicationStatus.SHORTLISTED, response.status());
        verify(notifications).send(eq(20L), eq(NotificationType.STATUS), anyString(), anyString(), eq("my-applications"));
    }

    @Test
    void stepsCannotBeSkipped() {
        givenApplication(ApplicationStatus.APPLIED);

        ApiException e = assertThrows(ApiException.class,
                () -> service.updateStatus(actor(recruiter), 1L, ApplicationStatus.SELECTED));

        assertEquals(HttpStatus.BAD_REQUEST, statusOf(e));
        verify(applications, never()).save(any());
    }

    @Test
    void aRecruiterCannotWithdrawOnTheStudentsBehalf() {
        givenApplication(ApplicationStatus.APPLIED);

        ApiException e = assertThrows(ApiException.class,
                () -> service.updateStatus(actor(recruiter), 1L, ApplicationStatus.WITHDRAWN));

        assertEquals(HttpStatus.BAD_REQUEST, statusOf(e));
    }

    @Test
    void selectedIsFinal() {
        givenApplication(ApplicationStatus.SELECTED);

        ApiException e = assertThrows(ApiException.class,
                () -> service.updateStatus(actor(recruiter), 1L, ApplicationStatus.REJECTED));

        assertEquals(HttpStatus.BAD_REQUEST, statusOf(e));
    }

    @Test
    void aRejectedApplicationCanBeReconsidered() {
        givenApplication(ApplicationStatus.REJECTED);

        ApplicationResponse response = service.updateStatus(actor(recruiter), 1L, ApplicationStatus.SHORTLISTED);

        assertEquals(ApplicationStatus.SHORTLISTED, response.status());
    }

    @Test
    void anotherRecruiterSeesNothing() {
        givenApplication(ApplicationStatus.APPLIED);

        ApiException e = assertThrows(ApiException.class,
                () -> service.updateStatus(actor(otherRecruiter), 1L, ApplicationStatus.SHORTLISTED));

        assertEquals(HttpStatus.NOT_FOUND, statusOf(e));
        verify(applications, never()).save(any());
    }

    @Test
    void anOfficerCanManageAnyApplication() {
        givenApplication(ApplicationStatus.APPLIED);

        ApplicationResponse response = service.updateStatus(actor(officer), 1L, ApplicationStatus.REJECTED);

        assertEquals(ApplicationStatus.REJECTED, response.status());
    }

    // ---------- student withdrawals ----------

    @Test
    void aStudentCanWithdrawTheirOwnApplicationAndTheRecruiterIsTold() {
        givenApplication(ApplicationStatus.APPLIED);

        ApplicationResponse response = service.withdraw(actor(student), 1L);

        assertEquals(ApplicationStatus.WITHDRAWN, response.status());
        verify(notifications).send(eq(10L), eq(NotificationType.APPLICATION), anyString(), anyString(), eq("my-jobs"));
    }

    @Test
    void aStudentCannotWithdrawSomeoneElsesApplication() {
        givenApplication(ApplicationStatus.APPLIED);

        ApiException e = assertThrows(ApiException.class, () -> service.withdraw(actor(otherStudent), 1L));

        assertEquals(HttpStatus.NOT_FOUND, statusOf(e));
    }

    @Test
    void aSelectedApplicationCannotBeWithdrawn() {
        givenApplication(ApplicationStatus.SELECTED);

        ApiException e = assertThrows(ApiException.class, () -> service.withdraw(actor(student), 1L));

        assertEquals(HttpStatus.CONFLICT, statusOf(e));
    }
}