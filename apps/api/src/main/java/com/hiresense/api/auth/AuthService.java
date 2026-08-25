package com.hiresense.api.auth;

import com.hiresense.api.auth.dto.CandidateSignupRequest;
import com.hiresense.api.auth.dto.UserResponse;
import com.hiresense.api.user.PlatformRole;
import com.hiresense.api.user.User;
import com.hiresense.api.user.UserRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse registerCandidate(CandidateSignupRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException();
        }
        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                PlatformRole.CANDIDATE);
        User saved = userRepository.save(user);
        return new UserResponse(saved.getId(), saved.getEmail(), saved.getFullName(), saved.getPlatformRole());
    }
}
