package kz.invision.mainapi.core.repositories;

import kz.invision.mainapi.core.models.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {
    Optional<CandidateProfile> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    boolean existsByPhoneNumber(String phoneNumber);
}
