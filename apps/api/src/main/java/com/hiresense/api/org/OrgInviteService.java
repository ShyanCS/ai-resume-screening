package com.hiresense.api.org;

import com.hiresense.api.auth.CurrentUser;
import com.hiresense.api.auth.email.EmailSender;
import com.hiresense.api.auth.email.EmailTokenService;
import com.hiresense.api.user.User;
import com.hiresense.api.user.UserRepository;
import java.time.Duration;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrgInviteService {

    private static final Duration INVITE_TTL = Duration.ofDays(7);

    private final OrganizationRepository organizationRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final UserRepository userRepository;
    private final EmailTokenService emailTokenService;
    private final EmailSender emailSender;
    private final String baseUrl;

    public OrgInviteService(
            OrganizationRepository organizationRepository,
            OrgMemberRepository orgMemberRepository,
            UserRepository userRepository,
            EmailTokenService emailTokenService,
            EmailSender emailSender,
            @Value("${app.base-url:http://localhost:3000}") String baseUrl) {
        this.organizationRepository = organizationRepository;
        this.orgMemberRepository = orgMemberRepository;
        this.userRepository = userRepository;
        this.emailTokenService = emailTokenService;
        this.emailSender = emailSender;
        this.baseUrl = baseUrl;
    }

    public void requireOrgRole(Long userId, Long orgId, OrgRole requiredRole) {
        OrgMember membership = orgMemberRepository
                .findByOrganizationIdAndUserId(orgId, userId)
                .orElseThrow(() -> new AccessDeniedException("Not a member of this organization"));
        if (membership.getRole() != requiredRole) {
            throw new AccessDeniedException("Requires " + requiredRole + " of this organization");
        }
    }

    @Transactional
    public void invite(Long orgId, String email) {
        Long actorId = CurrentUser.id();
        if (actorId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        requireOrgRole(actorId, orgId, OrgRole.ORG_ADMIN);

        Organization organization = organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));

        User invitee = userRepository
                .findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No HireSense account exists for this email"));

        if (orgMemberRepository
                .findByOrganizationIdAndUserId(orgId, invitee.getId())
                .isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member");
        }

        String rawToken = emailTokenService.issueInvite(invitee, INVITE_TTL, organization, OrgRole.RECRUITER);
        emailSender.send(
                invitee.getEmail(),
                "You're invited to join " + organization.getName() + " on HireSense",
                "You have been invited as a recruiter for " + organization.getName() + ".\n\n"
                        + "Accept your invitation:\n"
                        + baseUrl + "/accept-invite?token=" + rawToken + "\n\n"
                        + "The link expires in 7 days.");
    }

    @Transactional
    public void accept(String rawToken) {
        var token = emailTokenService.consumeToken(
                rawToken, com.hiresense.api.auth.email.EmailTokenPurpose.RECRUITER_INVITE);
        Organization organization = token.getOrganization();
        User invitee = token.getUser();
        if (orgMemberRepository
                .findByOrganizationIdAndUserId(organization.getId(), invitee.getId())
                .isEmpty()) {
            orgMemberRepository.save(new OrgMember(organization, invitee, token.getInvitedRole()));
        }
    }
}
