package kz.invision.analyzer.services;

import kz.invision.analyzer.api.dto.AnalysisRequest;
import kz.invision.analyzer.api.dto.AnalysisResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CandidateAnalysisServiceTest {

    private final CandidateAnalysisService service = new CandidateAnalysisService();

    @Test
    void shouldProduceStrongResultForBackendProfile() {
        AnalysisRequest request = new AnalysisRequest(
                "Aruzhan Sarsen",
                "Java Backend Engineer",
                "Java, Spring Boot, REST APIs, PostgreSQL, Docker, Kafka, CI/CD, Testing",
                4,
                "Built production backend services, improved API performance, owned integrations, worked with product, wrote tests, mentored juniors."
        );

        AnalysisResponse response = service.analyze(request);

        assertThat(response.overallScore()).isGreaterThanOrEqualTo(75);
        assertThat(response.roleMatchScore()).isGreaterThanOrEqualTo(75);
        assertThat(response.strengths()).isNotEmpty();
        assertThat(response.interviewQuestions()).isNotEmpty();
        assertThat(response.summary()).contains("Aruzhan Sarsen");
    }

    @Test
    void shouldExposeRisksForThinProfile() {
        AnalysisRequest request = new AnalysisRequest(
                "Dana",
                "Java Backend Engineer",
                "Excel, Word",
                0,
                "Junior candidate with limited commercial experience."
        );

        AnalysisResponse response = service.analyze(request);

        assertThat(response.overallScore()).isLessThan(75);
        assertThat(response.risks()).isNotEmpty();
        assertThat(response.recommendations()).isNotEmpty();
        assertThat(response.readinessLabel()).isIn("Needs evidence", "High risk");
    }
}
