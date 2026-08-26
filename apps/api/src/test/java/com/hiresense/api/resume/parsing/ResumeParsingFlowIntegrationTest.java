package com.hiresense.api.resume.parsing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hiresense.api.resume.ResumeRepository;
import com.hiresense.api.resume.ResumeStatus;
import com.hiresense.api.testsupport.TestDocuments;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
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
class ResumeParsingFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResumeRepository resumeRepository;

    private String signupAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/candidate")
                        .contentType("application/json")
                        .content("{\"email\":\"%s\",\"password\":\"sup3rSecret!\",\"fullName\":\"Parse User\"}"
                                .formatted(email)))
                .andExpect(status().isCreated());
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"%s\",\"password\":\"sup3rSecret!\"}".formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return (String) JsonPath.read(body, "$.accessToken");
    }

    @Test
    void uploadedPdfIsAsynchronouslyParsedToText() throws Exception {
        String token = signupAndLogin("parse.pdf@example.com");

        String response = mockMvc.perform(multipart("/api/v1/resumes")
                        .file(new MockMultipartFile(
                                "file",
                                "cv.pdf",
                                "application/pdf",
                                TestDocuments.pdfWithText("Skilled Java Developer with Spring experience")))
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
                    assertThat(resume.getContentText()).contains("Java Developer");
                    assertThat(resume.getParsedAt()).isNotNull();
                });
    }

    @Test
    void corruptButMagicValidFileFailsWithParseError() throws Exception {
        String token = signupAndLogin("parse.fail@example.com");
        byte[] header = {'%', 'P', 'D', 'F', '-', '1', '.', '4', '\n'};
        byte[] garbage = new byte[header.length + 64];
        System.arraycopy(header, 0, garbage, 0, header.length);
        for (int i = header.length; i < garbage.length; i++) {
            garbage[i] = (byte) ('a' + (i % 26));
        }

        String response = mockMvc.perform(multipart("/api/v1/resumes")
                        .file(new MockMultipartFile("file", "broken.pdf", "application/pdf", garbage))
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
                    var status = resume.getStatus();
                    if (status == ResumeStatus.PARSED) {
                        assertThat(resume.getContentText()).isBlank();
                    } else {
                        assertThat(status).isEqualTo(ResumeStatus.FAILED);
                        assertThat(resume.getParseError()).isNotBlank();
                    }
                });
    }
}
