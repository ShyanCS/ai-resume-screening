package com.hiresense.api.skill;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class SkillExtractionService {

    private final SkillRepository skillRepository;

    public SkillExtractionService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    public List<Skill> extractSkills(String resumeText) {
        if (resumeText == null || resumeText.isBlank()) {
            return List.of();
        }
        List<Skill> allSkills = skillRepository.findAll();
        return allSkills.stream()
                .filter(skill -> matches(resumeText, skill.getName()))
                .toList();
    }

    static boolean matches(String text, String skillName) {
        String body = java.util.Arrays.stream(skillName.trim().split("\\s+"))
                .map(Pattern::quote)
                .reduce((a, b) -> a + "\\s+" + b)
                .orElse("");
        Pattern pattern = Pattern.compile("(?i)(?<![A-Za-z0-9+#])" + body + "(?![A-Za-z0-9+#])");
        return pattern.matcher(text).find();
    }
}
