package com.hiresense.api.org;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hiresense.api.auth.email.EmailSender;
import com.jayway.jsonpath.JsonPath;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIf(value = "com.hiresense.api.testsupport.DatabaseAvailability#isReachable")
class OrgInviteIntegrationTest {

    private static final Pattern TOKEN_PARAM = Pattern.compile("token=([A-Za-z0-9_\\-]+)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrgMemberRepository orgMemberRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @MockitoBean
    private EmailSender emailSender;

    private String signup(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"sup3rSecret!\",\"fullName\":\"Some User\"}"
                                .formatted(email)))
                .andExpect(status().isCreated());
        return login(email);
    }

    private String signupOrg(String slug, String adminEmail) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"orgName\":\"%s Corp\",\"slug\":\"%s\",\"admin\":{\"email\":\"%s\",\"password\":\"sup3rSecret!\",\"fullName\":\"Org Admin\"}}"
                                        .formatted(slug, slug, adminEmail)))
                .andExpect(status().isCreated());
        return login(adminEmail);
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"sup3rSecret!\"}".formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return (String) JsonPath.read(body, "$.accessToken");
    }

    private Long orgIdBySlug(String slug) {
        return organizationRepository.findBySlug(slug).orElseThrow().getId();
    }

    private String captureInviteToken() {
        ArgumentCaptor<String> bodies = ArgumentCaptor.forClass(String.class);
        verify(emailSender, atLeast(1)).send(anyString(), anyString(), bodies.capture());
        Matcher matcher = TOKEN_PARAM.matcher(
                bodies.getAllValues().get(bodies.getAllValues().size() - 1));
        assertThat(matcher.find()).as("invite link contains token").isTrue();
        return matcher.group(1);
    }

    @Test
    void adminInvitesExistingUserAndAcceptanceCreatesRecruiterMembership() throws Exception {
        String adminToken = signupOrg("invite-org", "admin@invite.example");
        signup("recruit.me@example.com");
        Long orgId = orgIdBySlug("invite-org");

        mockMvc.perform(post("/api/v1/orgs/{orgId}/invites", orgId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"recruit.me@example.com\"}"))
                .andExpect(status().isAccepted());

        String token = captureInviteToken();

        mockMvc.perform(post("/api/v1/auth/accept-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\"}".formatted(token)))
                .andExpect(status().isCreated());

        var membership = orgMemberRepository.findAll().stream()
                .filter(m -> m.getOrganization().getId().equals(orgId))
                .filter(m -> m.getUser().getEmail().equals("recruit.me@example.com"))
                .findFirst()
                .orElseThrow();
        assertThat(membership.getRole()).isEqualTo(OrgRole.RECRUITER);

        mockMvc.perform(post("/api/v1/auth/accept-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\"}".formatted(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonAdminCannotInvite() throws Exception {
        String adminToken = signupOrg("guard-org", "boss@guard.example");
        Long orgId = orgIdBySlug("guard-org");
        String outsiderToken = signup("outsider@guard.example");

        mockMvc.perform(post("/api/v1/orgs/{orgId}/invites", orgId)
                        .header("Authorization", "Bearer " + outsiderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"someone@else.example\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/orgs/{orgId}/invites", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"someone@else.example\"}"))
                .andExpect(status().isUnauthorized());

        assertThat(adminToken).isNotBlank();
    }

    @Test
    void invitingUnknownAccountReturnsNotFound() throws Exception {
        String adminToken = signupOrg("unknown-org", "admin@unknown.example");
        Long orgId = orgIdBySlug("unknown-org");

        mockMvc.perform(post("/api/v1/orgs/{orgId}/invites", orgId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ghost@unknown.example\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invitingExistingMemberReturnsConflict() throws Exception {
        String adminEmail = "admin@dup.example";
        String adminToken = signupOrg("dup-org", adminEmail);
        Long orgId = orgIdBySlug("dup-org");

        mockMvc.perform(post("/api/v1/orgs/{orgId}/invites", orgId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(adminEmail)))
                .andExpect(status().isConflict());
    }
}
