package kz.invision.analyzer.api.dto;

import java.util.List;

public record AnalysisResponse(
        String candidateName,
        String targetRole,
        int overallScore,
        String readinessLabel,
        int roleMatchScore,
        int deliveryScore,
        int communicationScore,
        int growthScore,
        String headline,
        String summary,
        List<String> strengths,
        List<String> risks,
        List<String> recommendations,
        List<String> interviewQuestions,
        List<ScoreBadge> badges,
        AnalysisMeta meta
) {
    public record ScoreBadge(String label, String tone) {
    }

    public record AnalysisMeta(long processingTimeMs, boolean cached) {
    }
}
