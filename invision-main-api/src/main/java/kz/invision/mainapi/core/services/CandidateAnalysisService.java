package kz.invision.mainapi.core.services;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CandidateAnalysisService {

    private final ExternalAiCharacterizationService externalAiCharacterizationService;

    public CandidateAnalysisService(ExternalAiCharacterizationService externalAiCharacterizationService) {
        this.externalAiCharacterizationService = externalAiCharacterizationService;
    }

    public Map<String, Object> analyzeCandidate(
            String name,
            String skills,
            String experience,
            String valueStatement,
            List<String> projects
    ) {
        String normalizedSkills = skills == null ? "" : skills.toLowerCase(Locale.ROOT);
        int score = calculateStrictScore(normalizedSkills, experience);
        String aiVerdict = externalAiCharacterizationService.generateCandidateVerdict(
                name,
                skills,
                experience,
                valueStatement,
                projects
        );

        if (aiVerdict == null || aiVerdict.isBlank()) {
            aiVerdict = buildFallbackVerdict(
                    name,
                    normalizedSkills,
                    experience,
                    valueStatement,
                    projects,
                    score
            );
        }

        return Map.of(
                "score", score,
                "verdict", aiVerdict
        );
    }

    public Map<String, Object> analyzeCandidate(String name, String skills, String experience) {
        return analyzeCandidate(name, skills, experience, "", List.of());
    }

    private int calculateStrictScore(String normalizedSkills, String experience) {
        int score = 35;
        boolean hasJava = normalizedSkills.contains("java");
        boolean hasFlutter = normalizedSkills.contains("flutter");
        boolean hasSpring = normalizedSkills.contains("spring");
        boolean hasSql = normalizedSkills.contains("sql") || normalizedSkills.contains("postgres");
        boolean hasArchitecture = normalizedSkills.contains("architecture")
                || normalizedSkills.contains("архит");
        int experienceLength = experience == null ? 0 : experience.trim().length();

        if (hasJava) {
            score += 12;
        }
        if (hasFlutter) {
            score += 12;
        }
        if (hasSpring) {
            score += 8;
        }
        if (hasSql) {
            score += 8;
        }
        if (hasArchitecture) {
            score += 5;
        }

        if (experienceLength >= 250) {
            score += 15;
        } else if (experienceLength >= 120) {
            score += 10;
        } else if (experienceLength >= 60) {
            score += 5;
        }

        return Math.max(20, Math.min(score, 95));
    }

    private String buildFallbackVerdict(
            String name,
            String normalizedSkills,
            String experience,
            String valueStatement,
            List<String> projects,
            int score
    ) {
        String candidateName = name == null || name.isBlank() ? "Кандидат" : name.trim();
        String lowerExperience = experience == null ? "" : experience.toLowerCase(Locale.ROOT);
        String lowerValue = valueStatement == null ? "" : valueStatement.toLowerCase(Locale.ROOT);
        List<String> skillTokens = Arrays.stream(normalizedSkills.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.toList());

        String stackPreview = skillTokens.stream()
                .limit(3)
                .map(this::prettifySkill)
                .collect(Collectors.joining(", "));
        String projectPreview = projects == null || projects.isEmpty()
                ? ""
                : projects.stream().limit(2).collect(Collectors.joining("; "));

        boolean hasFlutter = containsAny(skillTokens, "flutter", "dart");
        boolean hasJava = containsAny(skillTokens, "java");
        boolean hasSpring = containsAny(skillTokens, "spring");
        boolean hasSql = containsAny(skillTokens, "sql", "postgres");
        boolean hasArchitecture = lowerExperience.contains("архит") || containsAny(skillTokens, "архит");
        boolean hasBanking = lowerExperience.contains("банк") || lowerExperience.contains("финтех")
                || projectPreview.toLowerCase(Locale.ROOT).contains("банк");
        boolean hasAnalytics = lowerExperience.contains("аналит") || lowerExperience.contains("математ");
        boolean hasTeamwork = lowerExperience.contains("команд") || lowerExperience.contains("коммуник");
        boolean hasLeadership = lowerExperience.contains("лид") || lowerExperience.contains("ментор");
        boolean valueFocus = lowerValue.contains("бизнес") || lowerValue.contains("ценност") || lowerValue.contains("результат");

        int variant = Math.abs((normalizedSkills + "|" + lowerExperience + "|" + lowerValue + "|" + projectPreview).hashCode()) % 3;

        if (hasFlutter && hasBanking) {
            return switch (variant) {
                case 0 -> candidateName + " выглядит как профиль для прикладной мобильной разработки в продуктовой среде: сочетание "
                        + fallbackStack(stackPreview) + " и финтех-контекста делает кандидата особенно уместным для клиентских приложений с высокой ценой ошибок. "
                        + "Сильная сторона здесь в практической пригодности к задачам, где важны стабильность, UX и интеграции. "
                        + "Текущий ИИ-балл: " + score + ".";
                case 1 -> candidateName + " скорее выделяется не общими словами про мобильную разработку, а предметной связкой "
                        + fallbackStack(stackPreview) + " с банковскими сценариями. "
                        + "Такой профиль полезен там, где мобильный интерфейс тесно связан с надежностью процессов и внешними интеграциями. "
                        + "Текущий ИИ-балл: " + score + ".";
                default -> candidateName + " смотрится убедительно именно на пересечении Flutter-разработки и банковского домена. "
                        + "Если оценивать по прикладной ценности, то " + fallbackStack(stackPreview) + " в таком контексте выглядит сильнее, чем просто абстрактный мобильный стек. "
                        + "Текущий ИИ-балл: " + score + ".";
            };
        }

        if (hasJava && hasSpring && hasSql) {
            return switch (variant) {
                case 0 -> candidateName + " воспринимается как backend-инженер с понятным прикладным фундаментом: стек "
                        + fallbackStack(stackPreview) + " хорошо ложится на сервисную разработку, API и работу с данными. "
                        + architectureClause(hasArchitecture) + " Текущий ИИ-балл: " + score + ".";
                case 1 -> candidateName + " производит впечатление кандидата для backend-задач, где требуется не просто знание Java, а связка "
                        + fallbackStack(stackPreview) + " в реальной сервисной разработке. "
                        + architectureClause(hasArchitecture) + " Текущий ИИ-балл: " + score + ".";
                default -> candidateName + " выглядит сильнее всего в backend-контуре: " + fallbackStack(stackPreview)
                        + " дает понятную базу для API, бизнес-логики и работы с хранилищем. "
                        + architectureClause(hasArchitecture) + " Текущий ИИ-балл: " + score + ".";
            };
        }

        if (hasAnalytics) {
            return switch (variant) {
                case 0 -> candidateName + " производит впечатление специалиста с выраженной аналитической опорой. "
                        + "Такой профиль обычно лучше раскрывается там, где важны точность решений, структурное мышление и аккуратная декомпозиция задач, а не только перечисление стека " + fallbackStack(stackPreview) + ". "
                        + "Текущий ИИ-балл: " + score + ".";
                case 1 -> candidateName + " скорее интересен своей аналитической базой, чем формальным набором технологий. "
                        + "Если интервью подтвердит глубину практики, такой кандидат может быть особенно полезен в задачах со сложной логикой и высокой требовательностью к качеству решений. "
                        + "Текущий ИИ-балл: " + score + ".";
                default -> candidateName + " читается как профиль с сильным уклоном в аналитику и осмысленную инженерную работу. "
                        + "На фоне обычных анкет это выделяет кандидата сильнее, чем просто общий перечень навыков " + fallbackStack(stackPreview) + ". "
                        + "Текущий ИИ-балл: " + score + ".";
            };
        }

        if (hasTeamwork || hasLeadership) {
            return switch (variant) {
                case 0 -> candidateName + " смотрится сильнее среднего по зрелости взаимодействия с командой, а это уменьшает риск сложной адаптации и повышает предсказуемость работы в delivery-процессе. "
                        + (hasLeadership ? "Дополнительный плюс дает признак лидерской или менторской функции. " : "")
                        + "Текущий ИИ-балл: " + score + ".";
                case 1 -> candidateName + " ценен не только технической частью, но и тем, как описан командный контекст работы. "
                        + (hasLeadership ? "Для роли с повышенной самостоятельностью это особенно важно. " : "")
                        + "Текущий ИИ-балл: " + score + ".";
                default -> candidateName + " выделяется не только стеком " + fallbackStack(stackPreview) + ", но и более зрелым описанием взаимодействия с людьми и процессами. "
                        + (hasLeadership ? "Это делает профиль заметно интереснее для команд, где нужен не просто исполнитель. " : "")
                        + "Текущий ИИ-балл: " + score + ".";
            };
        }

        if (!projectPreview.isBlank()) {
            return switch (variant) {
                case 0 -> candidateName + " выглядит как прикладной инженер, потому что за анкетой видны не только навыки " + fallbackStack(stackPreview)
                        + ", но и конкретные проекты: " + projectPreview + ". "
                        + valueClause(valueFocus) + " Текущий ИИ-балл: " + score + ".";
                case 1 -> candidateName + " воспринимается убедительнее за счет проектного следа: " + projectPreview + ". "
                        + "Это делает профиль более предметным и позволяет оценивать кандидата не по абстрактному стеку, а по реальным зонам применения. "
                        + "Текущий ИИ-балл: " + score + ".";
                default -> candidateName + " скорее читается через реализованные проекты, чем через сухой список технологий. "
                        + "На стороне кандидата работает то, что опыт привязан к конкретным задачам: " + projectPreview + ". "
                        + "Текущий ИИ-балл: " + score + ".";
            };
        }

        if (!stackPreview.isBlank()) {
            return switch (variant) {
                case 0 -> candidateName + " выглядит как прикладной инженер с фокусом на " + stackPreview + ". "
                        + "Анкета дает базу для сравнения, но итоговый вывод все еще зависит от того, насколько самостоятельно кандидат вел задачи и какой результат доводил до продакшена. "
                        + "Текущий ИИ-балл: " + score + ".";
                case 1 -> candidateName + " пока сильнее всего раскрывается через стек " + stackPreview + ". "
                        + "Для более точного вердикта не хватает масштаба проектов, но уже видно, в каком инженерном контуре кандидат может быть полезен. "
                        + "Текущий ИИ-балл: " + score + ".";
                default -> candidateName + " дает понятный технический профиль за счет стека " + stackPreview + ", хотя содержательность итоговой оценки все еще ограничена глубиной описания опыта. "
                        + "Для предварительного сравнения этого уже достаточно, но на интервью стоит добрать факты про самостоятельность и результат. "
                        + "Текущий ИИ-балл: " + score + ".";
            };
        }

        return candidateName + " пока описан слишком общо, поэтому ИИ оценивает профиль осторожно. "
                + "Для более содержательного сравнения не хватает четкого стека, конкретных проектов и описания личной зоны ответственности. "
                + "Текущий ИИ-балл: " + score + ".";
    }

    private boolean containsAny(List<String> values, String... probes) {
        return values.stream().anyMatch(value ->
                Arrays.stream(probes).anyMatch(value::contains)
        );
    }

    private String prettifySkill(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.equals(value.toUpperCase(Locale.ROOT))) {
            return value;
        }
        return Arrays.stream(value.split("\\s+"))
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .collect(Collectors.joining(" "));
    }

    private String fallbackStack(String preview) {
        return preview.isBlank() ? "указанного стека" : preview;
    }

    private String architectureClause(boolean hasArchitecture) {
        return hasArchitecture
                ? "Отдельный плюс дает акцент на архитектурном мышлении, что повышает ценность кандидата на более самостоятельных задачах."
                : "Для более сильной оценки не хватает фактов о масштабах систем и уровне архитектурной самостоятельности.";
    }

    private String valueClause(boolean valueFocus) {
        return valueFocus
                ? "В описании ценности заметен акцент на прикладной бизнес-результат, и это усиливает профиль."
                : "Для более сильной подачи можно было бы яснее связать опыт с бизнес-результатом.";
    }
}
