package kz.invision.mainapi.core.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
public class ExternalAiCharacterizationService {

    private static final Logger log = LoggerFactory.getLogger(ExternalAiCharacterizationService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${ai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${ai.model:gpt-4.1-mini}")
    private String model;

    @Value("${ai.api-key:}")
    private String apiKey;

    public ExternalAiCharacterizationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    @PostConstruct
    void logConfiguration() {
        if (!aiEnabled) {
            log.info("External AI is disabled. Set ai.enabled=true to enable remote AI calls.");
            return;
        }

        if (!hasApiConfiguration()) {
            log.warn("External AI is enabled, but AI_API_KEY is missing. Falling back to local analysis.");
            return;
        }

        log.info("External AI is enabled. Base URL: {}, model: {}", baseUrl, model);
    }

    public String generateCandidateVerdict(
            String name,
            String skills,
            String experience,
            String valueStatement,
            List<String> projects
    ) {
        if (!aiEnabled || !hasApiConfiguration()) {
            return null;
        }

        try {
            return sendPrompt(
                    """
                    You are an expert in IT recruiting and candidate assessment.
                    Write the result in Russian.

                    Strict rules:
                    - Do not use the same wording for different candidates.
                    - Base the verdict only on provided data.
                    - Silently correct spelling, punctuation, and grammar issues from source input.
                    - Write professionally and specifically, without filler.
                    - Emphasize real strengths, likely role fit, project context, possible risks, and what is still missing for a confident assessment.
                    - Keep the verdict individual in wording and sentence structure.

                    Output:
                    - Plain text only
                    - 3 to 5 sentences
                    - No bullet points
                    """,
                    buildPrompt(name, skills, experience, valueStatement, projects)
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("External AI request was interrupted.", ex);
            return null;
        } catch (IOException | RuntimeException ex) {
            log.warn("External AI verdict generation failed. Falling back to local analysis.", ex);
            return null;
        }
    }

    public String generateCandidateVerdict(String name, String skills, String experience) {
        return generateCandidateVerdict(name, skills, experience, "", List.of());
    }

    public String generateStructuredList(String systemPrompt, String rawInput) {
        if (!aiEnabled || !hasApiConfiguration()) {
            return null;
        }

        try {
            return sendPrompt(systemPrompt, rawInput);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("External AI request was interrupted.", ex);
            return null;
        } catch (IOException | RuntimeException ex) {
            log.warn("External AI structured generation failed. Falling back to local normalization.", ex);
            return null;
        }
    }

    private String sendPrompt(String systemPrompt, String userPrompt) throws IOException, InterruptedException {
        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of(
                        "model", model,
                        "input", java.util.List.of(
                                java.util.Map.of(
                                        "role", "system",
                                        "content", java.util.List.of(
                                                java.util.Map.of(
                                                        "type", "input_text",
                                                        "text", systemPrompt
                                                )
                                        )
                                ),
                                java.util.Map.of(
                                        "role", "user",
                                        "content", java.util.List.of(
                                                java.util.Map.of(
                                                        "type", "input_text",
                                                        "text", userPrompt
                                                )
                                        )
                                )
                        )
                )
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/responses"))
                .timeout(Duration.ofSeconds(45))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("External AI returned status {}. Response body: {}", response.statusCode(), trimForLog(response.body()));
            return null;
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode outputText = root.path("output_text");
        if (!outputText.isMissingNode() && !outputText.asText().isBlank()) {
            return outputText.asText().trim();
        }

        JsonNode output = root.path("output");
        for (JsonNode messageNode : output) {
            for (JsonNode contentNode : messageNode.path("content")) {
                JsonNode textNode = contentNode.path("text");
                if (!textNode.isMissingNode() && !textNode.asText().isBlank()) {
                    return textNode.asText().trim();
                }
            }
        }

        return null;
    }

    private String buildPrompt(
            String name,
            String skills,
            String experience,
            String valueStatement,
            List<String> projects
    ) {
        return """
                Prepare an individual candidate verdict in Russian.

                Candidate data:
                Name: %s
                Normalized skills: %s
                Experience summary: %s
                Value statement: %s
                Projects: %s

                Focus on stack combination, depth of experience, role fit, business value, projects, and risks.
                Avoid generic templates and vague praise.
                """.formatted(
                safeValue(name, "Не указано"),
                safeValue(skills, "Не указано"),
                safeValue(experience, "Не указано"),
                safeValue(valueStatement, "Не указано"),
                projects == null || projects.isEmpty() ? "Не указано" : String.join(", ", projects)
        );
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean hasApiConfiguration() {
        return apiKey != null && !apiKey.isBlank();
    }

    private String trimForLog(String value) {
        if (value == null || value.isBlank()) {
            return "<empty>";
        }
        return value.length() > 500 ? value.substring(0, 500) + "..." : value;
    }
}
