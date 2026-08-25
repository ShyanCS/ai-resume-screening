package com.hiresense.api.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final SecretKey KEY =
            new SecretKeySpec("test-signing-key-that-is-long-enough-32b!".getBytes(), "HmacSHA256");

    @Test
    void issuedTokenRoundTripsClaims() {
        JwtService service = new JwtService(KEY, Duration.ofMinutes(15));

        String token = service.issueAccessToken(42L, "user@example.com", "CANDIDATE");
        Claims claims = service.parseAndValidate(token);

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("CANDIDATE");
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService service = new JwtService(KEY, Duration.ofSeconds(-30));

        String token = service.issueAccessToken(1L, "x@example.com", "CANDIDATE");

        assertThatThrownBy(() -> service.parseAndValidate(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenSignedWithDifferentKeyIsRejected() {
        JwtService real = new JwtService(KEY, Duration.ofMinutes(5));
        SecretKey otherKey = new SecretKeySpec("another-signing-key-also-long-enough!!".getBytes(), "HmacSHA256");
        JwtService forgedIssuer = new JwtService(otherKey, Duration.ofMinutes(5));

        String forged = Jwts.builder()
                .subject("1")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(Duration.ofMinutes(5))))
                .signWith(otherKey)
                .compact();

        assertThat(real.parseAndValidate(real.issueAccessToken(1L, "a@b.c", "CANDIDATE"))
                        .getSubject())
                .isEqualTo("1");
        assertThatThrownBy(() -> real.parseAndValidate(forged)).isInstanceOf(SignatureException.class);
    }

    @Test
    void garbageTokenIsRejectedAsJwtException() {
        JwtService service = new JwtService(KEY, Duration.ofMinutes(5));
        assertThatThrownBy(() -> service.parseAndValidate("not-a-jwt")).isInstanceOf(JwtException.class);
    }
}
