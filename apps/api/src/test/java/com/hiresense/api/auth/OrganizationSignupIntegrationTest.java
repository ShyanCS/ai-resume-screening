package com.hiresense.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hiresense.api.org.OrgMemberRepository;
import com.hiresense.api.org.OrgRole;
import com.hiresense.api.org.OrganizationRepository;
import com.hiresense.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIf(value = "com.hiresense.api.testsupport.DatabaseAvailability#isReachable")
class OrganizationSignupIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrgMemberRepository orgMemberRepository;

    private String body(String orgName, String slug, String email) {
        String slugPart = slug == null ? "" : "\"slug\":\"%s\",".formatted(slug);
        return "{\"orgName\":\"%s\",%s\"admin\":{\"email\":\"%s\",\"password\":\"sup3rSecret!\","
                        .formatted(orgName, slugPart, email)
                + "\"fullName\":\"Ravi Menon\"}}";
    }

    @Test
    void signupCreatesOrgAdminMembershipWithDerivedSlug() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Acme HR Systems", null, "ravi@acme.example")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organization.name").value("Acme HR Systems"))
                .andExpect(jsonPath("$.organization.slug").value("acme-hr-systems"))
                .andExpect(jsonPath("$.admin.email").value("ravi@acme.example"))
                .andExpect(jsonPath("$.admin.platformRole").value("CANDIDATE"));

        var org = organizationRepository.findBySlug("acme-hr-systems").orElseThrow();
        var admin = userRepository.findByEmail("ravi@acme.example").orElseThrow();
        assertThat(admin.getPasswordHash()).startsWith("$2");

        var membership = orgMemberRepository.findAll().stream()
                .filter(m -> m.getOrganization().getId().equals(org.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(membership.getRole()).isEqualTo(OrgRole.ORG_ADMIN);
        assertThat(membership.getUser().getId()).isEqualTo(admin.getId());
    }

    @Test
    void explicitSlugIsHonoredAndNormalized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Beta Labs", "  Beta_Labs  ", "admin@beta.example")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organization.slug").value("beta-labs"));
    }

    @Test
    void duplicateSlugReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("First Corp", "dup-slug", "a@corp.example")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register/organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Second Corp", "dup-slug", "b@corp.example")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Organization slug already taken"));
    }

    @Test
    void invalidAdminPayloadReturnsFieldErrors() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/register/organization")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"orgName\":\"Gamma Co\",\"admin\":{\"email\":\"bad\",\"password\":\"x\",\"fullName\":\"\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[\"admin.email\"]").exists())
                .andExpect(jsonPath("$.errors[\"admin.password\"]").exists())
                .andExpect(jsonPath("$.errors[\"admin.fullName\"]").exists());
    }

    @Test
    void duplicateAdminEmailReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("One Org", "one-org", "shared@orgs.example")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register/organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Two Org", "two-org", "shared@orgs.example")))
                .andExpect(status().isConflict());
    }
}
