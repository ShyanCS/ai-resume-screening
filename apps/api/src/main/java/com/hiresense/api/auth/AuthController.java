package com.hiresense.api.auth;

import com.hiresense.api.auth.dto.AuthTokensResponse;
import com.hiresense.api.auth.dto.CandidateSignupRequest;
import com.hiresense.api.auth.dto.LoginRequest;
import com.hiresense.api.auth.dto.LoginResponse;
import com.hiresense.api.auth.dto.OrganizationSignupRequest;
import com.hiresense.api.auth.dto.OrganizationSignupResponse;
import com.hiresense.api.auth.dto.RefreshRequest;
import com.hiresense.api.auth.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/candidate")
    public ResponseEntity<UserResponse> registerCandidate(@Valid @RequestBody CandidateSignupRequest request) {
        UserResponse response = authService.registerCandidate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register/organization")
    public ResponseEntity<OrganizationSignupResponse> registerOrganization(
            @Valid @RequestBody OrganizationSignupRequest request) {
        OrganizationSignupResponse response = authService.registerOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/token/refresh")
    public AuthTokensResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
