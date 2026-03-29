package kz.invision.mainapi.core.models;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("skills")
    private String skills;

    @Column(columnDefinition = "TEXT")
    @JsonProperty("experience")
    private String experience;

    private String aiVerdict;
    private Integer aiScore = 0;

    @Enumerated(EnumType.STRING)
    private Role role = Role.CANDIDATE;

    public enum Role { CANDIDATE, ADMIN }
}