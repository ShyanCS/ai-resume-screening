package com.hiresense.api.org;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgMemberRepository extends JpaRepository<OrgMember, Long> {

    Optional<OrgMember> findByOrganizationIdAndUserId(Long organizationId, Long userId);
}
