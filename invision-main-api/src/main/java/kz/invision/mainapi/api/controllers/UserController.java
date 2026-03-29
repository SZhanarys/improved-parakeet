package kz.invision.mainapi.api.controllers;

import kz.invision.mainapi.core.models.User;
import kz.invision.mainapi.core.repositories.UserRepository;
import kz.invision.mainapi.core.services.CandidateAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final CandidateAnalysisService candidateAnalysisService;

    @PutMapping("/{id}/profile")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody User profileData) {
        return userRepository.findById(id).map(user -> {
            user.setFullName(profileData.getFullName());
            user.setSkills(profileData.getSkills());
            user.setExperience(profileData.getExperience());

            var ai = candidateAnalysisService.analyzeCandidate(
                    user.getFullName(),
                    user.getSkills(),
                    user.getExperience()
            );
            user.setAiScore((Integer) ai.get("score"));
            user.setAiVerdict((String) ai.get("verdict"));

            return ResponseEntity.ok(userRepository.save(user));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/list")
    public ResponseEntity<?> getList(@RequestParam String requesterRole) {
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "aiScore"));
        if ("ADMIN".equals(requesterRole)) {
            return ResponseEntity.ok(users);
        }

        return ResponseEntity.ok(users.stream().map(user -> Map.of(
                "name", user.getFullName() != null ? user.getFullName() : "Кандидат",
                "score", user.getAiScore() != null ? user.getAiScore() : 0
        )).collect(Collectors.toList()));
    }
}
