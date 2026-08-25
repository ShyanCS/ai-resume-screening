package com.hiresense.api.auth;

import com.hiresense.api.auth.dto.UserResponse;
import com.hiresense.api.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class MeController {

    private final UserRepository userRepository;

    public MeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public UserResponse me() {
        Long userId = CurrentUser.id();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return userRepository
                .findById(userId)
                .map(user ->
                        new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPlatformRole()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
