package com.hiresense.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hiresense.api.auth.token.RefreshTokenRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIf(value = "com.hiresense.api.testsupport.DatabaseAvailability#isReachable")
class PasswordResetIntegrationTest {

    private static final String EMAIL = "reset.me@example.com";
    private static final Pattern TOKEN_PARAM = Pattern.compile("token=([A-Za-z0-9_\\-]+)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private com.hiresense.api.auth.email.EmailSender emailSender;

    private void signupAndLogin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"oldPassword1\",\"fullName\":\"Reset Me\"}"
                                .formatted(EMAIL)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"oldPassword1\"}".formatted(EMAIL)))
                .andExpect(status().isOk());
    }

    private String requestResetToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(EMAIL)))
                .andExpect(status().isAccepted());

        ArgumentCaptor<String> subjects = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodies = ArgumentCaptor.forClass(String.class);
        verify(emailSender, atLeastOnce()).send(anyString(), subjects.capture(), bodies.capture());

        int idx = bodies.getAllValues().size() - 1;
        assertThat(subjects.getAllValues().get(idx)).contains("Reset");
        Matcher matcher = TOKEN_PARAM.matcher(bodies.getAllValues().get(idx));
        assertThat(matcher.find()).as("reset link contains token").isTrue();
        return matcher.group(1);
    }

    @Test
    void forgotPasswordIsSilentlyAcceptedForUnknownEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody.knows@example.com\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void resetChangesPasswordAndRevokesSessions() throws Exception {
        signupAndLogin();
        String token = requestResetToken();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"newPassword\":\"brandNewPass9\"}".formatted(token)))
                .andExpect(status().isNoContent());

        var user = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(passwordEncoder.matches("brandNewPass9", user.getPasswordHash()))
                .isTrue();
        assertThat(passwordEncoder.matches("oldPassword1", user.getPasswordHash()))
                .isFalse();

        boolean activeSessions = refreshTokenRepository
                .findAllByUserIdAndRevokedAtIsNull(user.getId())
                .isEmpty();
        assertThat(activeSessions)
                .as("all sessions revoked after password reset")
                .isTrue();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"newPassword\":\"reusedToken12\"}".formatted(token)))
                .andExpect(status().isBadRequest());
    }
}
