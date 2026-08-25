package com.hiresense.api.auth.token;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RefreshTokenHashingTest {

    @Test
    void hashIsDeterministicSha256Hex() {
        String first = RefreshTokenService.hash("token-value");
        String second = RefreshTokenService.hash("token-value");

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    void differentTokensProduceDifferentHashes() {
        assertThat(RefreshTokenService.hash("a")).isNotEqualTo(RefreshTokenService.hash("b"));
    }
}
