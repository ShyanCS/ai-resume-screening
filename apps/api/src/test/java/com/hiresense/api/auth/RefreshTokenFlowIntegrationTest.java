package com.hiresense.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hiresense.api.auth.token.RefreshTokenRepository;
import com.jayway.jsonpath.JsonPath;
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
class RefreshTokenFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void loginReturnsRefreshTokenAndRotationIssuesNewPair() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/register/candidate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"refresh.user@example.com\",\"password\":\"sup3rSecret!\",\"fullName\":\"Ref Lesh\"}"))
                .andExpect(status().isCreated());

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"refresh.user@example.com\",\"password\":\"sup3rSecret!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String originalAccess = (String) JsonPath.read(loginResponse, "$.accessToken");
        String originalRefresh = (String) JsonPath.read(loginResponse, "$.refreshToken");

        assertThat(originalAccess).isNotBlank();
        assertThat(originalRefresh).isNotBlank();

        String rotationResponse = mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(originalRefresh)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String newAccess = (String) JsonPath.read(rotationResponse, "$.accessToken");
        String newRefresh = (String) JsonPath.read(rotationResponse, "$.refreshToken");

        assertThat(newAccess).isNotBlank();
        assertThat(newRefresh).isNotEqualTo(originalRefresh);
    }

    @Test
    void reusingRotatedTokenRevokesEntireFamily() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/register/candidate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"family.user@example.com\",\"password\":\"sup3rSecret!\",\"fullName\":\"Fam Ily\"}"))
                .andExpect(status().isCreated());

        String firstLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"family.user@example.com\",\"password\":\"sup3rSecret!\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String refresh1 = (String) JsonPath.read(firstLogin, "$.refreshToken");

        String rotationResponse = mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refresh1)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String refresh2 = (String) JsonPath.read(rotationResponse, "$.refreshToken");

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refresh1)))
                .andExpect(status().isUnauthorized());

        boolean anyActiveForUser = refreshTokenRepository.findAll().stream()
                        .filter(t -> !t.isRevoked() && t.getUser().getEmail().equals("family.user@example.com"))
                        .count()
                == 0;
        assertThat(anyActiveForUser)
                .as("reuse detection revokes the whole token family")
                .isTrue();

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refresh2)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownRefreshTokenRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"bogus-token-value\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid refresh token"));
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/register/candidate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"logout.user@example.com\",\"password\":\"sup3rSecret!\",\"fullName\":\"Log Out\"}"))
                .andExpect(status().isCreated());

        String login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"logout.user@example.com\",\"password\":\"sup3rSecret!\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String refresh = (String) JsonPath.read(login, "$.refreshToken");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refresh)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refresh)))
                .andExpect(status().isUnauthorized());
    }
}
