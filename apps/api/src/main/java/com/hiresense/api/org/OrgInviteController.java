package com.hiresense.api.org;

import com.hiresense.api.auth.dto.AcceptInviteRequest;
import com.hiresense.api.auth.dto.CreateInviteRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class OrgInviteController {

    private final OrgInviteService orgInviteService;

    public OrgInviteController(OrgInviteService orgInviteService) {
        this.orgInviteService = orgInviteService;
    }

    @PostMapping("/orgs/{orgId}/invites")
    public ResponseEntity<Void> invite(@PathVariable Long orgId, @Valid @RequestBody CreateInviteRequest request) {
        orgInviteService.invite(orgId, request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/auth/accept-invite")
    public ResponseEntity<Void> accept(@Valid @RequestBody AcceptInviteRequest request) {
        orgInviteService.accept(request.token());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
