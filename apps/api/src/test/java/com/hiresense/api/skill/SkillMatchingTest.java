package com.hiresense.api.skill;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SkillMatchingTest {

    @Test
    void wholeWordMatchIsCaseInsensitive() {
        assertThat(SkillExtractionService.matches("Experienced with java and spring", "Java"))
                .isTrue();
        assertThat(SkillExtractionService.matches("JAVA developer", "Java")).isTrue();
    }

    @Test
    void substringInsideWordDoesNotMatch() {
        assertThat(SkillExtractionService.matches("JavaScript expert", "Java")).isFalse();
        assertThat(SkillExtractionService.matches("Kotlin expert", "Java")).isFalse();
    }

    @Test
    void multiWordSkillsAllowFlexibleWhitespace() {
        assertThat(SkillExtractionService.matches("Skilled in Machine\nLearning", "Machine Learning"))
                .isTrue();
    }

    @Test
    void symbolsInSkillNamesAreHandled() {
        assertThat(SkillExtractionService.matches("Proficient in C# and C++", "C#"))
                .isTrue();
        assertThat(SkillExtractionService.matches("Proficient in C and C++", "C#"))
                .isFalse();
        assertThat(SkillExtractionService.matches("Node.js runtime", "Node.js")).isTrue();
    }
}
