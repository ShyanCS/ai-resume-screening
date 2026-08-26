package com.hiresense.api.skill;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateSkillRepository extends JpaRepository<CandidateSkill, Long> {

    Optional<CandidateSkill> findByUserIdAndSkillId(Long userId, Long skillId);
}
