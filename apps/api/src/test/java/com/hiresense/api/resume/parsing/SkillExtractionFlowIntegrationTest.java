package com.hiresense.api.resume.parsing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hiresense.api.resume.ResumeRepository;
import com.hiresense.api.resume.ResumeStatus;
import com.hiresense.api.skill.CandidateSkillRepository;
import com.hiresense.api.skill.SkillSource;
import com.hiresense.api.testsupport.TestDocuments;
import com.hiresense.api.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIf(value = "com.hiresense.api.testsupport.DatabaseAvailability#isReachable")
class SkillExtractionFlowIntegrationTest {

    private static final String RESUME_TEXT =
            "Profile: Backend engineer. Skills: Java, Spring Boot, PostgreSQL, Docker, Machine Learning.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private CandidateSkillRepository candidateSkillRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void parsingPopulatesCandidateSkillsFromResumeText() throws Exception {
        String email = "skills.parse@example.com";
        mockMvc.perform(post("/api/v1/auth/register/candidate")
                        .contentType("application/json")
                        .content("{\"email\":\"%s\",\"password\":\"sup3rSecret!\",\"fullName\":\"Skill User\"}"
                                .formatted(email)))
                .andExpect(status().isCreated());
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();

        String login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"%s\",\"password\":\"sup3rSecret!\"}".formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = (String) JsonPath.read(login, "$.accessToken");

        String response = mockMvc.perform(multipart("/api/v1/resumes")
                        .file(new MockMultipartFile(
                                "file", "cv.pdf", "application/pdf", TestDocuments.pdfWithText(RESUME_TEXT)))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long resumeId = Long.valueOf((Integer) JsonPath.read(response, "$.id"));

        await().atMost(Duration.ofSeconds(30))
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    var resume = resumeRepository.findById(resumeId).orElseThrow();
                    assertThat(resume.getStatus()).isEqualTo(ResumeStatus.PARSED);

                    var ownSkills = candidateSkillRepository.findAll().stream()
                            .filter(cs -> cs.getUser().getId().equals(userId))
                            .toList();
                    var skillNames = ownSkills.stream()
                            .map(cs -> cs.getSkill().getName())
                            .collect(Collectors.toSet());

                    assertThat(skillNames).contains("Java", "Spring Boot", "PostgreSQL", "Docker", "Machine Learning");
                    assertThat(skillNames).doesNotContain("JavaScript", "Python");
                    assertThat(ownSkills).allMatch(cs -> cs.getSource() == SkillSource.RESUME_PARSED);
                });
    }
}
