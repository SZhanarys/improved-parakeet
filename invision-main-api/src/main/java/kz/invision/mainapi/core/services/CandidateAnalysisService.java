package kz.invision.mainapi.core.services;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CandidateAnalysisService {

    public Map<String, Object> analyzeCandidate(String name, String skills, String experience) {
        String normalizedSkills = skills == null ? "" : skills.toLowerCase();
        boolean strongMatch = normalizedSkills.contains("java") || normalizedSkills.contains("flutter");
        int score = strongMatch ? 95 : 70;

        return Map.of(
                "score", score,
                "verdict", "Кандидат " + (name == null || name.isBlank() ? "без имени" : name)
                        + " успешно прошел первичный ИИ-анализ."
        );
    }
}
