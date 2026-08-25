package com.hiresense.api.auth;

import com.hiresense.api.auth.dto.CandidateSignupRequest;
import com.hiresense.api.auth.dto.LoginRequest;
import com.hiresense.api.auth.dto.LoginResponse;
import com.hiresense.api.auth.dto.OrganizationResponse;
import com.hiresense.api.auth.dto.OrganizationSignupRequest;
import com.hiresense.api.auth.dto.OrganizationSignupResponse;
import com.hiresense.api.auth.dto.UserResponse;
import com.hiresense.api.auth.jwt.JwtService;
import com.hiresense.api.org.OrgMember;
import com.hiresense.api.org.OrgMemberRepository;
import com.hiresense.api.org.OrgRole;
import com.hiresense.api.org.Organization;
import com.hiresense.api.org.OrganizationRepository;
import com.hiresense.api.user.PlatformRole;
import com.hiresense.api.user.User;
import com.hiresense.api.user.UserRepository;
import java.util.Locale;
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

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OrganizationRepository organizationRepository,
            OrgMemberRepository orgMemberRepository,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.organizationRepository = organizationRepository;
        this.orgMemberRepository = orgMemberRepository;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
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
        return new LoginResponse(
                accessToken,
                new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPlatformRole()));
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
        return new UserResponse(saved.getId(), saved.getEmail(), saved.getFullName(), saved.getPlatformRole());
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
