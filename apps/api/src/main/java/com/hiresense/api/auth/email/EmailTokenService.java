package com.hiresense.api.auth.email;

import com.hiresense.api.user.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailTokenService {

    private static final int TOKEN_BYTES = 32;

    private final EmailTokenRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailTokenService(EmailTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public String issue(User user, EmailTokenPurpose purpose, Duration ttl) {
        return issueInContext(user, purpose, ttl, null, null);
    }

    @Transactional
    public String issueInvite(
            User user,
            Duration ttl,
            com.hiresense.api.org.Organization organization,
            com.hiresense.api.org.OrgRole invitedRole) {
        return issueInContext(user, EmailTokenPurpose.RECRUITER_INVITE, ttl, organization, invitedRole);
    }

    private String issueInContext(
            User user,
            EmailTokenPurpose purpose,
            Duration ttl,
            com.hiresense.api.org.Organization organization,
            com.hiresense.api.org.OrgRole invitedRole) {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.save(new EmailToken(user, hash(raw), purpose, Instant.now().plus(ttl), organization, invitedRole));
        return raw;
    }

    @Transactional
    public User consume(String rawToken, EmailTokenPurpose purpose) {
        return consumeToken(rawToken, purpose).getUser();
    }

    @Transactional
    public EmailToken consumeToken(String rawToken, EmailTokenPurpose purpose) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidEmailTokenException();
        }
        EmailToken token = repository.findByTokenHash(hash(rawToken)).orElseThrow(InvalidEmailTokenException::new);
        if (token.getPurpose() != purpose || token.isUsed() || token.isExpired()) {
            throw new InvalidEmailTokenException();
        }
        token.markUsed();
        return token;
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
