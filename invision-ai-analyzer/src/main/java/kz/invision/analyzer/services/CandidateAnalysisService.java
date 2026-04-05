package kz.invision.analyzer.services;

import kz.invision.analyzer.api.dto.AnalysisRequest;
import kz.invision.analyzer.api.dto.AnalysisResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class CandidateAnalysisService {

    private static final List<String> CORE_BACKEND_SKILLS = List.of(
            "java", "spring", "spring boot", "rest", "sql", "postgres", "docker", "kafka", "microservices"
    );
    private static final List<String> CORE_FRONTEND_SKILLS = List.of(
            "javascript", "typescript", "react", "vue", "next", "css", "html", "figma"
    );
    private static final List<String> CORE_MOBILE_SKILLS = List.of(
            "flutter", "dart", "kotlin", "swift", "android", "ios", "firebase"
    );
    private static final List<String> SIGNAL_DELIVERY = List.of(
            "production", "architecture", "api", "integration", "ci/cd", "testing", "monitoring"
    );
    private static final List<String> SIGNAL_COMMUNICATION = List.of(
            "lead", "mentor", "team", "stakeholder", "presentation", "communication", "collaboration"
    );
    private static final List<String> SIGNAL_GROWTH = List.of(
            "learn", "improved", "optimized", "launched", "owned", "initiative", "research"
    );
    private final ConcurrentMap<String, AnalysisResponse> analysisCache = new ConcurrentHashMap<>();

    public AnalysisResponse analyze(AnalysisRequest request) {
        long startedAt = System.nanoTime();
        String cacheKey = buildCacheKey(request);
        AnalysisResponse cachedResponse = analysisCache.get(cacheKey);
        if (cachedResponse != null) {
            return withMeta(cachedResponse, elapsedMillis(startedAt), true);
        }

        String normalizedRole = normalize(request.targetRole());
        String normalizedSkills = normalize(request.skills());
        String normalizedSummary = normalize(request.summary());
        String combinedProfile = normalizedSkills + " " + normalizedSummary;

        int roleMatchScore = calculateRoleMatchScore(normalizedRole, combinedProfile);
        int deliveryScore = calculateSignalScore(combinedProfile, SIGNAL_DELIVERY, request.experienceYears(), 42);
        int communicationScore = calculateSignalScore(combinedProfile, SIGNAL_COMMUNICATION, request.experienceYears(), 38);
        int growthScore = calculateSignalScore(combinedProfile, SIGNAL_GROWTH, request.experienceYears(), 36);

        int overallScore = clamp((int) Math.round(
                roleMatchScore * 0.42
                        + deliveryScore * 0.24
                        + communicationScore * 0.16
                        + growthScore * 0.18
        ));

        List<String> strengths = buildStrengths(request, combinedProfile, roleMatchScore, deliveryScore, communicationScore, growthScore);
        List<String> risks = buildRisks(request, combinedProfile, roleMatchScore, deliveryScore, communicationScore);
        List<String> recommendations = buildRecommendations(request, roleMatchScore, deliveryScore, communicationScore, growthScore);
        List<String> interviewQuestions = buildInterviewQuestions(request, combinedProfile);
        List<AnalysisResponse.ScoreBadge> badges = buildBadges(overallScore, roleMatchScore, deliveryScore, communicationScore, growthScore);

        String readinessLabel = resolveReadinessLabel(overallScore);
        String headline = buildHeadline(request, overallScore, roleMatchScore);
        String summary = buildSummary(request, overallScore, strengths, risks);

        AnalysisResponse computedResponse = new AnalysisResponse(
                request.candidateName().trim(),
                request.targetRole().trim(),
                overallScore,
                readinessLabel,
                roleMatchScore,
                deliveryScore,
                communicationScore,
                growthScore,
                headline,
                summary,
                strengths,
                risks,
                recommendations,
                interviewQuestions,
                badges,
                new AnalysisResponse.AnalysisMeta(elapsedMillis(startedAt), false)
        );
        analysisCache.put(cacheKey, computedResponse);
        return computedResponse;
    }

    private int calculateRoleMatchScore(String normalizedRole, String combinedProfile) {
        List<String> targetSkillSet = inferRoleSkillSet(normalizedRole);
        long matchedSkills = targetSkillSet.stream()
                .filter(combinedProfile::contains)
                .count();

        int baseScore = 45 + (int) matchedSkills * 8;
        if (combinedProfile.contains(normalizedRole)) {
            baseScore += 8;
        }
        return clamp(baseScore);
    }

    private int calculateSignalScore(String combinedProfile, List<String> signals, int experienceYears, int base) {
        long signalHits = signals.stream()
                .filter(combinedProfile::contains)
                .count();

        int score = base + (int) signalHits * 7 + Math.min(experienceYears, 10) * 2;
        return clamp(score);
    }

    private List<String> buildStrengths(
            AnalysisRequest request,
            String combinedProfile,
            int roleMatchScore,
            int deliveryScore,
            int communicationScore,
            int growthScore
    ) {
        Set<String> strengths = new LinkedHashSet<>();

        if (roleMatchScore >= 78) {
            strengths.add("Strong role alignment: the profile contains the core stack signals expected for " + request.targetRole().trim() + ".");
        }
        if (deliveryScore >= 72) {
            strengths.add("Execution maturity is visible: the summary hints at real delivery, integrations, testing, or production exposure.");
        }
        if (communicationScore >= 68) {
            strengths.add("Collaboration signal is solid: the profile suggests teamwork, stakeholder interaction, or mentoring capacity.");
        }
        if (growthScore >= 68) {
            strengths.add("Growth mindset is credible: the profile shows optimization, ownership, or active learning signals.");
        }

        topMatchedKeywords(combinedProfile, inferRoleSkillSet(normalize(request.targetRole())), 3)
                .forEach(skill -> strengths.add("Relevant stack fit: `" + skill + "` is directly aligned with the target role."));

        if (request.experienceYears() >= 4) {
            strengths.add("Experience level is high enough to handle ambiguous tasks without constant supervision.");
        }

        return strengths.stream().limit(5).toList();
    }

    private List<String> buildRisks(
            AnalysisRequest request,
            String combinedProfile,
            int roleMatchScore,
            int deliveryScore,
            int communicationScore
    ) {
        Set<String> risks = new LinkedHashSet<>();

        if (request.experienceYears() <= 1) {
            risks.add("Low production mileage: the candidate may still need close guidance on prioritization and edge cases.");
        }
        if (roleMatchScore < 70) {
            risks.add("Role fit is partial: the profile does not clearly demonstrate enough stack overlap for " + request.targetRole().trim() + ".");
        }
        if (deliveryScore < 68) {
            risks.add("Delivery evidence is thin: the summary should show more concrete outcomes, ownership, or shipped work.");
        }
        if (communicationScore < 64) {
            risks.add("Communication signal is under-documented: there is not enough proof of collaboration or stakeholder work.");
        }
        if (!combinedProfile.contains("test") && !combinedProfile.contains("testing")) {
            risks.add("Quality signal is weak: there is no explicit mention of testing or reliability practices.");
        }

        return risks.stream().limit(4).toList();
    }

    private List<String> buildRecommendations(
            AnalysisRequest request,
            int roleMatchScore,
            int deliveryScore,
            int communicationScore,
            int growthScore
    ) {
        List<String> recommendations = new ArrayList<>();

        if (roleMatchScore < 75) {
            recommendations.add("Tighten the profile around the target role: make the stack and role-specific achievements explicit in the summary.");
        }
        if (deliveryScore < 72) {
            recommendations.add("Add 2-3 measurable outcomes: performance gains, feature launches, reliability improvements, or scale numbers.");
        }
        if (communicationScore < 68) {
            recommendations.add("Show collaboration depth: mention cross-functional work, client communication, mentorship, or leading implementation discussions.");
        }
        if (growthScore < 68) {
            recommendations.add("Surface learning velocity: include examples of adopting new tools, improving architecture, or driving technical initiatives.");
        }

        recommendations.add("For the hackathon demo, show this analysis beside the raw candidate input to emphasize explainability rather than black-box scoring.");
        recommendations.add("Use the generated interview pack as the next step in the hiring flow, so the product feels actionable instead of decorative.");

        return recommendations.stream().limit(5).toList();
    }

    private List<String> buildInterviewQuestions(AnalysisRequest request, String combinedProfile) {
        Set<String> questions = new LinkedHashSet<>();
        questions.add("Which project best proves you are ready for the `" + request.targetRole().trim() + "` role, and what exactly was your contribution?");

        if (combinedProfile.contains("spring") || combinedProfile.contains("java")) {
            questions.add("Walk through a backend decision you made in Java or Spring. What tradeoff did you accept and why?");
        }
        if (combinedProfile.contains("flutter") || combinedProfile.contains("mobile")) {
            questions.add("Describe a mobile feature you shipped. How did you handle state, performance, and release quality?");
        }
        if (combinedProfile.contains("react") || combinedProfile.contains("frontend")) {
            questions.add("How do you keep a frontend codebase maintainable when product requirements change quickly?");
        }

        questions.add("Tell me about a bug or failure in production. How did you detect it, communicate it, and prevent it from happening again?");
        questions.add("If you joined tomorrow, what part of your current skill set would create the fastest impact in the first two weeks?");

        return questions.stream().limit(5).toList();
    }

    private List<AnalysisResponse.ScoreBadge> buildBadges(
            int overallScore,
            int roleMatchScore,
            int deliveryScore,
            int communicationScore,
            int growthScore
    ) {
        List<AnalysisResponse.ScoreBadge> badges = new ArrayList<>();

        if (overallScore >= 82) {
            badges.add(new AnalysisResponse.ScoreBadge("High hiring potential", "success"));
        }
        if (roleMatchScore >= 78) {
            badges.add(new AnalysisResponse.ScoreBadge("Role aligned", "info"));
        }
        if (deliveryScore >= 72) {
            badges.add(new AnalysisResponse.ScoreBadge("Execution signal", "accent"));
        }
        if (communicationScore >= 68) {
            badges.add(new AnalysisResponse.ScoreBadge("Team-ready", "warm"));
        }
        if (growthScore >= 68) {
            badges.add(new AnalysisResponse.ScoreBadge("Growth mindset", "neutral"));
        }

        return badges.stream()
                .sorted(Comparator.comparing(AnalysisResponse.ScoreBadge::label))
                .toList();
    }

    private String resolveReadinessLabel(int overallScore) {
        if (overallScore >= 86) {
            return "Strong shortlist";
        }
        if (overallScore >= 74) {
            return "Worth interview";
        }
        if (overallScore >= 62) {
            return "Needs evidence";
        }
        return "High risk";
    }

    private String buildHeadline(AnalysisRequest request, int overallScore, int roleMatchScore) {
        if (overallScore >= 84) {
            return request.candidateName().trim() + " looks like a credible match for the next interview stage.";
        }
        if (roleMatchScore >= 76) {
            return request.candidateName().trim() + " has promising role fit, but the profile needs stronger proof of impact.";
        }
        return request.candidateName().trim() + " shows potential, though the current profile is still too thin for a confident decision.";
    }

    private String buildSummary(AnalysisRequest request, int overallScore, List<String> strengths, List<String> risks) {
        String strongestPoint = strengths.isEmpty() ? "the role alignment is still unclear" : strengths.get(0);
        String mainRisk = risks.isEmpty() ? "there are no major risks detected" : risks.get(0);
        return "The analyzer gives " + request.candidateName().trim() + " a score of " + overallScore
                + "/100 for the `" + request.targetRole().trim() + "` track. Best signal: " + strongestPoint
                + " Main concern: " + mainRisk;
    }

    private List<String> inferRoleSkillSet(String normalizedRole) {
        if (normalizedRole.contains("frontend") || normalizedRole.contains("ui")) {
            return CORE_FRONTEND_SKILLS;
        }
        if (normalizedRole.contains("mobile") || normalizedRole.contains("flutter") || normalizedRole.contains("android")) {
            return CORE_MOBILE_SKILLS;
        }
        return CORE_BACKEND_SKILLS;
    }

    private List<String> topMatchedKeywords(String combinedProfile, List<String> targetKeywords, int limit) {
        return targetKeywords.stream()
                .filter(combinedProfile::contains)
                .limit(limit)
                .toList();
    }

    private String normalize(String input) {
        return input == null ? "" : input.toLowerCase(Locale.ROOT).trim();
    }

    private int clamp(int value) {
        return Math.max(35, Math.min(100, value));
    }

    private String buildCacheKey(AnalysisRequest request) {
        return String.join("|",
                normalize(request.candidateName()),
                normalize(request.targetRole()),
                normalize(request.skills()),
                String.valueOf(request.experienceYears()),
                normalize(request.summary())
        );
    }

    private AnalysisResponse withMeta(AnalysisResponse response, long processingTimeMs, boolean cached) {
        return new AnalysisResponse(
                response.candidateName(),
                response.targetRole(),
                response.overallScore(),
                response.readinessLabel(),
                response.roleMatchScore(),
                response.deliveryScore(),
                response.communicationScore(),
                response.growthScore(),
                response.headline(),
                response.summary(),
                response.strengths(),
                response.risks(),
                response.recommendations(),
                response.interviewQuestions(),
                response.badges(),
                new AnalysisResponse.AnalysisMeta(processingTimeMs, cached)
        );
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
