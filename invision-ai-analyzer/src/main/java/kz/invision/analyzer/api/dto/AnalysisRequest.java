package kz.invision.analyzer.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalysisRequest(
        @NotBlank(message = "Candidate name is required")
        @Size(max = 80, message = "Candidate name must be shorter than 80 characters")
        String candidateName,

        @NotBlank(message = "Target role is required")
        @Size(max = 80, message = "Target role must be shorter than 80 characters")
        String targetRole,

        @NotBlank(message = "Skills are required")
        @Size(max = 600, message = "Skills must be shorter than 600 characters")
        String skills,

        @Min(value = 0, message = "Experience years cannot be negative")
        @Max(value = 40, message = "Experience years must be realistic")
        int experienceYears,

        @NotBlank(message = "Candidate summary is required")
        @Size(max = 2000, message = "Summary must be shorter than 2000 characters")
        String summary
) {
}
