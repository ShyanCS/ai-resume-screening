package com.hiresense.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hiresense.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIf(value = "com.hiresense.api.testsupport.DatabaseAvailability#isReachable")
class CandidateSignupIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private String body(String email, String password, String fullName) {
        return "{\"email\":\"%s\",\"password\":\"%s\",\"fullName\":\"%s\"}".formatted(email, password, fullName);
    }

    @Test
    void signupCreatesCandidateWithHashedPasswordAndNormalizedEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Asha.Kumar@Example.com", "sup3rSecret!", "Asha Kumar")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("asha.kumar@example.com"))
                .andExpect(jsonPath("$.fullName").value("Asha Kumar"))
                .andExpect(jsonPath("$.platformRole").value("CANDIDATE"));

        var saved = userRepository.findByEmail("asha.kumar@example.com").orElseThrow();
        assertThat(saved.getPasswordHash()).startsWith("$2");
        assertThat(saved.getPasswordHash()).doesNotContain("sup3rSecret!");
        assertThat(saved.isEmailVerified()).isFalse();
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("dup@example.com", "sup3rSecret!", "First User")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register/candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("DUP@example.com", "anotherPass1", "Second User")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Email already registered"));
    }

    @Test
    void invalidPayloadReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("not-an-email", "short", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.fullName").exists());
    }
}
