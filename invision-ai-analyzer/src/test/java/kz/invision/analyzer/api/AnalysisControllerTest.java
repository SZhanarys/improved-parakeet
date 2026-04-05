package kz.invision.analyzer.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/analysis/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void shouldAnalyzeCandidate() throws Exception {
        String payload = """
                {
                  "candidateName": "Aruzhan Sarsen",
                  "targetRole": "Java Backend Engineer",
                  "skills": "Java, Spring Boot, Docker, PostgreSQL, Testing",
                  "experienceYears": 3,
                  "summary": "Built production APIs, improved performance, worked with product team, wrote tests."
                }
                """;

        mockMvc.perform(post("/api/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateName").value("Aruzhan Sarsen"))
                .andExpect(jsonPath("$.overallScore").isNumber())
                .andExpect(jsonPath("$.strengths").isArray())
                .andExpect(jsonPath("$.interviewQuestions").isArray());
    }

    @Test
    void shouldRejectInvalidPayload() throws Exception {
        String payload = """
                {
                  "candidateName": "",
                  "targetRole": "",
                  "skills": "",
                  "experienceYears": -1,
                  "summary": ""
                }
                """;

        mockMvc.perform(post("/api/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.candidateName").exists())
                .andExpect(jsonPath("$.errors.targetRole").exists());
    }
}
