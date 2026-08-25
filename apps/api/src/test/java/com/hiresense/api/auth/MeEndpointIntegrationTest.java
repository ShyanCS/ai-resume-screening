package com.hiresense.api.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class MeEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String accessTokenFor(String email, String password) throws Exception {
        String login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return (String) JsonPath.read(login, "$.accessToken");
    }

    @Test
    void meReturnsAuthenticatedUserProfile() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/register/candidate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"me.user@example.com\",\"password\":\"sup3rSecret!\",\"fullName\":\"Me User\"}"))
                .andExpect(status().isCreated());

        String token = accessTokenFor("me.user@example.com", "sup3rSecret!");

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me.user@example.com"))
                .andExpect(jsonPath("$.fullName").value("Me User"));
    }

    @Test
    void meWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void meWithGarbageTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }
}
