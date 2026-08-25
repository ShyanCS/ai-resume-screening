package com.hiresense.api.auth.email;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {

    Optional<EmailToken> findByTokenHash(String tokenHash);
}
