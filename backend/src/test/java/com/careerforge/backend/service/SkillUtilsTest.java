package com.careerforge.backend.service;

import com.careerforge.backend.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SkillUtilsTest {

    @Test
    void duplicatesIgnoringCaseAreRemoved() {
        Set<String> result = SkillUtils.clean(List.of("Java", "java", " JAVA "), 20);
        assertEquals(List.of("Java"), new ArrayList<>(result));
    }

    @Test
    void spacesAreTrimmedAndCollapsed() {
        Set<String> result = SkillUtils.clean(List.of("  Spring   Boot "), 20);
        assertEquals(List.of("Spring Boot"), new ArrayList<>(result));
    }

    @Test
    void blankAndNullEntriesAreDropped() {
        Set<String> result = SkillUtils.clean(Arrays.asList("a", null, "   ", "b"), 20);
        assertEquals(List.of("a", "b"), new ArrayList<>(result));
    }

    @Test
    void insertionOrderIsKept() {
        Set<String> result = SkillUtils.clean(List.of("React", "Java"), 20);
        assertEquals(List.of("React", "Java"), new ArrayList<>(result));
    }

    @Test
    void nullListGivesAnEmptySet() {
        assertTrue(SkillUtils.clean(null, 20).isEmpty());
    }

    @Test
    void moreSkillsThanAllowedIsRejected() {
        List<String> many = new ArrayList<>();
        for (int i = 0; i < 21; i++) many.add("skill" + i);
        assertThrows(ApiException.class, () -> SkillUtils.clean(many, 20));
    }
}