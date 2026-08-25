package com.hiresense.api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf(value = "com.hiresense.api.testsupport.DatabaseAvailability#isReachable")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User candidateUser(String email) {
        return new User(email, "$2a$10$stubhash", "Test User", PlatformRole.CANDIDATE);
    }

    @Test
    void savesAndLoadsCandidateByEmail() {
        User saved = userRepository.save(candidateUser("roundtrip@example.com"));

        User loaded = userRepository.findByEmail("roundtrip@example.com").orElseThrow();

        assertThat(loaded.getId()).isEqualTo(saved.getId());
        assertThat(loaded.getPlatformRole()).isEqualTo(PlatformRole.CANDIDATE);
        assertThat(loaded.isEmailVerified()).isFalse();
        assertThat(loaded.getCreatedAt()).isNotNull();
    }

    @Test
    void rejectsDuplicateEmail() {
        userRepository.saveAndFlush(candidateUser("dup@example.com"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(candidateUser("dup@example.com")));
    }

    @Test
    void existsByEmailReflectsPersistence() {
        assertThat(userRepository.existsByEmail("exists-check@example.com")).isFalse();

        userRepository.saveAndFlush(candidateUser("exists-check@example.com"));

        assertThat(userRepository.existsByEmail("exists-check@example.com")).isTrue();
    }
}
