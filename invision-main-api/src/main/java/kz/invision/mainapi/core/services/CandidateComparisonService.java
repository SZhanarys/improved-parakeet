package kz.invision.mainapi.core.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CandidateComparisonService {

    private final ExternalAiCharacterizationService externalAiCharacterizationService;
    private final ObjectMapper objectMapper;

    public CandidateComparisonService(
            ExternalAiCharacterizationService externalAiCharacterizationService,
            ObjectMapper objectMapper
    ) {
        this.externalAiCharacterizationService = externalAiCharacterizationService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> compareCandidates(
            Map<String, Object> firstCandidate,
            Map<String, Object> secondCandidate
    ) {
        String aiResult = externalAiCharacterizationService.generateStructuredList(
                """
                You are a senior IT recruiter.
                Compare two candidates and return JSON only.
                Write all text in Russian.

                Output format:
                {
                  "summaryTitle": "string",
                  "summaryText": "string",
                  "recommendation": "string",
                  "decisionFactors": ["string"],
                  "firstCandidateStrengths": ["string"],
                  "secondCandidateStrengths": ["string"]
                }

                Rules:
                - Base the comparison only on provided data.
                - Be specific and practical.
                - Do not praise both candidates equally if one is clearly stronger.
                - Mention risks or missing evidence where relevant.
                - Keep each bullet concise.
                """,
                buildComparisonPrompt(firstCandidate, secondCandidate)
        );

        Map<String, Object> parsed = parseComparison(aiResult);
        if (!parsed.isEmpty()) {
            return parsed;
        }

        return buildFallbackComparison(firstCandidate, secondCandidate);
    }

    private String buildComparisonPrompt(
            Map<String, Object> firstCandidate,
            Map<String, Object> secondCandidate
    ) {
        return """
                Candidate A:
                %s

                Candidate B:
                %s
                """.formatted(
                safeJson(firstCandidate),
                safeJson(secondCandidate)
        );
    }

    private String safeJson(Map<String, Object> value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    private Map<String, Object> parseComparison(String aiResult) {
        if (aiResult == null || aiResult.isBlank()) {
            return Map.of();
        }

        try {
            JsonNode root = objectMapper.readTree(aiResult);
            String summaryTitle = root.path("summaryTitle").asText("");
            String summaryText = root.path("summaryText").asText("");
            String recommendation = root.path("recommendation").asText("");
            List<String> decisionFactors = readStringList(root.path("decisionFactors"));
            List<String> firstStrengths = readStringList(root.path("firstCandidateStrengths"));
            List<String> secondStrengths = readStringList(root.path("secondCandidateStrengths"));

            if (summaryTitle.isBlank() && summaryText.isBlank() && recommendation.isBlank()) {
                return Map.of();
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("summaryTitle", summaryTitle);
            result.put("summaryText", summaryText);
            result.put("recommendation", recommendation);
            result.put("decisionFactors", decisionFactors);
            result.put("firstCandidateStrengths", firstStrengths);
            result.put("secondCandidateStrengths", secondStrengths);
            return result;
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private List<String> readStringList(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            String value = item.asText("").trim();
            if (!value.isBlank()) {
                result.add(value);
            }
        }
        return result;
    }

    private Map<String, Object> buildFallbackComparison(
            Map<String, Object> firstCandidate,
            Map<String, Object> secondCandidate
    ) {
        int firstScore = parseInt(firstCandidate.get("aiScore"));
        int secondScore = parseInt(secondCandidate.get("aiScore"));
        String firstName = stringValue(firstCandidate.get("fullName"), "Кандидат 1");
        String secondName = stringValue(secondCandidate.get("fullName"), "Кандидат 2");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summaryTitle", buildSummaryTitle(firstName, secondName, firstScore, secondScore));
        result.put("summaryText", buildSummaryText(firstCandidate, secondCandidate, firstScore, secondScore));
        result.put("recommendation", buildRecommendation(firstName, secondName, firstScore, secondScore));
        result.put("decisionFactors", buildDecisionFactors(firstCandidate, secondCandidate));
        result.put("firstCandidateStrengths", buildStrengths(firstCandidate));
        result.put("secondCandidateStrengths", buildStrengths(secondCandidate));
        return result;
    }

    private String buildSummaryTitle(String firstName, String secondName, int firstScore, int secondScore) {
        if (firstScore == secondScore) {
            return "Профили выглядят сопоставимо";
        }
        return firstScore > secondScore
                ? "Предварительное преимущество у " + firstName
                : "Предварительное преимущество у " + secondName;
    }

    private String buildSummaryText(
            Map<String, Object> firstCandidate,
            Map<String, Object> secondCandidate,
            int firstScore,
            int secondScore
    ) {
        String leaderName = firstScore >= secondScore
                ? stringValue(firstCandidate.get("fullName"), "Первый кандидат")
                : stringValue(secondCandidate.get("fullName"), "Второй кандидат");
        String followerName = firstScore >= secondScore
                ? stringValue(secondCandidate.get("fullName"), "Второй кандидат")
                : stringValue(firstCandidate.get("fullName"), "Первый кандидат");
        int delta = Math.abs(firstScore - secondScore);

        if (delta == 0) {
            return "По текущим данным кандидаты идут близко друг к другу. Для решения стоит смотреть на конкретные проекты, глубину роли и подтвержденные результаты.";
        }

        return leaderName + " выглядит сильнее по текущему AI score и полноте профиля. Разница с " + followerName
                + " составляет " + delta + " баллов, но итоговое решение все равно стоит проверять по интервью и реальным кейсам.";
    }

    private String buildRecommendation(String firstName, String secondName, int firstScore, int secondScore) {
        if (firstScore == secondScore) {
            return "Откройте детали обоих кандидатов и сравните подтвержденные проекты, самостоятельность и бизнес-контекст задач.";
        }
        return firstScore > secondScore
                ? "Если нужен более сильный профиль по текущим данным, первым стоит рассматривать " + firstName + "."
                : "Если нужен более сильный профиль по текущим данным, первым стоит рассматривать " + secondName + ".";
    }

    private List<String> buildDecisionFactors(
            Map<String, Object> firstCandidate,
            Map<String, Object> secondCandidate
    ) {
        List<String> result = new ArrayList<>();
        result.add("Сравните разницу в AI score и качество описания опыта.");
        result.add("Проверьте, у кого стек и проекты ближе к целевой роли.");

        if (!stringValue(firstCandidate.get("aiVerdict"), "").isBlank()
                || !stringValue(secondCandidate.get("aiVerdict"), "").isBlank()) {
            result.add("Сопоставьте AI verdict с фактическими проектами и зоной ответственности.");
        }

        return result;
    }

    private List<String> buildStrengths(Map<String, Object> candidate) {
        List<String> result = new ArrayList<>();
        List<String> skills = stringList(candidate.get("skills"));
        List<String> projects = stringList(candidate.get("projects"));
        String experienceSummary = stringValue(candidate.get("experienceSummary"), "");

        if (!skills.isEmpty()) {
            result.add("Ключевой стек: " + String.join(", ", skills.stream().limit(4).toList()) + ".");
        }
        if (!projects.isEmpty()) {
            result.add("Есть проектный след: " + String.join("; ", projects.stream().limit(2).toList()) + ".");
        }
        if (!experienceSummary.isBlank()) {
            result.add("Опыт описан достаточно подробно для предварительной оценки.");
        }
        if (result.isEmpty()) {
            result.add("Профиль требует дополнительных данных для сильного сравнения.");
        }
        return result;
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (Object item : values) {
            if (item == null) {
                continue;
            }
            String text = item.toString().trim();
            if (!text.isBlank()) {
                result.add(text);
            }
        }
        return result;
    }

    private int parseInt(Object value) {
        if (value instanceof Integer intValue) {
            return intValue;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = value.toString().trim();
        return text.isBlank() ? fallback : text;
    }
}
