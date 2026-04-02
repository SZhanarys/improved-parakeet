package kz.invision.mainapi.core.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CandidateNormalizationService {

    private static final Pattern LIST_SPLIT_PATTERN = Pattern.compile("[,;\\n\\r]+");
    private static final Pattern GENERIC_SKILL_PHRASES = Pattern.compile(
            "(?iu)\\b(?:"
                    + "\\u0447\\u0442\\u043e\\s+\\u0443\\u043c\\u0435\\u0435\\u0442(?:\\s+\\u043a\\u0430\\u043d\\u0434\\u0438\\u0434\\u0430\\u0442)?"
                    + "|\\u043d\\u0430\\u0432\\u044b\\u043a\\u0438"
                    + "|\\u0432\\u043b\\u0430\\u0434\\u0435\\u044e"
                    + "|\\u0432\\u043b\\u0430\\u0434\\u0435\\u043d\\u0438\\u0435"
                    + "|\\u0443\\u043c\\u0435\\u044e"
                    + "|\\u043c\\u043e\\u0439\\s+\\u0441\\u0442\\u0435\\u043a"
                    + "|\\u0441\\u0442\\u0435\\u043a"
                    + "|\\u0442\\u0435\\u0445\\u043d\\u043e\\u043b\\u043e\\u0433\\u0438\\u0438"
                    + "|skills?"
                    + "|skillset"
                    + "|stack"
                    + ")\\s*:?\\s*"
    );
    private static final Pattern GENERIC_PROJECT_PHRASES = Pattern.compile(
            "(?iu)^(?:"
                    + "\\u0440\\u0430\\u0437\\u0440\\u0430\\u0431\\u0430\\u0442\\u044b\\u0432\\u0430\\u043b(?:\\u0430)?\\s+\\u043f\\u0440\\u043e\\u0435\\u043a\\u0442"
                    + "|\\u0443\\u0447\\u0430\\u0441\\u0442\\u0432\\u043e\\u0432\\u0430\\u043b(?:\\u0430)?\\s+\\u0432\\s+\\u043f\\u0440\\u043e\\u0435\\u043a\\u0442\\u0435"
                    + "|\\u043f\\u0440\\u043e\\u0435\\u043a\\u0442"
                    + ")\\s*[:\\-]?\\s*"
    );
    private static final Map<String, String> SKILL_DICTIONARY = Map.ofEntries(
            Map.entry("flutter", "Flutter"),
            Map.entry("dart", "Dart"),
            Map.entry("java", "Java"),
            Map.entry("spring", "Spring"),
            Map.entry("spring boot", "Spring Boot"),
            Map.entry("postgres", "PostgreSQL"),
            Map.entry("postgresql", "PostgreSQL"),
            Map.entry("postgre sql", "PostgreSQL"),
            Map.entry("sql", "SQL"),
            Map.entry("rest api", "REST API"),
            Map.entry("rest", "REST"),
            Map.entry("solid", "SOLID"),
            Map.entry("git", "Git"),
            Map.entry("docker", "Docker"),
            Map.entry("kubernetes", "Kubernetes"),
            Map.entry("firebase", "Firebase"),
            Map.entry("ооп", "ООП"),
            Map.entry("архитектура", "Архитектура")
    );

    private final ExternalAiCharacterizationService externalAiCharacterizationService;
    private final ObjectMapper objectMapper;

    public CandidateNormalizationService(
            ExternalAiCharacterizationService externalAiCharacterizationService,
            ObjectMapper objectMapper
    ) {
        this.externalAiCharacterizationService = externalAiCharacterizationService;
        this.objectMapper = objectMapper;
    }

    public List<String> normalizeSkills(String rawSkillsInput) {
        String aiResult = externalAiCharacterizationService.generateStructuredList(
                """
                You are an IT recruiting analyst.
                Normalize candidate skills from raw input and answer in JSON only.

                Rules:
                - Input may contain no commas, broken grammar, first-person wording, and spelling mistakes.
                - Remove phrases like "Что умеет", "Навыки", "Владею", "Умею", "Стек", "Я работал с", "Использовал".
                - Keep only technologies, tools, frameworks, databases, methods, and engineering competencies.
                - Fix spelling mistakes and normalize technology names.
                - Split merged text into separate skills even when separators are weak or missing.
                - Do not keep filler text, roles, or full narrative sentences.

                Output format:
                {"items":["Flutter","Java","PostgreSQL"]}
                """,
                safeValue(rawSkillsInput)
        );

        List<String> parsed = parseAiList(aiResult);
        if (!parsed.isEmpty()) {
            return parsed;
        }

        return normalizeSkillsFallback(rawSkillsInput);
    }

    public List<String> normalizeProjects(String rawProjectsInput) {
        String aiResult = externalAiCharacterizationService.generateStructuredList(
                """
                You are an IT recruiting analyst.
                Normalize candidate projects and answer in JSON only.

                Rules:
                - Input may be noisy, in first person, with typos and weak punctuation.
                - Keep project essence and candidate role or contribution.
                - Rewrite each item in concise professional Russian.
                - Remove filler phrases like "разрабатывал проект", "участвовал в проекте", "я делал".
                - Fix spelling mistakes and grammar.
                - Each item should read like a short recruiter-ready project description.

                Output format:
                {"items":["Мобильное приложение для банка, Flutter-разработчик"]}
                """,
                safeValue(rawProjectsInput)
        );

        List<String> parsed = parseAiList(aiResult);
        if (!parsed.isEmpty()) {
            return parsed;
        }

        return normalizeProjectsFallback(rawProjectsInput);
    }

    public String polishValueStatement(String rawValueStatement, List<String> skills, List<String> projects) {
        String aiResult = externalAiCharacterizationService.generateStructuredList(
                """
                You are an IT recruiting analyst and editor.
                Rewrite the candidate value statement in Russian.
                Correct spelling, punctuation, and grammar.
                Rewrite first-person or casual wording into professional recruiter-ready wording.
                Preserve meaning, remove filler, and keep it concise.
                Prefer neutral or third-person wording such as "Кандидат..." when useful.
                Return plain text only.
                """,
                """
                Raw value statement:
                %s

                Normalized skills:
                %s

                Normalized projects:
                %s
                """.formatted(
                        safeValue(rawValueStatement),
                        String.join(", ", safeList(skills)),
                        String.join(", ", safeList(projects))
                )
        );

        if (aiResult != null && !aiResult.isBlank()) {
            return cleanupParagraph(aiResult);
        }

        return polishValueStatementFallback(rawValueStatement, skills);
    }

    public String polishExperienceSummary(String rawExperienceSummary, List<String> skills, List<String> projects) {
        String aiResult = externalAiCharacterizationService.generateStructuredList(
                """
                You are an IT recruiting analyst and editor.
                Rewrite the candidate experience summary in Russian.
                Correct spelling, punctuation, and grammar.
                Convert first-person wording into professional third-person or neutral resume style.
                Preserve meaning, remove filler, and keep it concise.
                If duration, role, or contribution is mentioned, keep that information.
                Prefer wording like "Кандидат работал...", "Опыт включает...", "В рамках проектов...".
                Return plain text only.
                """,
                """
                Raw experience summary:
                %s

                Normalized skills:
                %s

                Normalized projects:
                %s
                """.formatted(
                        safeValue(rawExperienceSummary),
                        String.join(", ", safeList(skills)),
                        String.join(", ", safeList(projects))
                )
        );

        if (aiResult != null && !aiResult.isBlank()) {
            return cleanupParagraph(aiResult);
        }

        return polishExperienceFallback(rawExperienceSummary, skills, projects);
    }

    private List<String> normalizeSkillsFallback(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return List.of();
        }

        String prepared = prepareSkillInput(rawInput);
        List<String> extracted = extractKnownSkills(prepared);
        if (!extracted.isEmpty()) {
            return extracted;
        }

        Set<String> result = new LinkedHashSet<>();
        LIST_SPLIT_PATTERN.splitAsStream(prepared)
                .map(this::cleanupValue)
                .map(this::normalizeSkillToken)
                .filter(value -> !value.isBlank())
                .forEach(result::add);

        if (result.isEmpty()) {
            extractKnownSkills(rawInput).forEach(result::add);
        }

        return new ArrayList<>(result);
    }

    private List<String> normalizeProjectsFallback(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return List.of();
        }

        return LIST_SPLIT_PATTERN.splitAsStream(rawInput)
                .map(this::cleanupParagraph)
                .map(this::normalizeNarrativeText)
                .map(value -> GENERIC_PROJECT_PHRASES.matcher(value).replaceFirst(""))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(this::capitalizeFirst)
                .distinct()
                .collect(Collectors.toList());
    }

    private String polishValueStatementFallback(String rawValueStatement, List<String> skills) {
        String cleaned = cleanupParagraph(rawValueStatement);
        if (cleaned.isBlank()) {
            if (skills == null || skills.isEmpty()) {
                return "";
            }
            return "Кандидат приносит практическую ценность за счет уверенной работы с " + joinTopItems(skills, 4) + ".";
        }

        String normalized = normalizeNarrativeText(fixCommonTypos(cleaned));
        if (!normalized.toLowerCase(Locale.ROOT).startsWith("кандидат")) {
            normalized = "Кандидат " + Character.toLowerCase(normalized.charAt(0)) + normalized.substring(1);
        }
        if (skills != null && !skills.isEmpty() && !containsAnyIgnoreCase(normalized, skills)) {
            normalized = normalized + " Ключевой стек: " + joinTopItems(skills, 4) + ".";
        }
        return capitalizeFirst(ensureTrailingPeriod(normalized));
    }

    private String polishExperienceFallback(String rawExperienceSummary, List<String> skills, List<String> projects) {
        String cleaned = normalizeNarrativeText(fixCommonTypos(cleanupParagraph(rawExperienceSummary)));

        List<String> parts = splitSentences(cleaned).stream()
                .map(this::trimLeadingProjectVerb)
                .filter(value -> !value.isBlank())
                .limit(3)
                .collect(Collectors.toCollection(ArrayList::new));

        if (parts.isEmpty() && projects != null && !projects.isEmpty()) {
            parts.add("Опыт подтверждается проектами: " + joinTopItems(projects, 2) + ".");
        }

        if (!parts.isEmpty() && skills != null && !skills.isEmpty()) {
            String summary = String.join(" ", parts);
            if (!containsAnyIgnoreCase(summary, skills)) {
                parts.add("Основной стек: " + joinTopItems(skills, 4) + ".");
            }
        }

        return parts.stream()
                .map(this::capitalizeFirst)
                .map(this::ensureTrailingPeriod)
                .collect(Collectors.joining(" "))
                .trim();
    }

    private List<String> parseAiList(String aiResult) {
        if (aiResult == null || aiResult.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(aiResult);
            JsonNode items = root.path("items");
            if (!items.isArray()) {
                return List.of();
            }

            List<String> result = new ArrayList<>();
            for (JsonNode item : items) {
                String value = cleanupValue(item.asText());
                if (!value.isBlank()) {
                    result.add(value);
                }
            }
            return deduplicate(result);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<String> extractKnownSkills(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return List.of();
        }

        String normalized = " " + rawInput.toLowerCase(Locale.ROOT)
                .replaceAll("[()\\[\\]{}]", " ")
                .replaceAll("[^\\p{L}\\p{N}#+./-]+", " ")
                + " ";

        Set<String> result = new LinkedHashSet<>();
        SKILL_DICTIONARY.forEach((alias, canonical) -> {
            String probe = " " + alias.toLowerCase(Locale.ROOT) + " ";
            if (normalized.contains(probe)) {
                result.add(canonical);
            }
        });
        return new ArrayList<>(result);
    }

    private List<String> deduplicate(List<String> items) {
        return new ArrayList<>(new LinkedHashSet<>(items));
    }

    private String normalizeSkillToken(String value) {
        if (value.isBlank()) {
            return "";
        }

        String normalizedKey = value.toLowerCase(Locale.ROOT);
        if (SKILL_DICTIONARY.containsKey(normalizedKey)) {
            return SKILL_DICTIONARY.get(normalizedKey);
        }

        List<String> extracted = extractKnownSkills(value);
        if (extracted.size() == 1) {
            return extracted.get(0);
        }

        if (value.equals(value.toUpperCase(Locale.ROOT)) || value.contains(".")) {
            return value;
        }

        return Arrays.stream(value.split("\\s+"))
                .map(this::capitalizeWord)
                .collect(Collectors.joining(" "));
    }

    private String prepareSkillInput(String value) {
        return GENERIC_SKILL_PHRASES.matcher(value == null ? "" : value)
                .replaceAll("")
                .replaceAll("(?iu)\\b(?:я\\s+)?(?:работал(?:а)?\\s+с|использовал(?:а)?|знаю|умею|владею)\\b", "")
                .replaceAll("(?iu)\\s+(?:и|and)\\s+", ", ")
                .replace('|', ',')
                .replace('/', ',');
    }

    private String cleanupValue(String value) {
        if (value == null) {
            return "";
        }

        return fixCommonTypos(prepareSkillInput(value))
                .replaceAll("\\s+", " ")
                .replaceAll("^[\\-•\\d.)\\s]+", "")
                .trim();
    }

    private String cleanupParagraph(String value) {
        if (value == null) {
            return "";
        }

        return fixCommonTypos(value)
                .replaceAll("\\s+", " ")
                .replaceAll("\\s+([,.;:!?])", "$1")
                .trim();
    }

    private String normalizeNarrativeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value
                .replaceAll("(?iu)^я\\s+", "Кандидат ")
                .replaceAll("(?iu)\\bя\\s+работал(?:а)?\\b", "кандидат работал")
                .replaceAll("(?iu)\\bя\\s+занимал(?:ся|ась)\\b", "кандидат занимался")
                .replaceAll("(?iu)\\bя\\s+делал(?:а)?\\b", "кандидат выполнял")
                .replaceAll("(?iu)\\bя\\s+разрабатывал(?:а)?\\b", "кандидат разрабатывал")
                .replaceAll("(?iu)\\bя\\s+участвовал(?:а)?\\b", "кандидат участвовал")
                .replaceAll("(?iu)\\bумею\\b", "владеет")
                .replaceAll("(?iu)\\bделал(?:а)?\\b", "выполнял");
    }

    private String fixCommonTypos(String value) {
        return value
                .replace("архетирутура", "архитектура")
                .replace("архетектура", "архитектура")
                .replace("кавчественно", "качественно")
                .replace("постгрес", "PostgreSQL")
                .replace("флаттер", "Flutter");
    }

    private List<String> splitSentences(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        return Arrays.stream(text.split("(?<=[.!?])\\s+"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toList());
    }

    private String trimLeadingProjectVerb(String value) {
        return GENERIC_PROJECT_PHRASES.matcher(value).replaceFirst("").trim();
    }

    private boolean containsAnyIgnoreCase(String source, List<String> values) {
        String lowerSource = source.toLowerCase(Locale.ROOT);
        return values.stream().anyMatch(value -> lowerSource.contains(value.toLowerCase(Locale.ROOT)));
    }

    private String joinTopItems(List<String> values, int limit) {
        return values.stream().limit(limit).collect(Collectors.joining(", "));
    }

    private String capitalizeWord(String value) {
        if (value.isBlank()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    private String capitalizeFirst(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String ensureTrailingPeriod(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.matches(".*[.!?]$") ? value : value + ".";
    }

    private List<String> safeList(List<String> value) {
        return value == null ? List.of() : value;
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }
}
