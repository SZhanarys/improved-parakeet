package kz.invision.analyzer.api;

import jakarta.validation.Valid;
import kz.invision.analyzer.api.dto.AnalysisRequest;
import kz.invision.analyzer.api.dto.AnalysisResponse;
import kz.invision.analyzer.services.CandidateAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final CandidateAnalysisService candidateAnalysisService;

    public AnalysisController(CandidateAnalysisService candidateAnalysisService) {
        this.candidateAnalysisService = candidateAnalysisService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public AnalysisResponse analyze(@Valid @RequestBody AnalysisRequest request) {
        return candidateAnalysisService.analyze(request);
    }
}
