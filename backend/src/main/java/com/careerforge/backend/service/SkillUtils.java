package com.careerforge.backend.service;

import com.careerforge.backend.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SkillUtils {
    private SkillUtils() {}

    
    public static Set<String> clean(List<String> raw, int max) {
        if (raw == null) return new LinkedHashSet<>();
        Map<String, String> unique = new LinkedHashMap<>();
        for (String skill : raw) {
            if (skill == null) continue;
            String cleaned = skill.trim().replaceAll("\\s+", " ");
            if (!cleaned.isEmpty()) unique.putIfAbsent(cleaned.toLowerCase(Locale.ROOT), cleaned);
        }
        if (unique.size() > max) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "At most " + max + " skills");
        }
        return new LinkedHashSet<>(unique.values());
    }
}