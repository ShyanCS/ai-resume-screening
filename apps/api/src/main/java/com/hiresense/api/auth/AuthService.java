package com.hiresense.api.auth;

import com.hiresense.api.auth.dto.AuthTokensResponse;
import com.hiresense.api.auth.dto.CandidateSignupRequest;
import com.hiresense.api.auth.dto.LoginRequest;
import com.hiresense.api.auth.dto.LoginResponse;
import com.hiresense.api.auth.dto.OrganizationResponse;
import com.hiresense.api.auth.dto.OrganizationSignupRequest;
import com.hiresense.api.auth.dto.OrganizationSignupResponse;
import com.hiresense.api.auth.dto.UserResponse;
import com.hiresense.api.auth.email.EmailSender;
import com.hiresense.api.auth.email.EmailTokenPurpose;
import com.hiresense.api.auth.email.EmailTokenService;
import com.hiresense.api.auth.jwt.JwtService;
import com.hiresense.api.auth.token.RefreshTokenService;
import com.hiresense.api.org.OrgMember;
import com.hiresense.api.org.OrgMemberRepository;
import com.hiresense.api.org.OrgRole;
import com.hiresense.api.org.Organization;
import com.hiresense.api.org.OrganizationRepository;
import com.hiresense.api.user.PlatformRole;
import com.hiresense.api.user.User;
import com.hiresense.api.user.UserRepository;
import java.time.Duration;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationRepository organizationRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailTokenService emailTokenService;
    private final EmailSender emailSender;
    private final String baseUrl;
    private final long emailVerificationTtlHours;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OrganizationRepository organizationRepository,
            OrgMemberRepository orgMemberRepository,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            EmailTokenService emailTokenService,
            EmailSender emailSender,
            @Value("${app.base-url:http://localhost:3000}") String baseUrl,
            @Value("${app.auth.email-verification-ttl-hours:24}") long emailVerificationTtlHours) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.organizationRepository = organizationRepository;
        this.orgMemberRepository = orgMemberRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.emailTokenService = emailTokenService;
        this.emailSender = emailSender;
        this.baseUrl = baseUrl;
        this.emailVerificationTtlHours = emailVerificationTtlHours;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository
                .findByEmail(normalizedEmail)
                .filter(User::isEnabled)
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        String accessToken = jwtService.issueAccessToken(
                user.getId(), user.getEmail(), user.getPlatformRole().name());
        String refreshToken = refreshTokenService.issue(user);
        return new LoginResponse(
                accessToken,
                refreshToken,
                new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPlatformRole()));
    }

    @Transactional
    public AuthTokensResponse refresh(String refreshToken) {
        User user = refreshTokenService.rotate(refreshToken);
        String accessToken = jwtService.issueAccessToken(
                user.getId(), user.getEmail(), user.getPlatformRole().name());
        String newRefreshToken = refreshTokenService.issue(user);
        return new AuthTokensResponse(accessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    @Transactional
    public UserResponse registerCandidate(CandidateSignupRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException();
        }
        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                PlatformRole.CANDIDATE);
        User saved = userRepository.save(user);
        sendVerificationEmail(saved);
        return new UserResponse(saved.getId(), saved.getEmail(), saved.getFullName(), saved.getPlatformRole());
    }

    public void sendVerificationEmail(User user) {
        String rawToken = emailTokenService.issue(
                user, EmailTokenPurpose.EMAIL_VERIFICATION, Duration.ofHours(emailVerificationTtlHours));
        emailSender.send(
                user.getEmail(),
                "Verify your HireSense account",
                "Welcome to HireSense!\n\n"
                        + "Confirm your email address:\n"
                        + baseUrl + "/verify-email?token=" + rawToken + "\n\n"
                        + "If you did not sign up, you can ignore this email.");
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        User user = emailTokenService.consume(rawToken, EmailTokenPurpose.EMAIL_VERIFICATION);
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public void resendVerification(String email) {
        userRepository
                .findByEmail(normalizeEmail(email))
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::sendVerificationEmail);
    }

    @Transactional
    public OrganizationSignupResponse registerOrganization(OrganizationSignupRequest request) {
        String slug = request.slug() == null || request.slug().isBlank()
                ? deriveSlug(request.orgName())
                : normalizeSlug(request.slug());
        if (slug.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "orgName must contain alphanumeric characters to derive a slug");
        }
        if (organizationRepository.existsBySlug(slug)) {
            throw new SlugAlreadyTakenException();
        }

        OrganizationSignupRequest.AdminSignup admin = request.admin();
        String normalizedAdminEmail = normalizeEmail(admin.email());
        if (userRepository.existsByEmail(normalizedAdminEmail)) {
            throw new DuplicateEmailException();
        }

        Organization organization =
                organizationRepository.save(new Organization(request.orgName().trim(), slug));

        User adminUser = new User(
                normalizedAdminEmail,
                passwordEncoder.encode(admin.password()),
                admin.fullName().trim(),
                PlatformRole.CANDIDATE);
        userRepository.save(adminUser);

        orgMemberRepository.save(new OrgMember(organization, adminUser, OrgRole.ORG_ADMIN));

        sendVerificationEmail(adminUser);

        return new OrganizationSignupResponse(
                new OrganizationResponse(organization.getId(), organization.getName(), organization.getSlug()),
                new UserResponse(
                        adminUser.getId(), adminUser.getEmail(), adminUser.getFullName(), adminUser.getPlatformRole()));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String deriveSlug(String orgName) {
        return normalizeSlug(orgName);
    }

    private static String normalizeSlug(String value) {
        String cleaned = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        cleaned = cleaned.replaceAll("^-+", "").replaceAll("-+$", "");
        if (cleaned.length() > 100) {
            cleaned = cleaned.substring(0, 100);
        }
        return cleaned;
    }
}
