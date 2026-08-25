package com.hiresense.api.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Bean
    public SecretKey jwtSigningKey(
            @Value("${app.jwt.secret:dev-only-signing-secret-change-me-in-production-env}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes for HS256");
        }
        return new javax.crypto.spec.SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public Duration jwtAccessTtl(@Value("${app.jwt.access-ttl-seconds:900}") long ttlSeconds) {
        return Duration.ofSeconds(ttlSeconds);
    }
}
