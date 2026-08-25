package com.hiresense.api.auth.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hiresense.api.user.UserRepository;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIf(value = "com.hiresense.api.testsupport.DatabaseAvailability#isReachable")
class EmailVerificationIntegrationTest {

    private static final Pattern TOKEN_PARAM = Pattern.compile("token=([A-Za-z0-9_\\-]+)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EmailSender emailSender;

    private String signupAndCaptureToken(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"sup3rSecret!\",\"fullName\":\"Verify Me\"}"
                                .formatted(email)))
                .andExpect(status().isCreated());

        ArgumentCaptor<String> bodies = ArgumentCaptor.forClass(String.class);
        verify(emailSender, atLeastOnce()).send(anyString(), anyString(), bodies.capture());
        String body = bodies.getAllValues().get(bodies.getAllValues().size() - 1);
        Matcher matcher = TOKEN_PARAM.matcher(body);
        assertThat(matcher.find()).as("verification link contains token").isTrue();
        return matcher.group(1);
    }

    @Test
    void verificationLinkMarksUserVerifiedAndIsSingleUse() throws Exception {
        String token = signupAndCaptureToken("verify.me@example.com");

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\"}".formatted(token)))
                .andExpect(status().isNoContent());

        assertThat(userRepository
                        .findByEmail("verify.me@example.com")
                        .orElseThrow()
                        .isEmailVerified())
                .isTrue();

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\"}".formatted(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resendVerificationAcceptsKnownUnverifiedAndIgnoresUnknownSilently() throws Exception {
        signupAndCaptureToken("resend.me@example.com");

        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"resend.me@example.com\"}"))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ghost.user@example.com\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void unknownTokenRejectedWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"not-a-real-token\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid or expired token"));
    }
}
