package com.hiresense.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void bcryptEncoderProducesVerifiableDistinctHashes() {
        String hash = passwordEncoder.encode("correct horse battery staple");

        assertThat(passwordEncoder.matches("correct horse battery staple", hash))
                .isTrue();
        assertThat(passwordEncoder.matches("wrong password", hash)).isFalse();
        assertThat(passwordEncoder.encode("correct horse battery staple")).isNotEqualTo(hash);
    }

    @Test
    void healthEndpointRemainsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/health").with(anonymous())).andExpect(status().isOk());
    }
}
