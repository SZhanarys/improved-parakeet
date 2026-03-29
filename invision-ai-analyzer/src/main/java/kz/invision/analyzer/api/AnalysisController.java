package kz.invision.analyzer.api;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/analyze")
public class AnalysisController {
    @PostMapping
    public Map<String, Object> analyze(@RequestBody Map<String, String> data) {
        return Map.of(
                "score", 85,
                "feedback", "Good skills in Java and Flutter"
        );
    }
}