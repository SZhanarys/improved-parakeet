package kz.invision.mainapi.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Configuration
public class SchemaTextColumnUpdater {

    @Bean
    ApplicationRunner updateTextColumns(JdbcTemplate jdbcTemplate) {
        return args -> {
            List<String> statements = List.of(
                    "ALTER TABLE IF EXISTS candidate_profiles ALTER COLUMN value_statement TYPE TEXT",
                    "ALTER TABLE IF EXISTS candidate_profiles ALTER COLUMN experience_summary TYPE TEXT",
                    "ALTER TABLE IF EXISTS candidate_profiles ALTER COLUMN raw_skills_input TYPE TEXT",
                    "ALTER TABLE IF EXISTS candidate_profiles ALTER COLUMN raw_projects_input TYPE TEXT",
                    "ALTER TABLE IF EXISTS candidate_skills ALTER COLUMN name TYPE TEXT",
                    "ALTER TABLE IF EXISTS candidate_projects ALTER COLUMN title TYPE TEXT",
                    "ALTER TABLE IF EXISTS users ALTER COLUMN full_name TYPE TEXT",
                    "ALTER TABLE IF EXISTS users ALTER COLUMN skills TYPE TEXT",
                    "ALTER TABLE IF EXISTS users ALTER COLUMN experience TYPE TEXT",
                    "ALTER TABLE IF EXISTS users ALTER COLUMN ai_verdict TYPE TEXT"
            );

            for (String statement : statements) {
                jdbcTemplate.execute(statement);
            }
        };
    }
}
