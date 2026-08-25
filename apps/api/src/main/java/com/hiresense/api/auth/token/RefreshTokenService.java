package com.hiresense.api.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository repository;
    private final Duration ttl;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository repository, @Value("${app.auth.refresh-ttl-days:30}") long ttlDays) {
        this.repository = repository;
        this.ttl = Duration.ofDays(ttlDays);
    }

    @Transactional
    public String issue(com.hiresense.api.user.User user) {
        String raw = generateRawToken();
        repository.save(new RefreshToken(user, hash(raw), Instant.now().plus(ttl)));
        return raw;
    }

    @Transactional
    public com.hiresense.api.user.User rotate(String rawToken) {
        RefreshToken stored = requireValid(rawToken);
        if (stored.isRevoked()) {
            revokeAllForUser(stored.getUser().getId());
            throw new InvalidRefreshTokenException();
        }
        stored.revoke();
        return stored.getUser();
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken)).ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.findAllByUserIdAndRevokedAtIsNull(userId).forEach(RefreshToken::revoke);
    }

    private RefreshToken requireValid(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        RefreshToken stored = repository.findByTokenHash(hash(rawToken)).orElseThrow(InvalidRefreshTokenException::new);
        if (stored.isExpired()) {
            throw new InvalidRefreshTokenException();
        }
        return stored;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
