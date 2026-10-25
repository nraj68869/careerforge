package com.careerforge.backend.service;

import com.careerforge.backend.dto.MatchInfo;
import com.careerforge.backend.entity.Job;
import com.careerforge.backend.entity.StudentProfile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class MatchingService {

    // different spellings of the same skill count as equal
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("js", "javascript"), Map.entry("ecmascript", "javascript"),
            Map.entry("ts", "typescript"),
            Map.entry("reactjs", "react"), Map.entry("nodejs", "node"),
            Map.entry("expressjs", "express"), Map.entry("vuejs", "vue"),
            Map.entry("postgres", "postgresql"), Map.entry("psql", "postgresql"),
            Map.entry("mongo", "mongodb"), Map.entry("py", "python"),
            Map.entry("cpp", "c++"), Map.entry("k8s", "kubernetes"),
            Map.entry("springboot", "spring"), Map.entry("ml", "machinelearning"));

    /**
     * Score 0-100.
     *  - no minimum CGPA on the job: score = share of required skills the student has
     *  - minimum CGPA set: 80% skills + 20% CGPA (met = full, not met = 0, unknown = half)
     * Returns null when the job lists no required skills (nothing to compare).
     */
    public MatchInfo score(StudentProfile profile, Job job) {
        Set<String> required = job.getRequiredSkills();
        if (required == null || required.isEmpty()) return null;

        Set<String> studentKeys = new HashSet<>();
        if (profile != null) {
            for (String skill : profile.getSkills()) studentKeys.add(key(skill));
        }

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String skill : required) {
            (studentKeys.contains(key(skill)) ? matched : missing).add(skill);
        }
        matched.sort(String.CASE_INSENSITIVE_ORDER);
        missing.sort(String.CASE_INSENSITIVE_ORDER);

        double skillScore = (double) matched.size() / required.size();
        double total = skillScore;
        Boolean cgpaOk = null;

        if (job.getMinCgpa() != null) {
            BigDecimal cgpa = profile == null ? null : profile.getCgpa();
            double cgpaScore;
            if (cgpa == null) {
                cgpaScore = 0.5;
            } else {
                cgpaOk = cgpa.compareTo(job.getMinCgpa()) >= 0;
                cgpaScore = cgpaOk ? 1.0 : 0.0;
            }
            total = 0.8 * skillScore + 0.2 * cgpaScore;
        }
        return new MatchInfo((int) Math.round(total * 100), matched, missing, cgpaOk);
    }

    private String key(String skill) {
        String k = skill.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9+#]", "");
        return ALIASES.getOrDefault(k, k);
    }
}