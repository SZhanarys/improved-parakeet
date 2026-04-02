package kz.invision.mainapi.api.controllers;

import kz.invision.mainapi.api.dto.CandidateQuestionnaireRequest;
import kz.invision.mainapi.core.models.CandidateProfile;
import kz.invision.mainapi.core.models.CandidateProject;
import kz.invision.mainapi.core.models.CandidateSkill;
import kz.invision.mainapi.core.models.User;
import kz.invision.mainapi.core.repositories.CandidateProfileRepository;
import kz.invision.mainapi.core.repositories.UserRepository;
import kz.invision.mainapi.core.services.CandidateAnalysisService;
import kz.invision.mainapi.core.services.CandidateComparisonService;
import kz.invision.mainapi.core.services.CandidateNormalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_PHONE_LENGTH = 32;
    private static final int MAX_SKILLS_LENGTH = 4000;
    private static final int MAX_VALUE_STATEMENT_LENGTH = 5000;
    private static final int MAX_EXPERIENCE_LENGTH = 8000;
    private static final int MAX_PROJECTS_LENGTH = 5000;

    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CandidateAnalysisService candidateAnalysisService;
    private final CandidateNormalizationService candidateNormalizationService;
    private final CandidateComparisonService candidateComparisonService;

    @PutMapping("/{id}/profile")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody User profileData) {
        return userRepository.findById(id).map(user -> {
            user.setFullName(profileData.getFullName());
            user.setSkills(profileData.getSkills());
            user.setExperience(profileData.getExperience());

            var ai = candidateAnalysisService.analyzeCandidate(
                    user.getFullName(),
                    user.getSkills(),
                    user.getExperience(),
                    "",
                    List.of()
            );
            user.setAiScore((Integer) ai.get("score"));
            user.setAiVerdict((String) ai.get("verdict"));

            return ResponseEntity.ok(userRepository.save(user));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/questionnaire")
    public ResponseEntity<?> submitQuestionnaire(
            @PathVariable Long id,
            @RequestParam Long requesterId,
            @RequestBody CandidateQuestionnaireRequest request
    ) {
        try {
            Optional<User> requester = userRepository.findById(requesterId);
            if (requester.isEmpty() || !requester.get().getId().equals(id)) {
                return ResponseEntity.status(403).body(Map.of("message", "Access denied"));
            }

            User user = requester.get();
            if (user.getRole() != User.Role.CANDIDATE) {
                return ResponseEntity.status(403).body(Map.of("message", "Only candidates can submit questionnaire"));
            }

            String validationError = validateQuestionnaire(request);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(Map.of("message", validationError));
            }

            String normalizedPhone = normalizePhone(request.getPhoneNumber());
            Optional<CandidateProfile> existingProfile = candidateProfileRepository.findByUserId(user.getId());
            if (candidateProfileRepository.existsByPhoneNumber(normalizedPhone)
                    && existingProfile.stream().noneMatch(profile -> normalizedPhone.equals(profile.getPhoneNumber()))) {
                return ResponseEntity.badRequest().body(Map.of("message", "Phone number is already used"));
            }

            List<String> normalizedSkills = candidateNormalizationService.normalizeSkills(request.getSkillsInput());
            List<String> normalizedProjects = candidateNormalizationService.normalizeProjects(request.getProjectsInput());

            if (normalizedSkills.isEmpty()) {
                normalizedSkills = List.of(cleanValue(request.getSkillsInput()));
            }
            if (normalizedProjects.isEmpty()) {
                normalizedProjects = List.of(cleanValue(request.getProjectsInput()));
            }

            String polishedValueStatement = candidateNormalizationService.polishValueStatement(
                    request.getValueStatement(),
                    normalizedSkills,
                    normalizedProjects
            );
            String polishedExperienceSummary = candidateNormalizationService.polishExperienceSummary(
                    request.getExperienceSummary(),
                    normalizedSkills,
                    normalizedProjects
            );

            CandidateProfile profile = existingProfile.orElseGet(CandidateProfile::new);
            profile.setUser(user);
            profile.setFirstName(cleanValue(request.getFirstName()));
            profile.setLastName(cleanValue(request.getLastName()));
            profile.setPhoneNumber(normalizedPhone);
            profile.setValueStatement(polishedValueStatement);
            profile.setExperienceSummary(polishedExperienceSummary);
            profile.setRawSkillsInput(cleanValue(request.getSkillsInput()));
            profile.setRawProjectsInput(cleanValue(request.getProjectsInput()));

            CandidateProfile savedProfile = candidateProfileRepository.save(profile);
            replaceSkills(savedProfile, normalizedSkills);
            replaceProjects(savedProfile, normalizedProjects);
            candidateProfileRepository.save(savedProfile);

            user.setFullName(savedProfile.getLastName() + " " + savedProfile.getFirstName());
            user.setSkills(String.join(", ", normalizedSkills));
            user.setExperience(savedProfile.getExperienceSummary() + "\nProjects: " + String.join(", ", normalizedProjects));

            var ai = candidateAnalysisService.analyzeCandidate(
                    user.getFullName(),
                    user.getSkills(),
                    user.getExperience(),
                    savedProfile.getValueStatement(),
                    normalizedProjects
            );
            user.setAiScore((Integer) ai.get("score"));
            user.setAiVerdict((String) ai.get("verdict"));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "message", existingProfile.isPresent()
                            ? "Questionnaire updated successfully"
                            : "Questionnaire submitted successfully",
                    "aiScore", user.getAiScore(),
                    "aiVerdict", user.getAiVerdict(),
                    "skills", normalizedSkills,
                    "projects", normalizedProjects
            ));
        } catch (Exception ex) {
            Throwable rootCause = ex;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }

            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Questionnaire save failed: " + ex.getClass().getSimpleName() + ": " + rootCause.getMessage()
            ));
        }
    }

    @GetMapping("/{id}/questionnaire-status")
    public ResponseEntity<?> getQuestionnaireStatus(
            @PathVariable Long id,
            @RequestParam Long requesterId
    ) {
        if (!id.equals(requesterId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied"));
        }

        return ResponseEntity.ok(Map.of("submitted", candidateProfileRepository.existsByUserId(id)));
    }

    @GetMapping("/list")
    public ResponseEntity<?> getList(@RequestParam Long requesterId) {
        var requester = userRepository.findById(requesterId);
        if (requester.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied"));
        }

        String requesterRole = requester.get().getRole().name();
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "aiScore"));
        if ("ADMIN".equals(requesterRole)) {
            return ResponseEntity.ok(users.stream().map(user -> {
                Optional<CandidateProfile> profile = candidateProfileRepository.findByUserId(user.getId());
                List<String> normalizedSkills = profile
                        .map(candidateProfile -> candidateProfile.getSkills().stream()
                                .map(CandidateSkill::getName)
                                .collect(Collectors.toList()))
                        .filter(skills -> !skills.isEmpty())
                        .orElseGet(() -> splitStoredSkills(user.getSkills()));
                return Map.of(
                        "id", user.getId(),
                        "fullName", user.getFullName() != null ? user.getFullName() : "Candidate",
                        "aiScore", user.getAiScore() != null ? user.getAiScore() : 0,
                        "phoneNumber", profile.map(CandidateProfile::getPhoneNumber).orElse(""),
                        "submitted", profile.isPresent(),
                        "skills", normalizedSkills
                );
            }).collect(Collectors.toList()));
        }

        return ResponseEntity.ok(users.stream().map(user -> Map.of(
                "name", user.getFullName() != null ? user.getFullName() : "Candidate",
                "score", user.getAiScore() != null ? user.getAiScore() : 0
        )).collect(Collectors.toList()));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<?> getCandidateDetails(
            @PathVariable Long id,
            @RequestParam Long requesterId
    ) {
        var requester = userRepository.findById(requesterId);
        if (requester.isEmpty() || requester.get().getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied"));
        }

        Map<String, Object> response = buildCandidateDetailsPayload(id);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/compare")
    public ResponseEntity<?> compareCandidates(
            @RequestParam Long requesterId,
            @RequestParam Long firstCandidateId,
            @RequestParam Long secondCandidateId
    ) {
        var requester = userRepository.findById(requesterId);
        if (requester.isEmpty() || requester.get().getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied"));
        }
        if (firstCandidateId.equals(secondCandidateId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Select two different candidates"));
        }

        Map<String, Object> firstCandidate = buildCandidateDetailsPayload(firstCandidateId);
        Map<String, Object> secondCandidate = buildCandidateDetailsPayload(secondCandidateId);
        if (firstCandidate == null || secondCandidate == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
                "firstCandidate", firstCandidate,
                "secondCandidate", secondCandidate,
                "comparison", candidateComparisonService.compareCandidates(firstCandidate, secondCandidate)
        ));
    }

    private Map<String, Object> buildCandidateDetailsPayload(Long id) {
        Optional<User> userOptional = userRepository.findById(id);
        Optional<CandidateProfile> profileOptional = candidateProfileRepository.findByUserId(id);
        if (userOptional.isEmpty() || profileOptional.isEmpty()) {
            return null;
        }

        User user = userOptional.get();
        CandidateProfile profile = profileOptional.get();

        List<String> normalizedSkills = candidateNormalizationService.normalizeSkills(
                profile.getRawSkillsInput()
        );
        List<String> normalizedProjects = candidateNormalizationService.normalizeProjects(
                profile.getRawProjectsInput()
        );
        if (normalizedSkills.isEmpty()) {
            normalizedSkills = profile.getSkills().stream()
                    .map(CandidateSkill::getName)
                    .collect(Collectors.toList());
        }
        if (normalizedProjects.isEmpty()) {
            normalizedProjects = profile.getProjects().stream()
                    .map(CandidateProject::getTitle)
                    .collect(Collectors.toList());
        }
        String polishedValueStatement = candidateNormalizationService.polishValueStatement(
                profile.getValueStatement(),
                normalizedSkills,
                normalizedProjects
        );
        String polishedExperienceSummary = candidateNormalizationService.polishExperienceSummary(
                profile.getExperienceSummary(),
                normalizedSkills,
                normalizedProjects
        );
        var ai = candidateAnalysisService.analyzeCandidate(
                user.getFullName(),
                String.join(", ", normalizedSkills),
                polishedExperienceSummary,
                polishedValueStatement,
                normalizedProjects
        );
        user.setAiScore((Integer) ai.get("score"));
        user.setAiVerdict((String) ai.get("verdict"));
        User savedUser = userRepository.save(user);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", savedUser.getId());
        response.put("email", savedUser.getEmail());
        response.put("fullName", savedUser.getFullName() != null ? savedUser.getFullName() : "Candidate");
        response.put("firstName", profile.getFirstName());
        response.put("lastName", profile.getLastName());
        response.put("phoneNumber", profile.getPhoneNumber());
        response.put("valueStatement", polishedValueStatement);
        response.put("experienceSummary", polishedExperienceSummary);
        response.put("rawSkillsInput", profile.getRawSkillsInput());
        response.put("rawProjectsInput", profile.getRawProjectsInput());
        response.put("skills", normalizedSkills);
        response.put("projects", normalizedProjects);
        response.put("aiScore", savedUser.getAiScore() != null ? savedUser.getAiScore() : 0);
        response.put("aiVerdict", savedUser.getAiVerdict() != null ? savedUser.getAiVerdict() : "No AI characteristics yet.");
        response.put("role", savedUser.getRole().name());
        return response;
    }

    private String validateQuestionnaire(CandidateQuestionnaireRequest request) {
        if (isBlank(request.getFirstName())
                || isBlank(request.getLastName())
                || isBlank(request.getPhoneNumber())
                || isBlank(request.getSkillsInput())
                || isBlank(request.getValueStatement())
                || isBlank(request.getExperienceSummary())
                || isBlank(request.getProjectsInput())) {
            return "All questionnaire fields are required";
        }

        String normalizedPhone = normalizePhone(request.getPhoneNumber());
        if (normalizedPhone.length() < 10) {
            return "Phone number is not valid";
        }

        if (cleanValue(request.getValueStatement()).length() < 3) {
            return "Value statement is too short";
        }

        if (cleanValue(request.getExperienceSummary()).length() < 5) {
            return "Experience description is too short";
        }

        if (cleanValue(request.getFirstName()).length() > MAX_NAME_LENGTH
                || cleanValue(request.getLastName()).length() > MAX_NAME_LENGTH) {
            return "First name or last name is too long";
        }

        if (normalizedPhone.length() > MAX_PHONE_LENGTH) {
            return "Phone number is too long";
        }

        if (cleanValue(request.getSkillsInput()).length() > MAX_SKILLS_LENGTH) {
            return "Skills description is too long";
        }

        if (cleanValue(request.getValueStatement()).length() > MAX_VALUE_STATEMENT_LENGTH) {
            return "Value statement is too long";
        }

        if (cleanValue(request.getExperienceSummary()).length() > MAX_EXPERIENCE_LENGTH) {
            return "Experience description is too long";
        }

        if (cleanValue(request.getProjectsInput()).length() > MAX_PROJECTS_LENGTH) {
            return "Projects description is too long";
        }

        return null;
    }

    private List<CandidateSkill> buildSkillEntities(CandidateProfile profile, List<String> skills) {
        List<CandidateSkill> result = new ArrayList<>();
        for (String skillName : skills) {
            CandidateSkill skill = new CandidateSkill();
            skill.setProfile(profile);
            skill.setName(skillName);
            result.add(skill);
        }
        return result;
    }

    private List<CandidateProject> buildProjectEntities(CandidateProfile profile, List<String> projects) {
        List<CandidateProject> result = new ArrayList<>();
        for (String projectTitle : projects) {
            CandidateProject project = new CandidateProject();
            project.setProfile(profile);
            project.setTitle(projectTitle);
            result.add(project);
        }
        return result;
    }

    private void replaceSkills(CandidateProfile profile, List<String> skills) {
        profile.getSkills().clear();
        profile.getSkills().addAll(buildSkillEntities(profile, skills));
    }

    private void replaceProjects(CandidateProfile profile, List<String> projects) {
        profile.getProjects().clear();
        profile.getProjects().addAll(buildProjectEntities(profile, projects));
    }

    private String cleanValue(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private String normalizePhone(String phoneNumber) {
        return phoneNumber == null ? "" : phoneNumber.replaceAll("[^\\d+]", "");
    }

    private List<String> splitStoredSkills(String skills) {
        if (skills == null || skills.isBlank()) {
            return List.of();
        }

        return java.util.Arrays.stream(skills.split("\\s*,\\s*"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toList());
    }
}
