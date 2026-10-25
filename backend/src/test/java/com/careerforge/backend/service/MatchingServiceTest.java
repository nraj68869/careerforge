package com.careerforge.backend.service;

import com.careerforge.backend.dto.MatchInfo;
import com.careerforge.backend.entity.Job;
import com.careerforge.backend.entity.StudentProfile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchingServiceTest {
    private final MatchingService service = new MatchingService();

    private Job job(String minCgpa, String... skills) {
        Job job = new Job();
        job.getRequiredSkills().addAll(List.of(skills));
        if (minCgpa != null) job.setMinCgpa(new BigDecimal(minCgpa));
        return job;
    }

    private StudentProfile student(String cgpa, String... skills) {
        StudentProfile profile = new StudentProfile();
        profile.getSkills().addAll(List.of(skills));
        if (cgpa != null) profile.setCgpa(new BigDecimal(cgpa));
        return profile;
    }

    @Test
    void jobWithoutRequiredSkillsHasNoScore() {
        assertNull(service.score(student("8", "Java"), job(null)));
    }

    @Test
    void everySkillMatchedGives100() {
        MatchInfo match = service.score(student("8", "java", "REACT"), job(null, "Java", "React"));
        assertEquals(100, match.score());
        assertTrue(match.missingSkills().isEmpty());
    }

    @Test
    void partialMatchListsMatchedAndMissingSkills() {
        MatchInfo match = service.score(student(null, "Java", "spring boot", "PostgreSQL"),
                job(null, "Java", "Spring Boot", "Docker"));
        assertEquals(67, match.score());
        assertEquals(List.of("Java", "Spring Boot"), match.matchedSkills());
        assertEquals(List.of("Docker"), match.missingSkills());
        assertNull(match.cgpaOk());
    }

    @Test
    void cgpaMetKeepsTheScoreHigh() {
        MatchInfo match = service.score(student("8.4", "Java", "spring boot", "PostgreSQL"),
                job("7.5", "Java", "Spring Boot", "Docker"));
        assertEquals(73, match.score());
        assertEquals(Boolean.TRUE, match.cgpaOk());
    }

    @Test
    void cgpaBelowTheMinimumCostsTwentyPoints() {
        MatchInfo match = service.score(student("6.0", "Java"), job("7.5", "Java"));
        assertEquals(80, match.score());
        assertEquals(Boolean.FALSE, match.cgpaOk());
    }

    @Test
    void unknownCgpaGetsHalfOfTheCgpaShare() {
        MatchInfo match = service.score(student(null, "Java"), job("7.5", "Java"));
        assertEquals(90, match.score());
        assertNull(match.cgpaOk());
    }

    @Test
    void differentSpellingsOfASkillAreTheSame() {
        MatchInfo match = service.score(student(null, "JS", "nodejs", "Postgres"),
                job(null, "JavaScript", "Node.js", "PostgreSQL"));
        assertEquals(100, match.score());
    }

    @Test
    void punctuationAndCaseAreIgnored() {
        MatchInfo match = service.score(student(null, "c++"), job(null, "C++"));
        assertEquals(100, match.score());
    }

    @Test
    void studentWithoutProfileScoresZero() {
        MatchInfo match = service.score(null, job(null, "Java"));
        assertEquals(0, match.score());
        assertEquals(List.of("Java"), match.missingSkills());
    }
}